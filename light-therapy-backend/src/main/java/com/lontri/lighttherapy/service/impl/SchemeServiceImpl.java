package com.lontri.lighttherapy.service.impl;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.dto.SchemeDtos.CreateSchemeWithStagesReq;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.Scheme;
import com.lontri.lighttherapy.entity.SchemeGroup;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.entity.SchemeStageDeviceConfig;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.enums.SchemeScope;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.repository.ExperimentGroupRepository;
import com.lontri.lighttherapy.repository.SchemeGroupRepository;
import com.lontri.lighttherapy.repository.SchemeRepository;
import com.lontri.lighttherapy.repository.SchemeStageRepository;
import com.lontri.lighttherapy.repository.SchemeStageDeviceConfigRepository;
import com.lontri.lighttherapy.service.SchemeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SchemeServiceImpl implements SchemeService {

    private static final String ACTIVE = "ACTIVE";

    private final SchemeRepository schemeRepo;
    private final SchemeStageRepository stageRepo;
    private final ExperimentGroupRepository groupRepo;
    private final SchemeGroupRepository schemeGroupRepo;
    private final SchemeStageDeviceConfigRepository stageDeviceConfigRepo;
    private final DeviceRepository deviceRepo;

    public SchemeServiceImpl(
            SchemeRepository schemeRepo,
            SchemeStageRepository stageRepo,
            ExperimentGroupRepository groupRepo,
            SchemeGroupRepository schemeGroupRepo,
            SchemeStageDeviceConfigRepository stageDeviceConfigRepo,
            DeviceRepository deviceRepo
    ) {
        this.schemeRepo = schemeRepo;
        this.stageRepo = stageRepo;
        this.groupRepo = groupRepo;
        this.schemeGroupRepo = schemeGroupRepo;
        this.stageDeviceConfigRepo = stageDeviceConfigRepo;
        this.deviceRepo = deviceRepo;
    }

    /**
     * 研究者后台：按组查看方案（组内方案）
     * - groupId == null：这里按你们需求，我默认返回 GLOBAL
     * - groupId != null：走 scheme_group 中间表
     */
    @Override
    public Page<Scheme> list(Long groupId, Pageable pageable) {
        if (groupId == null) {
            return schemeRepo.findByScopeAndStatus(SchemeScope.GLOBAL, ACTIVE, pageable);
        }
        List<Long> schemeIds = schemeGroupRepo.findSchemeIdsByGroupId(groupId);
        if (schemeIds == null || schemeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return schemeRepo.findByIdInAndStatus(schemeIds, ACTIVE, pageable);
    }

    /**
     * 单组创建（CreateSchemeReq 只有一个 groupId）
     * - scope == null 默认 GROUP
     * - GLOBAL：groupId 必须为 null
     * - GROUP：groupId 必须非 null，并写入 scheme_group
     */
    @Override
    @Transactional
    public Scheme create(SchemeDtos.CreateSchemeReq req) {
        if (req == null) {
            throw new BizException(40000, "request is null", HttpStatus.BAD_REQUEST);
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new BizException(40001, "scheme name is required", HttpStatus.BAD_REQUEST);
        }
        if (schemeRepo.existsByName(req.getName())) {
            throw new BizException(40911, "scheme name exists", HttpStatus.CONFLICT);
        }

        // 校验 scope + groupId 合法性（scope null 默认 GROUP）
        validateScope(req.getScope(), req.getGroupId());
        SchemeScope scope = (req.getScope() == null ? SchemeScope.GROUP : req.getScope());

        LocalDateTime now = LocalDateTime.now();

        Scheme s = new Scheme();
        s.setName(req.getName());
        s.setDescription(req.getDescription());
        s.setStatus(ACTIVE);
        s.setScope(scope);

        // 强烈建议：统一不用 scheme.group_id（避免两套逻辑打架）
        s.setGroupId(null);

        s.setCreatedAt(now);
        s.setUpdatedAt(now);

        // GLOBAL：只存 scheme，不写 scheme_group
        if (scope == SchemeScope.GLOBAL) {
            return schemeRepo.save(s);
        }

        // GROUP：必须有 groupId，并写 scheme_group
        Long groupId = req.getGroupId();
        ExperimentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new BizException(40410, "group not found: " + groupId, HttpStatus.NOT_FOUND));

        Scheme saved = schemeRepo.save(s);

        SchemeGroup link = new SchemeGroup();
        link.setSchemeId(saved.getId());
        link.setGroupId(group.getId());
        schemeGroupRepo.save(link);

        return saved;
    }

    @Override
    public Scheme update(Long id, SchemeDtos.UpdateSchemeReq req) {
        Scheme s = schemeRepo.findById(id)
                .orElseThrow(() -> new BizException(40420, "scheme not found", HttpStatus.NOT_FOUND));

        // 你们 UpdateSchemeReq 目前看起来是 public 字段：req.name/req.description/req.status
        // 如果你们后来改成 getter，这里相应改一下即可
        s.setName(req.name);
        s.setDescription(req.description);
        s.setStatus(req.status);

        s.setUpdatedAt(LocalDateTime.now());
        return schemeRepo.save(s);
    }

    @Override
    @Transactional
    public Scheme updateWithStages(Long id, SchemeDtos.UpdateSchemeWithStagesReq req) {
        if (req == null) {
            throw new BizException(40000, "request is null", HttpStatus.BAD_REQUEST);
        }
        if (req.name == null || req.name.trim().isEmpty()) {
            throw new BizException(40001, "scheme name is required", HttpStatus.BAD_REQUEST);
        }
        if (req.stages == null || req.stages.isEmpty()) {
            throw new BizException(40002, "stages is required", HttpStatus.BAD_REQUEST);
        }

        // 检查方案是否存在
        Scheme scheme = schemeRepo.findById(id)
                .orElseThrow(() -> new BizException(40420, "scheme not found", HttpStatus.NOT_FOUND));

        // 检查名称是否与其他方案重复
        if (!scheme.getName().equals(req.name) && schemeRepo.existsByName(req.name)) {
            throw new BizException(40911, "scheme name exists", HttpStatus.CONFLICT);
        }

        boolean hasGroups = req.groupId != null && !req.groupId.isEmpty();
        SchemeScope scope = hasGroups ? SchemeScope.GROUP : SchemeScope.GLOBAL;

        // 只有 GROUP 才校验 groupId 都存在（避免 GLOBAL NPE）
        if (hasGroups) {
            List<Long> groupIds = req.groupId;
            List<Long> exists = groupRepo.findAllById(groupIds).stream()
                    .map(ExperimentGroup::getId)
                    .collect(Collectors.toList());

            if (exists.size() != groupIds.size()) {
                Set<Long> missing = new LinkedHashSet<>(groupIds);
                missing.removeAll(new HashSet<>(exists));
                throw new BizException(40410, "group not found: " + missing, HttpStatus.NOT_FOUND);
            }
        }

        // stageNo 唯一性校验
        Set<Integer> stageNos = new HashSet<>();
        for (SchemeDtos.UpdateSchemeWithStagesReq.UpdateStageReq s : req.stages) {
            if (s == null) {
                throw new BizException(40003, "stage item is null", HttpStatus.BAD_REQUEST);
            }
            if (!stageNos.add(s.stageNo)) {
                throw new BizException(40021, "duplicate stageNo: " + s.stageNo, HttpStatus.BAD_REQUEST);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新方案基本信息
        scheme.setName(req.name);
        scheme.setDescription(req.description);
        scheme.setStatus(req.status);
        scheme.setScope(scope);

        // 统一不用 scheme.group_id，避免与 scheme_group 两套逻辑冲突
        scheme.setGroupId(null);

        scheme.setUpdatedAt(now);

        // 保存方案基本信息
        scheme = schemeRepo.save(scheme);
        Long schemeId = scheme.getId();

        // 删除原有的方案组关联关系
        List<SchemeGroup> existingGroups = schemeGroupRepo.findBySchemeId(schemeId);
        if (!existingGroups.isEmpty()) {
            schemeGroupRepo.deleteAll(existingGroups);
        }

        // GROUP：写 scheme_group 多条绑定（去重处理，避免违反唯一约束）
        if (hasGroups) {
            Set<Long> uniqueGroupIds = new HashSet<>(req.groupId);
            List<SchemeGroup> schemeGroups = new ArrayList<>();
            for (Long gid : uniqueGroupIds) {
                SchemeGroup link = new SchemeGroup();
                link.setSchemeId(schemeId);
                link.setGroupId(gid);
                schemeGroups.add(link);
            }
            // 批量保存，避免逐个保存时可能的部分成功导致的约束冲突
            schemeGroupRepo.saveAll(schemeGroups);
        }

        // 处理阶段：有的update，没有的添加，多的delete
        List<SchemeStage> oldStages = stageRepo.findBySchemeIdOrderByStageNoAsc(schemeId);
        Map<Integer, SchemeStage> oldStageMap = new HashMap<>();
        for (SchemeStage stage : oldStages) {
            oldStageMap.put(stage.getStageNo(), stage);
        }
        
        // 存储需要保存的阶段
        List<SchemeStage> stagesToSave = new ArrayList<>();
        // 存储新的stageNo列表
        Set<Integer> newStageNos = new HashSet<>();
        
        // 遍历新的阶段列表
        for (SchemeDtos.UpdateSchemeWithStagesReq.UpdateStageReq stageReq : req.stages) {
            Integer stageNo = stageReq.stageNo;
            newStageNos.add(stageNo);
            
            SchemeStage stage;
            if (oldStageMap.containsKey(stageNo)) {
                // 存在相同stageNo的阶段，进行更新
                stage = oldStageMap.get(stageNo);
                stage.setName(stageReq.name);
                stage.setDurationMinutes(stageReq.durationMinutes);
                stage.setLightIntensity(stageReq.lightIntensity);
                stage.setLightColorTemp(stageReq.lightColorTemp);
                stage.setNotes(stageReq.description);
                stage.setUpdatedAt(now);
                
                // 删除该阶段的原有设备配置
                stageDeviceConfigRepo.deleteByStageId(stage.getId());
                // 立即刷新到数据库，确保删除操作生效
                stageDeviceConfigRepo.flush();
            } else {
                // 不存在相同stageNo的阶段，创建新的
                stage = new SchemeStage();
                stage.setSchemeId(schemeId);
                stage.setStageNo(stageNo);
                stage.setName(stageReq.name);
                stage.setDurationMinutes(stageReq.durationMinutes);
                stage.setLightIntensity(stageReq.lightIntensity);
                stage.setLightColorTemp(stageReq.lightColorTemp);
                stage.setNotes(stageReq.description);
                stage.setCreatedAt(now);
                stage.setUpdatedAt(now);
            }
            stagesToSave.add(stage);
        }
        
        // 删除多余的阶段（原有的阶段中stageNo不在新列表中的）
        for (SchemeStage stage : oldStages) {
            if (!newStageNos.contains(stage.getStageNo())) {
                // 删除该阶段的设备配置
                stageDeviceConfigRepo.deleteByStageId(stage.getId());
                // 删除该阶段
                stageRepo.delete(stage);
            }
        }
        
        // 保存阶段
        List<SchemeStage> savedStages = stageRepo.saveAll(stagesToSave);
        // 立即刷新到数据库，确保阶段保存操作生效
        stageRepo.flush();
        
        // 构建stageNo到savedStage的映射，用于处理设备配置
        Map<Integer, SchemeStage> savedStageMap = new HashMap<>();
        for (SchemeStage stage : savedStages) {
            savedStageMap.put(stage.getStageNo(), stage);
        }
        
        // 保存设备独立配置
        for (SchemeDtos.UpdateSchemeWithStagesReq.UpdateStageReq stageReq : req.stages) {
            SchemeStage savedStage = savedStageMap.get(stageReq.stageNo);
            
            if (stageReq.deviceConfigs != null && !stageReq.deviceConfigs.isEmpty()) {
                // 先获取该阶段的所有原有设备配置，构建id到配置的映射
                Map<Long, SchemeStageDeviceConfig> existingConfigsByIdMap = new HashMap<>();
                Map<Long, SchemeStageDeviceConfig> existingConfigsByDeviceIdMap = new HashMap<>();
                List<SchemeStageDeviceConfig> existingConfigs = stageDeviceConfigRepo.findByStageId(savedStage.getId());
                for (SchemeStageDeviceConfig config : existingConfigs) {
                    existingConfigsByIdMap.put(config.getId(), config);
                    existingConfigsByDeviceIdMap.put(config.getDeviceId(), config);
                }
                
                List<SchemeStageDeviceConfig> deviceConfigs = new ArrayList<>();
                List<Long> processedConfigIds = new ArrayList<>();
                // 用于跟踪前端提供的设备配置中已经处理过的deviceId，避免重复处理
                Set<Long> processedDeviceIds = new HashSet<>();
                
                for (SchemeDtos.UpdateSchemeWithStagesReq.UpdateStageReq.DeviceConfig deviceConfig : stageReq.deviceConfigs) {
                    // 根据deviceSn查询设备
                    Device device = deviceRepo.findByDeviceSn(deviceConfig.deviceSn)
                            .orElseThrow(() -> new BizException(40041, "device not found: " + deviceConfig.deviceSn, HttpStatus.BAD_REQUEST));
                    
                    // 检查该deviceId是否已经处理过，如果是则跳过
                    if (processedDeviceIds.contains(device.getId())) {
                        continue;
                    }
                    
                    SchemeStageDeviceConfig config;
                    boolean isNewConfig = false;
                    
                    if (deviceConfig.id != null && existingConfigsByIdMap.containsKey(deviceConfig.id)) {
                        // 如果提供了id且存在，则根据id更新
                        config = existingConfigsByIdMap.get(deviceConfig.id);
                        // 如果deviceId发生了变化，需要从原deviceId的映射中移除
                        if (!config.getDeviceId().equals(device.getId())) {
                            existingConfigsByDeviceIdMap.remove(config.getDeviceId());
                        }
                        config.setDeviceId(device.getId());
                        config.setDeviceSn(deviceConfig.deviceSn);
                        config.setLightIntensity(deviceConfig.lightIntensity);
                        config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                        config.setLightColorTemp(deviceConfig.lightColorTemp);
                        config.setUpdatedAt(now);
                        // 记录已处理的id
                        processedConfigIds.add(deviceConfig.id);
                        // 从映射中移除，剩余的将被删除
                        existingConfigsByDeviceIdMap.remove(config.getDeviceId());
                    } else if (existingConfigsByDeviceIdMap.containsKey(device.getId())) {
                        // 如果没有提供id或id不存在，但deviceId存在，则更新
                        config = existingConfigsByDeviceIdMap.get(device.getId());
                        config.setDeviceSn(deviceConfig.deviceSn);
                        config.setLightIntensity(deviceConfig.lightIntensity);
                        config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                        config.setLightColorTemp(deviceConfig.lightColorTemp);
                        config.setUpdatedAt(now);
                        // 记录已处理的id
                        processedConfigIds.add(config.getId());
                        // 从映射中移除，剩余的将被删除
                        existingConfigsByDeviceIdMap.remove(config.getDeviceId());
                    } else {
                        // 如果都不存在，则创建新的
                        config = new SchemeStageDeviceConfig();
                        config.setStageId(savedStage.getId());
                        config.setDeviceId(device.getId());
                        config.setDeviceSn(deviceConfig.deviceSn);
                        config.setLightIntensity(deviceConfig.lightIntensity);
                        config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                        config.setLightColorTemp(deviceConfig.lightColorTemp);
                        config.setCreatedAt(now);
                        config.setUpdatedAt(now);
                        isNewConfig = true;
                    }
                    
                    // 添加到设备配置列表
                    deviceConfigs.add(config);
                    // 标记该deviceId已经处理过
                    processedDeviceIds.add(device.getId());
                }
                
                // 保存或更新设备配置
                stageDeviceConfigRepo.saveAll(deviceConfigs);
                // 立即刷新到数据库，确保保存操作生效
                stageDeviceConfigRepo.flush();
                
                // 删除剩余的原有配置（前端没有提供的配置）
                if (!existingConfigsByDeviceIdMap.isEmpty()) {
                    List<SchemeStageDeviceConfig> configsToDelete = new ArrayList<>(existingConfigsByDeviceIdMap.values());
                    stageDeviceConfigRepo.deleteAll(configsToDelete);
                    // 立即刷新到数据库，确保删除操作生效
                    stageDeviceConfigRepo.flush();
                }
            } else {
                // 如果前端没有提供设备配置，则删除所有原有配置
                stageDeviceConfigRepo.deleteByStageId(savedStage.getId());
                // 立即刷新到数据库，确保删除操作生效
                stageDeviceConfigRepo.flush();
            }
        }

        return scheme;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 级联删除：先删除方案的所有阶段设备配置
        List<SchemeStage> stages = stageRepo.findBySchemeIdOrderByStageNoAsc(id);
        if (!stages.isEmpty()) {
            List<Long> stageIds = stages.stream().map(SchemeStage::getId).collect(Collectors.toList());
            stageDeviceConfigRepo.deleteByStageIdIn(stageIds);
            // 再删除方案的所有阶段
            stageRepo.deleteBySchemeId(id);
        }
        // 最后删除方案本身
        schemeRepo.deleteById(id);
    }

    @Override
    public List<SchemeStage> listStages(Long schemeId) {
        return stageRepo.findBySchemeIdOrderByStageNoAsc(schemeId);
    }

    @Override
    public SchemeStage createStage(Long schemeId, SchemeDtos.CreateStageReq req) {
        schemeRepo.findById(schemeId)
                .orElseThrow(() -> new BizException(40420, "scheme not found", HttpStatus.NOT_FOUND));

        SchemeStage st = new SchemeStage();
        st.setSchemeId(schemeId);
        st.setStageNo(req.stageNo);
        st.setName(req.name);
        st.setDurationMinutes(req.durationMinutes);
        st.setNotes(req.description);
        st.setLightIntensity(req.lightIntensity);
        st.setLightColorTemp(req.lightColorTemp);

        LocalDateTime now = LocalDateTime.now();
        st.setCreatedAt(now);
        st.setUpdatedAt(now);

        // 保存stage
        SchemeStage savedStage = stageRepo.save(st);
        
        // 保存设备独立配置
        if (req.deviceConfigs != null && !req.deviceConfigs.isEmpty()) {
            List<SchemeStageDeviceConfig> deviceConfigs = new ArrayList<>();
            for (SchemeDtos.CreateStageReq.DeviceConfig deviceConfig : req.deviceConfigs) {
                // 查找设备
                Device device = deviceRepo.findByDeviceSn(deviceConfig.deviceSn)
                        .orElseThrow(() -> new BizException(40430, "device not found: " + deviceConfig.deviceSn, HttpStatus.NOT_FOUND));
                
                SchemeStageDeviceConfig config = new SchemeStageDeviceConfig();
                config.setStageId(savedStage.getId());
                config.setDeviceId(device.getId());
                config.setDeviceSn(deviceConfig.deviceSn);
                config.setLightIntensity(deviceConfig.lightIntensity);
                config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                config.setLightColorTemp(deviceConfig.lightColorTemp);
                config.setCreatedAt(now);
                config.setUpdatedAt(now);
                deviceConfigs.add(config);
            }
            stageDeviceConfigRepo.saveAll(deviceConfigs);
        }

        return savedStage;
    }

    @Override
    public SchemeStage updateStage(Long id, SchemeDtos.UpdateStageReq req) {
        SchemeStage st = stageRepo.findById(id)
                .orElseThrow(() -> new BizException(40421, "stage not found", HttpStatus.NOT_FOUND));
        st.setStageNo(req.stageNo);
        st.setName(req.name);
        st.setDurationMinutes(req.durationMinutes);
        st.setNotes(req.description);
        st.setLightIntensity(req.lightIntensity);
        st.setLightColorTemp(req.lightColorTemp);
        st.setUpdatedAt(LocalDateTime.now());

        // 保存stage
        SchemeStage savedStage = stageRepo.save(st);
        
        // 更新设备独立配置：先删除旧的配置，再保存新的配置
        stageDeviceConfigRepo.deleteByStageId(savedStage.getId());
        
        if (req.deviceConfigs != null && !req.deviceConfigs.isEmpty()) {
            List<SchemeStageDeviceConfig> deviceConfigs = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (SchemeDtos.UpdateStageReq.DeviceConfig deviceConfig : req.deviceConfigs) {
                // 查找设备
                Device device = deviceRepo.findByDeviceSn(deviceConfig.deviceSn)
                        .orElseThrow(() -> new BizException(40430, "device not found: " + deviceConfig.deviceSn, HttpStatus.NOT_FOUND));
                
                SchemeStageDeviceConfig config = new SchemeStageDeviceConfig();
                config.setStageId(savedStage.getId());
                config.setDeviceId(device.getId());
                config.setDeviceSn(deviceConfig.deviceSn);
                config.setLightIntensity(deviceConfig.lightIntensity);
                config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                config.setLightColorTemp(deviceConfig.lightColorTemp);
                config.setCreatedAt(now);
                config.setUpdatedAt(now);
                deviceConfigs.add(config);
            }
            stageDeviceConfigRepo.saveAll(deviceConfigs);
        }

        return savedStage;
    }

    @Override
    public void deleteStage(Long id) {
        // 先删除设备独立配置
        stageDeviceConfigRepo.deleteByStageId(id);
        // 再删除stage
        stageRepo.deleteById(id);
    }

    /**
     * 批量创建：scheme + stages，并且（可选）绑定多个 groupId
     * - req.groupId 为空/NULL => GLOBAL
     * - req.groupId 非空     => GROUP + 写 scheme_group 多条记录
     */
    @Override
    @Transactional
    public Scheme createStageWithStages(CreateSchemeWithStagesReq req) {
        if (req == null) {
            throw new BizException(40000, "request is null", HttpStatus.BAD_REQUEST);
        }
        if (req.name == null || req.name.trim().isEmpty()) {
            throw new BizException(40001, "scheme name is required", HttpStatus.BAD_REQUEST);
        }
        if (req.stages == null || req.stages.isEmpty()) {
            throw new BizException(40002, "stages is required", HttpStatus.BAD_REQUEST);
        }

        if (schemeRepo.existsByName(req.name)) {
            throw new BizException(40911, "scheme name exists", HttpStatus.CONFLICT);
        }

        boolean hasGroups = req.groupId != null && !req.groupId.isEmpty();
        SchemeScope scope = hasGroups ? SchemeScope.GROUP : SchemeScope.GLOBAL;

        // 只有 GROUP 才校验 groupId 都存在（避免 GLOBAL NPE）
        if (hasGroups) {
            List<Long> groupIds = req.groupId;
            List<Long> exists = groupRepo.findAllById(groupIds).stream()
                    .map(ExperimentGroup::getId)
                    .collect(Collectors.toList());

            if (exists.size() != groupIds.size()) {
                Set<Long> missing = new LinkedHashSet<>(groupIds);
                missing.removeAll(new HashSet<>(exists));
                throw new BizException(40410, "group not found: " + missing, HttpStatus.NOT_FOUND);
            }
        }

        // stageNo 唯一性校验
        Set<Integer> stageNos = new HashSet<>();
        for (CreateSchemeWithStagesReq.CreateStageReq s : req.stages) {
            if (s == null) {
                throw new BizException(40003, "stage item is null", HttpStatus.BAD_REQUEST);
            }
            if (!stageNos.add(s.stageNo)) {
                throw new BizException(40021, "duplicate stageNo: " + s.stageNo, HttpStatus.BAD_REQUEST);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Scheme scheme = new Scheme();
        scheme.setName(req.name);
        scheme.setDescription(req.description);
        scheme.setStatus(ACTIVE);
        scheme.setScope(scope);

        // 统一不用 scheme.group_id，避免与 scheme_group 两套逻辑冲突
        scheme.setGroupId(null);

        scheme.setCreatedAt(now);
        scheme.setUpdatedAt(now);

        scheme = schemeRepo.save(scheme);
        Long schemeId = scheme.getId();

        // GROUP：写 scheme_group 多条绑定（去重处理，避免违反唯一约束）
        if (hasGroups) {
            Set<Long> uniqueGroupIds = new HashSet<>(req.groupId);
            List<SchemeGroup> schemeGroups = new ArrayList<>();
            for (Long gid : uniqueGroupIds) {
                SchemeGroup link = new SchemeGroup();
                link.setSchemeId(schemeId);
                link.setGroupId(gid);
                schemeGroups.add(link);
            }
            // 批量保存，避免逐个保存时可能的部分成功导致的约束冲突
            schemeGroupRepo.saveAll(schemeGroups);
        }

        // 批量写 stages
        List<SchemeStage> stages = req.stages.stream().map(s -> {
            SchemeStage st = new SchemeStage();
            st.setSchemeId(schemeId);
            st.setStageNo(s.stageNo);
            st.setName(s.name);
            st.setDurationMinutes(s.durationMinutes);
            st.setLightIntensity(s.lightIntensity);
            st.setLightColorTemp(s.lightColorTemp);
            st.setNotes(s.description);
            st.setCreatedAt(now);
            st.setUpdatedAt(now);
            return st;
        }).collect(Collectors.toList());

        // 保存阶段并处理设备配置
        stageRepo.saveAll(stages);
        
        // 处理设备配置
        for (int i = 0; i < req.stages.size(); i++) {
            CreateSchemeWithStagesReq.CreateStageReq stageReq = req.stages.get(i);
            SchemeStage stage = stages.get(i);
            
            if (stageReq.deviceConfigs != null && !stageReq.deviceConfigs.isEmpty()) {
                List<SchemeStageDeviceConfig> deviceConfigs = new ArrayList<>();
                for (CreateSchemeWithStagesReq.CreateStageReq.DeviceConfig deviceConfig : stageReq.deviceConfigs) {
                    // 查找设备
                    Device device = deviceRepo.findByDeviceSn(deviceConfig.deviceSn)
                            .orElseThrow(() -> new BizException(40430, "device not found: " + deviceConfig.deviceSn, HttpStatus.NOT_FOUND));
                    
                    SchemeStageDeviceConfig config = new SchemeStageDeviceConfig();
                    config.setStageId(stage.getId());
                    config.setDeviceId(device.getId());
                    config.setDeviceSn(deviceConfig.deviceSn);
                    config.setLightIntensity(deviceConfig.lightIntensity);
                    config.setSkyLightIntensity(deviceConfig.skyLightIntensity);
                    config.setLightColorTemp(deviceConfig.lightColorTemp);
                    config.setCreatedAt(now);
                    config.setUpdatedAt(now);
                    deviceConfigs.add(config);
                }
                stageDeviceConfigRepo.saveAll(deviceConfigs);
            }
        }
        
        return scheme;
    }

    /**
     * 全局固定方案列表（分页）
     */
    @Override
    public Page<Scheme> listGlobalActive(Pageable pageable) {
        return schemeRepo.findByScopeAndStatus(SchemeScope.GLOBAL, ACTIVE, pageable);
    }

    /**
     * 所有组方案列表（分页）
     */
    @Override
    public Page<Scheme> listAllGroupActive(Pageable pageable) {
        return schemeRepo.findByScopeAndStatus(SchemeScope.GROUP, ACTIVE, pageable);
    }

    @Override
    public Page<Scheme> listAllActive(Pageable pageable) {
        return schemeRepo.findByStatus(ACTIVE, pageable);
    }

    @Override
    public Page<SchemeDtos.SchemeWithStagesDTO> listAllActiveWithStages(Pageable pageable) {
        // 1. 查询所有状态为ACTIVE的方案
        Page<Scheme> schemesPage = schemeRepo.findByStatus(ACTIVE, pageable);
        return convertToSchemeWithStagesDTO(schemesPage);
    }

    @Override
    public Page<SchemeDtos.SchemeWithStagesDTO> listGlobalActiveWithStages(Pageable pageable) {
        // 1. 查询全局状态为ACTIVE的方案
        Page<Scheme> schemesPage = schemeRepo.findByScopeAndStatus(SchemeScope.GLOBAL, ACTIVE, pageable);
        return convertToSchemeWithStagesDTO(schemesPage);
    }

    @Override
    public Page<SchemeDtos.SchemeWithStagesDTO> listGroupActiveWithStages(Long groupId, Pageable pageable) {
        if (groupId == null) {
            return Page.empty(pageable);
        }
        List<Long> schemeIds = schemeGroupRepo.findSchemeIdsByGroupId(groupId);
        if (schemeIds == null || schemeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        // 1. 查询组内状态为ACTIVE的方案
        Page<Scheme> schemesPage = schemeRepo.findByIdInAndStatus(schemeIds, ACTIVE, pageable);
        return convertToSchemeWithStagesDTO(schemesPage);
    }

    @Override
    public Page<SchemeDtos.SchemeWithStagesDTO> listRecommendedWithStages(Long groupId, Pageable pageable) {
        // 1. 获取推荐方案
        Page<Scheme> schemesPage = listRecommended(groupId, pageable);
        return convertToSchemeWithStagesDTO(schemesPage);
    }

    /**
     * 将Scheme分页结果转换为SchemeWithStagesDTO分页结果
     */
    @Transactional
    public Page<SchemeDtos.SchemeWithStagesDTO> convertToSchemeWithStagesDTO(Page<Scheme> schemesPage) {
        List<Scheme> schemes = schemesPage.getContent();
        
        if (schemes.isEmpty()) {
            return Page.empty(schemesPage.getPageable());
        }
        
        // 收集所有方案的ID
        List<Long> schemeIds = schemes.stream().map(Scheme::getId).collect(Collectors.toList());
        
        // 查询所有方案关联的scheme_group记录
        List<SchemeGroup> allSchemeGroups = schemeGroupRepo.findBySchemeIdIn(schemeIds);
        
        // 收集所有需要查询的groupIds
        Set<Long> groupIdsSet = new HashSet<>();
        for (SchemeGroup schemeGroup : allSchemeGroups) {
            groupIdsSet.add(schemeGroup.getGroupId());
        }
        // 另外，还需要添加scheme表中直接存储的groupId
        for (Scheme scheme : schemes) {
            if (scheme.getGroupId() != null) {
                groupIdsSet.add(scheme.getGroupId());
            }
        }
        
        // 查询所有group的详细信息，使用fetch join立即加载surveyTemplates集合
        List<ExperimentGroup> allGroups = new ArrayList<>();
        if (!groupIdsSet.isEmpty()) {
            allGroups = groupRepo.findAllWithSurveyTemplates(groupIdsSet);
        }
        // 创建groupId到ExperimentGroup的映射，方便查询
        Map<Long, ExperimentGroup> groupMap = new HashMap<>();
        for (ExperimentGroup group : allGroups) {
            groupMap.put(group.getId(), group);
        }
        
        // 创建schemeId到groupIds的映射
        Map<Long, List<Long>> schemeToGroupIdsMap = new HashMap<>();
        for (SchemeGroup schemeGroup : allSchemeGroups) {
            schemeToGroupIdsMap.computeIfAbsent(schemeGroup.getSchemeId(), k -> new ArrayList<>())
                    .add(schemeGroup.getGroupId());
        }
        
        // 对每个方案，查询其关联的stage和scheme_stage_device_config
        List<SchemeDtos.SchemeWithStagesDTO> dtos = schemes.stream().map(scheme -> {
            SchemeDtos.SchemeWithStagesDTO dto = new SchemeDtos.SchemeWithStagesDTO();
            dto.setId(scheme.getId());
            dto.setGroupId(scheme.getGroupId());
            dto.setScope(scheme.getScope());
            dto.setName(scheme.getName());
            dto.setDescription(scheme.getDescription());
            dto.setStatus(scheme.getStatus());
            dto.setCreatedAt(scheme.getCreatedAt());
            dto.setUpdatedAt(scheme.getUpdatedAt());

            // 获取该方案关联的所有groupId
            List<Long> groupIds = new ArrayList<>();
            
            // 对于GLOBAL方案，不应该有组信息，直接设置为空列表
            if (scheme.getScope() != SchemeScope.GLOBAL) {
                // 从scheme_group表中获取关联的groupId
                if (schemeToGroupIdsMap.containsKey(scheme.getId())) {
                    groupIds.addAll(schemeToGroupIdsMap.get(scheme.getId()));
                }
                // 如果groupIds为空，并且scheme.groupId不为null，那么将scheme.groupId添加到groupIds中
                if (groupIds.isEmpty() && scheme.getGroupId() != null) {
                    groupIds.add(scheme.getGroupId());
                }
                // 去重
                groupIds = groupIds.stream().distinct().collect(Collectors.toList());
            }
            
            dto.setGroupIds(groupIds);
            
            // 获取该方案关联的所有group详细信息
            List<SchemeDtos.ExperimentGroupDTO> groupDtos = new ArrayList<>();
            
            // 对于GLOBAL方案，不应该有组信息，直接设置为空列表
            if (scheme.getScope() != SchemeScope.GLOBAL) {
                for (Long groupId : groupIds) {
                    ExperimentGroup group = groupMap.get(groupId);
                    if (group != null) {
                        SchemeDtos.ExperimentGroupDTO groupDto = new SchemeDtos.ExperimentGroupDTO();
                        groupDto.setId(group.getId());
                        groupDto.setName(group.getName());
                        groupDto.setDescription(group.getDescription());
                        groupDto.setSurveyTemplateIds(group.getSurveyTemplates().stream()
                                .map(com.lontri.lighttherapy.entity.SurveyTemplate::getId)
                                .collect(java.util.stream.Collectors.toList()));
                        groupDto.setStatus(group.getStatus());
                        groupDto.setCreatedAt(group.getCreatedAt());
                        groupDto.setUpdatedAt(group.getUpdatedAt());
                        groupDtos.add(groupDto);
                    }
                }
            }
            
            dto.setGroups(groupDtos);

            // 查询该方案的所有stage
            List<SchemeStage> stages = stageRepo.findBySchemeIdOrderByStageNoAsc(scheme.getId());
            List<SchemeDtos.SchemeStageDTO> stageDtos = stages.stream().map(stage -> {
                SchemeDtos.SchemeStageDTO stageDto = new SchemeDtos.SchemeStageDTO();
                stageDto.setId(stage.getId());
                stageDto.setSchemeId(stage.getSchemeId());
                stageDto.setStageNo(stage.getStageNo());
                stageDto.setName(stage.getName());
                stageDto.setStartDayOffset(stage.getStartDayOffset());
                stageDto.setDurationMinutes(stage.getDurationMinutes());
                stageDto.setLightIntensity(stage.getLightIntensity());
                stageDto.setLightColorTemp(stage.getLightColorTemp());
                stageDto.setSessionCountPerDay(stage.getSessionCountPerDay());
                stageDto.setNotes(stage.getNotes());
                stageDto.setCreatedAt(stage.getCreatedAt());
                stageDto.setUpdatedAt(stage.getUpdatedAt());

                // 查询该stage的所有scheme_stage_device_config
                List<SchemeStageDeviceConfig> deviceConfigs = stageDeviceConfigRepo.findByStageId(stage.getId());
                List<SchemeDtos.SchemeStageDeviceConfigDTO> deviceConfigDtos = deviceConfigs.stream().map(config -> {
                    SchemeDtos.SchemeStageDeviceConfigDTO configDto = new SchemeDtos.SchemeStageDeviceConfigDTO();
                    configDto.setId(config.getId());
                    configDto.setStageId(config.getStageId());
                    configDto.setDeviceId(config.getDeviceId());
                    configDto.setDeviceSn(config.getDeviceSn());
                    configDto.setLightIntensity(config.getLightIntensity());
                    configDto.setLightColorTemp(config.getLightColorTemp());
                    configDto.setSkyLightIntensity(config.getSkyLightIntensity());
                    configDto.setCreatedAt(config.getCreatedAt());
                    configDto.setUpdatedAt(config.getUpdatedAt());
                    return configDto;
                }).collect(Collectors.toList());

                stageDto.setDeviceConfigs(deviceConfigDtos);
                return stageDto;
            }).collect(Collectors.toList());

            dto.setStages(stageDtos);
            return dto;
        }).collect(Collectors.toList());

        // 返回分页结果
        return new PageImpl<>(dtos, schemesPage.getPageable(), schemesPage.getTotalElements());
    }

    /**
     * 组内方案列表（分页）
     * - 走 scheme_group 获取 schemeIds，再回查 scheme
     */
    @Override
    public Page<Scheme> listGroupActive(Long groupId, Pageable pageable) {
        if (groupId == null) {
            return Page.empty(pageable);
        }
        List<Long> schemeIds = schemeGroupRepo.findSchemeIdsByGroupId(groupId);
        if (schemeIds == null || schemeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return schemeRepo.findByIdInAndStatus(schemeIds, ACTIVE, pageable);
    }

    /**
     * 受试者推荐方案：GLOBAL + GROUP（置顶 GLOBAL，合并后分页）
     */
    @Override
    public Page<Scheme> listRecommended(Long groupId, Pageable pageable) {
        // groupId 可能为 null：只返回全局
        if (groupId == null) {
            return schemeRepo.findByScopeAndStatus(SchemeScope.GLOBAL, ACTIVE, pageable);
        }

        // 1) 全局方案（通常很少，直接查全量）
        List<Scheme> globals = schemeRepo.findByScopeAndStatusOrderByIdDesc(SchemeScope.GLOBAL, ACTIVE);

        // 2) 组内方案（通过中间表）
        List<Long> schemeIds = schemeGroupRepo.findSchemeIdsByGroupId(groupId);
        List<Scheme> groupSchemes = (schemeIds == null || schemeIds.isEmpty())
                ? Collections.emptyList()
                : schemeRepo.findByIdInAndStatus(schemeIds, ACTIVE);

        // 3) 合并 + 排序：GLOBAL 优先，再按 id 倒序
        List<Scheme> merged = new ArrayList<>(globals.size() + groupSchemes.size());
        merged.addAll(globals);
        merged.addAll(groupSchemes);

        merged.sort((a, b) -> {
            int sa = a.getScope() == SchemeScope.GLOBAL ? 0 : 1;
            int sb = b.getScope() == SchemeScope.GLOBAL ? 0 : 1;
            if (sa != sb) return Integer.compare(sa, sb);
            return Long.compare(b.getId(), a.getId());
        });

        // 4) 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), merged.size());
        List<Scheme> content = start >= merged.size() ? Collections.emptyList() : merged.subList(start, end);

        return new PageImpl<>(content, pageable, merged.size());
    }

    @Override
    public Scheme getById(Long id) {
        return schemeRepo.findById(id)
                .orElseThrow(() -> new BizException(40420, "scheme not found", HttpStatus.NOT_FOUND));
    }

    /**
     * scope + groupId 约束校验（scope null 默认 GROUP）
     */
    private void validateScope(SchemeScope scope, Long groupId) {
        SchemeScope s = (scope == null ? SchemeScope.GROUP : scope);

        if (s == SchemeScope.GLOBAL && groupId != null) {
            throw new BizException(40012, "GLOBAL scheme must have groupId = null", HttpStatus.BAD_REQUEST);
        }
        if (s == SchemeScope.GROUP && groupId == null) {
            throw new BizException(40011, "GROUP scheme must have groupId != null", HttpStatus.BAD_REQUEST);
        }
    }
}
