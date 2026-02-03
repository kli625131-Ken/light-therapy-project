package com.lontri.lighttherapy.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.TreatmentDtos;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.ExperimentGroupDevice;
import com.lontri.lighttherapy.entity.SchemeDeviceScope;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.entity.Subject;
import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.TreatmentSessionDevice;
import com.lontri.lighttherapy.enums.EventType;
import com.lontri.lighttherapy.enums.TreatmentSessionStatus;
import com.lontri.lighttherapy.executor.TreatmentExecutor;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.repository.ExperimentGroupDeviceRepository;
import com.lontri.lighttherapy.repository.SchemeDeviceScopeRepository;
import com.lontri.lighttherapy.repository.SchemeStageRepository;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.TreatmentEventRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionDeviceRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionRepository;
import com.lontri.lighttherapy.service.TreatmentEventService;
import com.lontri.lighttherapy.service.TreatmentService;
import com.lontri.lighttherapy.service.TreatmentSessionStateMachine;
import com.lontri.lighttherapy.service.TreatmentSessionStateMachine.Action;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentSessionRepository sessionRepo;
    private final TreatmentSessionDeviceRepository sessionDeviceRepo;
    private final TreatmentEventRepository eventRepo;

    private final SubjectRepository subjectRepo;
    private final ExperimentGroupDeviceRepository egdRepo;
    private final DeviceRepository deviceRepo;

    private final SchemeDeviceScopeRepository scopeRepo;

    private final TreatmentSessionStateMachine stateMachine;
    private final TreatmentEventService eventService;
    
    private final TreatmentExecutor executor;
    
    private final SchemeStageRepository stageRepo;

    /**
     * 手动模式必须有一个固定 schemeId（因为 treatment_session.scheme_id NOT NULL）
     * 你可以在 application.yml 配置：
     * lighttherapy.manual-scheme-id: 1
     */
    @Value("${lighttherapy.manual-scheme-id:1}")
    private Long manualSchemeId;

    public TreatmentServiceImpl(
            TreatmentSessionRepository sessionRepo,
            TreatmentSessionDeviceRepository sessionDeviceRepo,
            TreatmentEventRepository eventRepo,
            SubjectRepository subjectRepo,
            ExperimentGroupDeviceRepository egdRepo,
            DeviceRepository deviceRepo,
            SchemeDeviceScopeRepository scopeRepo,
            TreatmentSessionStateMachine stateMachine,
            TreatmentEventService eventService,
            TreatmentExecutor executor,
            SchemeStageRepository stageRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.sessionDeviceRepo = sessionDeviceRepo;
        this.eventRepo = eventRepo;
        this.subjectRepo = subjectRepo;
        this.egdRepo = egdRepo;
        this.deviceRepo = deviceRepo;
        this.scopeRepo = scopeRepo;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.executor = executor;
        this.stageRepo = stageRepo;
    }

    // =========================
    // Create
    // =========================

    @Override
    @Transactional
    public TreatmentSession create(@Valid TreatmentDtos.CreateSessionReq req) {

        Subject subject = subjectRepo.findById(req.subjectId)
                .orElseThrow(() -> new BizException(40430, "subject not found", HttpStatus.NOT_FOUND));

        Long groupId = subject.getGroupId();
        if (groupId == null) {
            throw new BizException(40021, "subject has no group_id", HttpStatus.BAD_REQUEST);
        }

        List<SchemeDeviceScope> scopes = scopeRepo.findBySchemeId(req.schemeId);

        // 设备清单：优先 deviceSns，否则从 group 池按 scope 过滤
        // 兼容旧字段 deviceSn：如果提供了单个设备，添加到 deviceSns 列表
        List<String> deviceSns = new ArrayList<>();
        if (req.deviceSns != null) {
            deviceSns.addAll(req.deviceSns);
        }
        if (req.deviceSn != null && !req.deviceSn.isEmpty()) {
            deviceSns.add(req.deviceSn);
        }
        List<Device> devices = resolveDevicesForSession(groupId, deviceSns, scopes);

        // 创建 session（对齐 DB 默认 PLANNED）
        TreatmentSession s = new TreatmentSession();
        s.setSubjectId(req.subjectId);
        s.setSchemeId(req.schemeId);
        s.setStageId(req.stageId);
        s.setStatus(TreatmentSessionStatus.PLANNED);

        // 兼容旧字段 device_sn：如果 req.deviceSn 提供，就写入（否则不写）
        // 之后业务逻辑不要再依赖这个字段
        s = sessionRepo.save(s);

        // 固化本次会话控制设备
        for (Device d : devices) {
            TreatmentSessionDevice tsd = new TreatmentSessionDevice();
            tsd.setSessionId(s.getId());
            tsd.setDeviceId(d.getId());
            tsd.setDeviceSn(d.getDeviceSn());
            sessionDeviceRepo.save(tsd);
        }

        // 记事件：SESSION_CREATED
        eventService.record(s.getId(), EventType.SESSION_CREATED, null);

        return s;
    }

    /**
     * /start-now：create + start
     */
    @Override
    @Transactional
    public TreatmentSession createAndStart(@Valid TreatmentDtos.CreateSessionReq req) {
        TreatmentSession s = create(req);

        // start 前检查是否已经有 session_device
        // 注意：不要调用 syncDevicesBeforeStart，因为它会启动执行器，而 stateMachine.apply(START) 也会启动执行器
        // 避免重复启动执行器导致的问题
        List<TreatmentSessionDevice> links = sessionDeviceRepo.findBySessionId(s.getId());
        if (links == null || links.isEmpty()) {
            throw new BizException(40031, "session has no bound devices(treatment_session_device)", HttpStatus.BAD_REQUEST);
        }

        return stateMachine.apply(s.getId(), Action.START, null);
    }

    // =========================
    // Lifecycle
    // =========================

    @Override
    @Transactional
    public TreatmentSession start(Long id) {
        // 直接在方法中添加简单的设备绑定检查，避免重复启动执行器
        List<TreatmentSessionDevice> links = sessionDeviceRepo.findBySessionId(id);
        if (links == null || links.isEmpty()) {
            throw new BizException(40031, "session has no bound devices(treatment_session_device)", HttpStatus.BAD_REQUEST);
        }
        
        // 查询会话信息，验证session存在
        TreatmentSession session = sessionRepo.findById(id)
                .orElseThrow(() -> new BizException(40410, "session not found", HttpStatus.NOT_FOUND));
        
        // 查询scheme下的所有stage，按stageNo排序，确保scheme有有效的治疗阶段
        List<SchemeStage> stages = stageRepo.findBySchemeIdOrderByStageNoAsc(session.getSchemeId());
        if (stages == null || stages.isEmpty()) {
            throw new BizException(40421, "scheme has no stages", HttpStatus.NOT_FOUND);
        }
        
        return stateMachine.apply(id, Action.START, null);
    }

    @Override
    @Transactional
    public TreatmentSession end(Long id) {
        TreatmentSession session = stateMachine.apply(id, Action.END, null);
        // 停止执行器，确保不再发送指令
        executor.stop(id, "session ended");
        return session;
    }

    @Override
    @Transactional
    public TreatmentSession pause(Long id) {
        TreatmentSession session = stateMachine.apply(id, Action.PAUSE, null);
        // 暂停执行器
        executor.pauseSession(id);
        return session;
    }

    @Override
    @Transactional
    public TreatmentSession resume(Long id) {
        TreatmentSession session = stateMachine.apply(id, Action.RESUME, null);
        // 恢复执行器
        executor.resumeSession(id);
        return session;
    }

    @Override
    @Transactional
    public TreatmentSession cancel(Long id, String reason) {
        TreatmentSession session = stateMachine.apply(id, Action.CANCEL, reason);
        // 停止执行器，确保不再发送指令
        executor.stop(id, "session cancelled: " + reason);
        return session;
    }

    @Override
    @Transactional
    public TreatmentSession fail(Long id, String reason) {
        TreatmentSession session = stateMachine.apply(id, Action.FAIL, reason);
        // 停止执行器，确保不再发送指令
        executor.stop(id, "session failed: " + reason);
        return session;
    }

    // =========================
    // Manual Start
    // =========================

    /**
     * 手动模式：你 DTO 没有 schemeId，但 treatment_session.scheme_id NOT NULL
     * 所以必须使用一个固定 manualSchemeId（配置/常量）。
     *
     * 处理策略：
     * 1) 创建一个 session（schemeId=manualSchemeId）
     * 2) 将 deviceSn 写入旧字段（可选），并写入 session_device（只绑定这一台）
     * 3) metrics_json 写入 lux/cctK（如果你 TreatmentSession.metricsJson 映射为 String）
     * 4) 记事件 MANUAL_STARTED，然后 START
     */
    @Override
    @Transactional
    public TreatmentSession manualStart(@Valid TreatmentDtos.ManualStartReq req) {
        Subject subject = subjectRepo.findById(req.subjectId)
                .orElseThrow(() -> new BizException(40430, "subject not found", HttpStatus.NOT_FOUND));

        Long groupId = subject.getGroupId();
        if (groupId == null) throw new BizException(40021, "subject has no group_id", HttpStatus.BAD_REQUEST);

        // 验证设备列表非空
        if (req.deviceControls == null || req.deviceControls.isEmpty()) {
            throw new BizException(40040, "device controls cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // 获取所有设备SN
        List<String> deviceSns = req.deviceControls.stream()
                .map(TreatmentDtos.ManualStartReq.DeviceControlParam::getDeviceSn)
                .map(String::trim)
                .collect(Collectors.toList());

        // 批量查询设备
        List<Device> devices = deviceRepo.findByDeviceSnIn(deviceSns);
        if (devices.size() != deviceSns.size()) {
            throw new BizException(40041, "some deviceSn not found", HttpStatus.BAD_REQUEST);
        }

        // 检查设备的deviceSn是否为空
        for (Device device : devices) {
            if (device.getDeviceSn() == null || device.getDeviceSn().trim().isEmpty()) {
                throw new BizException(40044, "device " + device.getId() + " has empty deviceSn", HttpStatus.BAD_REQUEST);
            }
        }

        // 校验所有设备属于本组
        List<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        List<Long> existingIds = egdRepo.findExistingDeviceIds(groupId, deviceIds);
        if (existingIds.size() != deviceIds.size()) {
            throw new BizException(40042, "some devices not in this experiment group", HttpStatus.BAD_REQUEST);
        }

        // manualScheme 的 scope 校验（可选但推荐：防止手动用到不允许的设备类型/模式）
        List<SchemeDeviceScope> scopes = scopeRepo.findBySchemeId(manualSchemeId);
        if (!scopes.isEmpty()) {
            boolean allAllowed = devices.stream().allMatch(device -> scopes.stream().anyMatch(scope -> matchScope(device, scope)));
            if (!allAllowed) {
                throw new BizException(40043, "some devices not allowed by manual scheme scope", HttpStatus.BAD_REQUEST);
            }
        }

        // 创建 session
        TreatmentSession s = new TreatmentSession();
        s.setSubjectId(req.subjectId);
        s.setSchemeId(manualSchemeId);
        s.setStageId(null);
        s.setStatus(TreatmentSessionStatus.PLANNED);
        
        // 构建metrics_json，包含所有设备的配置
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 使用一个简单的Map来构建设备配置
            java.util.List<java.util.Map<String, Object>> deviceConfigs = new java.util.ArrayList<>();
            for (TreatmentDtos.ManualStartReq.DeviceControlParam param : req.deviceControls) {
                java.util.Map<String, Object> deviceConfig = new java.util.HashMap<>();
                deviceConfig.put("deviceSn", param.getDeviceSn().trim());
                deviceConfig.put("dim", param.getDim());
                deviceConfig.put("cctK", param.getCctK());
                if (param.getSkyDim() != null) {
                    deviceConfig.put("skyDim", param.getSkyDim());
                }
                deviceConfigs.add(deviceConfig);
            }
            
            java.util.Map<String, Object> metricsJson = new java.util.HashMap<>();
            metricsJson.put("deviceConfigs", deviceConfigs);
            s.setMetricsJson(mapper.writeValueAsString(metricsJson));
        } catch (JsonProcessingException e) {
            throw new BizException(50001, "Failed to process JSON: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        s = sessionRepo.save(s);
        Long sessionId = s.getId();

        // 绑定所有设备到session_device
        for (Device device : devices) {
            try {
                TreatmentSessionDevice tsd = new TreatmentSessionDevice();
                tsd.setSessionId(s.getId());
                tsd.setDeviceId(device.getId());
                tsd.setDeviceSn(device.getDeviceSn());
                sessionDeviceRepo.save(tsd);
            } catch (Exception e) {
                throw new BizException(50002, "Failed to save device " + device.getDeviceSn() + " for session: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 事件：created + manual_started
        eventService.record(sessionId, EventType.SESSION_CREATED, null);
        eventService.record(sessionId, EventType.MANUAL_STARTED, null);

        syncDevicesBeforeManualStart(sessionId);
        // A) 先落库：统一入口（只管状态）
        TreatmentSession ts = stateMachine.apply(sessionId, Action.START, null);
        // B) 事务提交后再启动执行器（避免 executor 新线程读不到刚保存的数据）
        afterCommit(() -> {
            // 构建设备配置列表
            List<TreatmentExecutor.DeviceManualConfig> deviceConfigs = new ArrayList<>();
            for (TreatmentDtos.ManualStartReq.DeviceControlParam param : req.deviceControls) {
                deviceConfigs.add(new TreatmentExecutor.DeviceManualConfig(
                    param.getDeviceSn().trim(),
                    param.getDim(),
                    param.getSumDim(),
                    param.getSkyDim(),
                    param.getCctK(),
                    param.getSkyCctK()
                ));
            }
            // 调用多设备版本的手动启动
            executor.startManual(sessionId, deviceConfigs);
        });
        return ts;
    }
    
    @Transactional
    public void manualControl(TreatmentDtos.ManualControlReq req) {

        TreatmentSession s = sessionRepo.findById(req.getSessionId())
            .orElseThrow(() -> new BizException(40410, "session not found", HttpStatus.NOT_FOUND));

        if (s.getStatus() != TreatmentSessionStatus.RUNNING) {
            throw new BizException(40901, "session not RUNNING", HttpStatus.CONFLICT);
        }

        if (!Objects.equals(s.getSchemeId(), manualSchemeId)) {
            throw new BizException(40902, "not manual mode session", HttpStatus.CONFLICT);
        }

        // 处理每个设备的控制请求
        for (TreatmentDtos.ManualControlReq.DeviceControlParam param : req.getDeviceControls()) {
            String deviceSn = param.getDeviceSn();
            Integer dim = param.getDim();
            Integer sumDim = param.getSumDim();
            Integer skyDim = param.getSkyDim();
            Integer cctK = param.getCctK();
            Integer skyCctK = param.getSkyCctK();

            // 验证设备是否绑定到会话
            boolean bound = sessionDeviceRepo.existsBySessionIdAndDeviceSn(req.getSessionId(), deviceSn);
            if (!bound) {
                throw new BizException(40903, "device " + deviceSn + " not bound to session", HttpStatus.CONFLICT);
            }

            // 调用执行器控制设备
            executor.manualControlOnce(
                req.getSessionId(),
                deviceSn,
                dim,
                sumDim,
                skyDim,
                cctK,
                skyCctK,
                req.getSource(),
                req.getNote()
            );
        }
    }

    // =========================
    // Query
    // =========================

    @Override
    @Transactional(readOnly = true)
    public Page<TreatmentSession> list(Long subjectId, String status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(java.time.LocalTime.MAX) : null;
        
        if (subjectId != null && hasText(status)) {
            return sessionRepo.search(subjectId, status, start, end, pageable);
        } else if (subjectId != null) {
            return sessionRepo.search(subjectId, null, start, end, pageable);
        } else if (hasText(status)) {
            return sessionRepo.search(null, status, start, end, pageable);
        } else {
            return sessionRepo.search(null, null, start, end, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TreatmentSession get(Long id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new BizException(40410, "session not found", HttpStatus.NOT_FOUND));
    }

    // =========================
    // Events
    // =========================

    @Override
    @Transactional
    public TreatmentEvent addEvent(Long sessionId, @Valid TreatmentDtos.AddEventReq req) {
        // 你 DTO 的 eventType 是 String，这里做两层：
        // 1) 能映射到规范 EventType 的 -> 存规范 code
        // 2) 映射不到 -> 原样存（不阻塞）
        String typeToStore = normalizeEventType(req.eventType);

        TreatmentEvent e = new TreatmentEvent();
        e.setSessionId(sessionId);
        e.setEventType(typeToStore);
        e.setMessage(req.message);
        e.setDetailJson(req.detailJson);
        e.setEventTime(LocalDateTime.now());
        return eventRepo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentEvent> listEvents(Long sessionId) {
        return eventRepo.findBySessionIdOrderByEventTimeAsc(sessionId);
    }

    // =========================
    // Internal helpers
    // =========================

    private void syncDevicesBeforeStart(Long sessionId) {
        List<TreatmentSessionDevice> links = sessionDeviceRepo.findBySessionId(sessionId);
        if (links == null || links.isEmpty()) {
            throw new BizException(40031, "session has no bound devices(treatment_session_device)", HttpStatus.BAD_REQUEST);
        }
        
        // 1. 查询会话信息，验证session存在
        TreatmentSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BizException(40410, "session not found", HttpStatus.NOT_FOUND));
        
        // 2. 查询scheme下的所有stage，按stageNo排序，确保scheme有有效的治疗阶段
        List<SchemeStage> stages = stageRepo.findBySchemeIdOrderByStageNoAsc(session.getSchemeId());
        if (stages == null || stages.isEmpty()) {
            throw new BizException(40421, "scheme has no stages", HttpStatus.NOT_FOUND);
        }
        
        // 3. 通过TreatmentExecutor启动方案，会自动处理所有设备的配置和控制
        // 重要：必须在事务提交后执行，否则执行器无法读取到刚创建的会话和设备关联信息
        afterCommit(() -> executor.startScheme(sessionId));
    }

    private void syncDevicesBeforeManualStart(Long sessionId) {
        List<TreatmentSessionDevice> links = sessionDeviceRepo.findBySessionId(sessionId);
        if (links == null || links.isEmpty()) {
            throw new BizException(40031, "session has no bound devices(treatment_session_device)", HttpStatus.BAD_REQUEST);
        }
        
        // 手动模式不需要启动方案，执行器启动已在afterCommit中处理
        // 此方法仅用于验证设备绑定
    }
    private void afterCommit(Runnable r) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { r.run(); }
                }
            );
        } else {
            r.run();
        }
    }
    private List<Device> resolveDevicesForSession(Long groupId, List<String> deviceSns, List<SchemeDeviceScope> scopes) {
        List<String> sns = normalizeSns(deviceSns);

        // 用户选择
        if (!sns.isEmpty()) {
            List<Device> devices = deviceRepo.findByDeviceSnIn(sns);
            if (devices.size() != sns.size()) {
                throw new BizException(40041, "some deviceSn not found", HttpStatus.BAD_REQUEST);
            }

            // 校验属于 group
            List<Long> ids = devices.stream().map(Device::getId).collect(java.util.stream.Collectors.toList());
            List<Long> existing = egdRepo.findExistingDeviceIds(groupId, ids);
            if (existing.size() != ids.size()) {
                throw new BizException(40042, "some devices are not in this experiment group", HttpStatus.BAD_REQUEST);
            }

            // 校验 scope（如果有 scope）
            if (!scopes.isEmpty()) {
                for (Device d : devices) {
                    if (!scopes.stream().anyMatch(s -> matchScope(d, s))) {
                        throw new BizException(40043, "device not allowed by scheme scope: " + d.getDeviceSn(), HttpStatus.BAD_REQUEST);
                    }
                }
            }
            return devices;
        }

        // 默认组设备池
        List<Long> groupDeviceIds = egdRepo.findByGroupId(groupId).stream()
                .map(ExperimentGroupDevice::getDeviceId)
                .collect(java.util.stream.Collectors.toList());

        if (groupDeviceIds.isEmpty()) {
            throw new BizException(40044, "experiment group has no bound devices", HttpStatus.BAD_REQUEST);
        }

        List<Device> devices = deviceRepo.findByIdIn(groupDeviceIds);

        // 默认：按 scope 过滤（如果有 scope）
        if (!scopes.isEmpty()) {
            devices = devices.stream()
                    .filter(d -> scopes.stream().anyMatch(s -> matchScope(d, s)))
                    .collect(java.util.stream.Collectors.toList());

            if (devices.isEmpty()) {
                throw new BizException(40045, "no group devices match scheme scope", HttpStatus.BAD_REQUEST);
            }
        }

        return devices;
    }

    private boolean matchScope(Device d, SchemeDeviceScope s) {
        if (s.getGatewayId() != null) {
            if (d.getGatewayId() == null) return false;
            if (!Objects.equals(s.getGatewayId(), d.getGatewayId())) return false;
        }
        if (s.getDeviceType() != null && s.getDeviceType() != d.getDeviceType()) return false;
        if (s.getSendMode() != null && s.getSendMode() != d.getSendMode()) return false;
        return true;
    }

    // private TreatmentSessionStatus parseStatus(String status) {
    //     try {
    //         return TreatmentSessionStatus.valueOf(status.trim().toUpperCase());
    //     } catch (Exception e) {
    //         throw new BizException(40011, "invalid status: " + status, HttpStatus.BAD_REQUEST);
    //     }
    // }

    private List<String> normalizeSns(List<String> sns) {
        if (sns == null) return Collections.emptyList();
        return sns.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 兼容你 DTO 的 eventType（START/END/DEVICE_MSG/ERROR/...）
     * 如果它恰好等于某个 EventType.code() 或 name()，则转为规范 code；
     * 否则原样存。
     */
    private String normalizeEventType(String raw) {
        if (!hasText(raw)) {
            throw new BizException(40061, "eventType is blank", HttpStatus.BAD_REQUEST);
        }
        String v = raw.trim();

        // 1) 直接匹配 code
        for (EventType t : EventType.values()) {
            if (t.code().equalsIgnoreCase(v)) return t.code();
        }
        // 2) 匹配枚举 name()
        try {
            return EventType.valueOf(v.toUpperCase()).code();
        } catch (Exception ignore) {
            // 3) 保留原样（兼容老前端/临时事件）
            return v;
        }
    }
}
