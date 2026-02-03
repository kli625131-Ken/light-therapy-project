package com.lontri.lighttherapy.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lontri.lighttherapy.dto.AuthDtos;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.Scheme;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.TreatmentSessionDevice;
import com.lontri.lighttherapy.enums.TreatmentSessionStatus;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.repository.SchemeRepository;
import com.lontri.lighttherapy.repository.SchemeStageRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionDeviceRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionRepository;

@Service
public class SubjectSnapshotService {

    /**
     * 你项目里统一常量即可（最好来自配置或 Scheme 表）
     */
    private static final long MANUAL_SCHEME_ID = 3L;

    private final TreatmentSessionRepository sessionRepo;
    private final TreatmentSessionDeviceRepository tsdRepo;
    private final DeviceRepository deviceRepo;
    private final SchemeRepository schemeRepo;
    private final SchemeStageRepository stageRepo;

    private final ObjectMapper objectMapper;

    public SubjectSnapshotService(TreatmentSessionRepository sessionRepo,
                                  TreatmentSessionDeviceRepository tsdRepo,
                                  DeviceRepository deviceRepo,
                                  SchemeRepository schemeRepo,
                                  SchemeStageRepository stageRepo,
                                  ObjectMapper objectMapper // ✅ 建议注入 Spring 的 ObjectMapper
    ) {
        this.sessionRepo = sessionRepo;
        this.tsdRepo = tsdRepo;
        this.deviceRepo = deviceRepo;
        this.schemeRepo = schemeRepo;
        this.stageRepo = stageRepo;
        this.objectMapper = objectMapper;
    }

    public AuthDtos.SubjectSnapshot build(Long subjectId) {
        AuthDtos.SubjectSnapshot snap = new AuthDtos.SubjectSnapshot();
        snap.serverTime = LocalDateTime.now().toString();

        // ✅ status 用枚举（你的 TreatmentSession.status 已经是 @Enumerated(EnumType.STRING)）
        TreatmentSession s = sessionRepo
                .findFirstBySubjectIdAndStatusOrderByStartTimeDesc(subjectId, TreatmentSessionStatus.RUNNING)
                .orElse(null);

        if (s == null) return snap;

        AuthDtos.CurrentSession cs = new AuthDtos.CurrentSession();
        cs.sessionId = s.getId();
        cs.status = (s.getStatus() == null) ? null : s.getStatus().name();
        cs.schemeId = s.getSchemeId();
        cs.stageId = s.getStageId();
        cs.startTime = s.getStartTime();

        // ✅ Long 比较用 equals，避免 “==” 装箱问题
        cs.mode = (s.getSchemeId() != null && s.getSchemeId().equals(MANUAL_SCHEME_ID)) ? "MANUAL" : "SCHEME";

        // ✅ schemeName（防御性：scheme 可能被删/为空）
        if (s.getSchemeId() != null) {
            cs.schemeName = schemeRepo.findById(s.getSchemeId())
                    .map(Scheme::getName) // 你的字段若不是 name 改这里
                    .orElse(null);
        }

        // ✅ devices：优先 session_device；空则 fallback 旧字段 session.deviceSn
        List<String> sns = tsdRepo.findBySessionId(s.getId())
                .stream()
                .map(TreatmentSessionDevice::getDeviceSn)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (sns.isEmpty() && s.getDeviceSn() != null && !s.getDeviceSn().trim().isEmpty()) {
            sns = Collections.singletonList(s.getDeviceSn().trim());
        }

        cs.devices = buildDeviceInfos(sns);

        // ✅ stage：手动 / 方案 两种模式
        if ("MANUAL".equals(cs.mode)) {
            cs.stage = buildManualStageFromMetrics(s.getMetricsJson());
        } else {
            cs.stage = buildSchemeStage(s);
        }

        snap.currentSession = cs;
        return snap;
    }

    private List<AuthDtos.DeviceInfo> buildDeviceInfos(List<String> sns) {
        if (sns == null || sns.isEmpty()) return new ArrayList<>();

        // 批量查 device，避免 N+1
        Map<String, Device> deviceMap = deviceRepo.findByDeviceSnIn(sns).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Device::getDeviceSn, d -> d, (a, b) -> a));

        return sns.stream().map(sn -> {
            AuthDtos.DeviceInfo di = new AuthDtos.DeviceInfo();
            di.deviceSn = sn;

            Device d = deviceMap.get(sn);
            if (d != null) {
                di.sendMode = (d.getSendMode() == null) ? null : d.getSendMode().name();
                di.deviceType = (d.getDeviceType() == null) ? null : d.getDeviceType().name();
            }
            return di;
        }).collect(Collectors.toList());
    }

    /**
     * 手动模式 stage：从 metrics_json 解析 lux/cctK（兼容两种结构）
     * - 结构 A：{"manual":{"lux":..,"cctK":..}}
     * - 结构 B：{"lux":..,"cctK":..}  （你 manualStart 当前写法）
     */
    private AuthDtos.StageInfo buildManualStageFromMetrics(String metricsJson) {
        AuthDtos.StageInfo st = new AuthDtos.StageInfo();
        st.name = "Manual";

        if (!hasText(metricsJson)) return st;

        try {
            JsonNode root = objectMapper.readTree(metricsJson);

            // A: root.manual
            JsonNode manual = root.path("manual");
            if (!manual.isMissingNode() && manual.isObject()) {
                applyManualFields(st, manual);
                return st;
            }

            // B: root 直接包含字段
            applyManualFields(st, root);
            return st;

        } catch (Exception ignore) {
            // 不阻断登录
            return st;
        }
    }

    private void applyManualFields(AuthDtos.StageInfo st, JsonNode node) {
        if (node == null) return;

        if (node.has("lux") && node.get("lux").canConvertToInt()) {
            st.lux = node.get("lux").asInt();
        }
        // 兼容 cct / cctK 两种字段
        if (node.has("cctK") && node.get("cctK").canConvertToInt()) {
            st.cctK = node.get("cctK").asInt();
        } else if (node.has("cct") && node.get("cct").canConvertToInt()) {
            st.cctK = node.get("cct").asInt();
        }
    }

    private AuthDtos.StageInfo buildSchemeStage(TreatmentSession s) {
        SchemeStage stage = null;

        // 1) 优先按 stageId 精确定位
        if (s.getStageId() != null) {
            stage = stageRepo.findById(s.getStageId()).orElse(null);
        }

        // 2) 没有 stageId：按 schemeId + stageNo 最小取一个（最小可用）
        if (stage == null && s.getSchemeId() != null) {
            stage = stageRepo.findFirstBySchemeIdOrderByStageNoAsc(s.getSchemeId()).orElse(null);
        }

        if (stage == null) return null;

        AuthDtos.StageInfo st = new AuthDtos.StageInfo();
        st.stageId = stage.getId();
        st.stageNo = stage.getStageNo();
        st.lux = stage.getLightIntensity();
        st.cctK = stage.getLightColorTemp();
        st.durationMinutes = stage.getDurationMinutes(); // 如果你表/实体不是 minutes，改对应字段
        st.name = stage.getName();
        return st;
    }

    private boolean hasText(String v) {
        return v != null && !v.trim().isEmpty();
    }
}
