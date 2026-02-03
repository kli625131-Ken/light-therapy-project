package com.lontri.lighttherapy.service.impl;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.GroupDtos;
import com.lontri.lighttherapy.dto.GroupResponseDto;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.ExperimentGroupDevice;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.repository.ExperimentGroupDeviceRepository;
import com.lontri.lighttherapy.repository.ExperimentGroupRepository;
import com.lontri.lighttherapy.repository.SchemeRepository;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.SurveyTemplateRepository;
import com.lontri.lighttherapy.service.GroupService;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupServiceImpl implements GroupService {

    private final ExperimentGroupRepository groupRepo;
    private final SubjectRepository subjectRepo;
    private final SchemeRepository schemeRepo;
    private final SurveyTemplateRepository surveyTemplateRepo; // ✅新增
    private final DeviceRepository deviceRepo; // ✅新增
    private final ExperimentGroupDeviceRepository experimentGroupDeviceRepo; // ✅新增

    public GroupServiceImpl(ExperimentGroupRepository groupRepo,
            SubjectRepository subjectRepo,
            SchemeRepository schemeRepo,
            SurveyTemplateRepository surveyTemplateRepo, // ✅新增
            DeviceRepository deviceRepo, // ✅新增
            ExperimentGroupDeviceRepository experimentGroupDeviceRepo) { // ✅新增
		this.groupRepo = groupRepo;
		this.subjectRepo = subjectRepo;
		this.schemeRepo = schemeRepo;
		this.surveyTemplateRepo = surveyTemplateRepo;
		this.deviceRepo = deviceRepo;
		this.experimentGroupDeviceRepo = experimentGroupDeviceRepo;
	}

    @Override
    @Transactional
    public Page<GroupResponseDto> list(Pageable pageable) {
        return groupRepo.findAll(pageable).map(group -> {
            // 确保在事务内初始化集合
            if (group.getSurveyTemplates() != null) {
                group.getSurveyTemplates().size();
            }
            // 获取该分组下的所有设备ID
            List<Long> deviceIds = experimentGroupDeviceRepo.findDeviceIdsByGroupId(group.getId());
            // 获取设备信息
            List<Device> devices = deviceRepo.findByIdIn(deviceIds);
            // 转换为DTO
            return GroupResponseDto.fromEntity(group, devices);
        });
    }

    @Override
    public ExperimentGroup create(GroupDtos.CreateGroupReq req) {
        if (groupRepo.existsByName(req.name)) {
            throw new BizException(40910, "group name exists", HttpStatus.CONFLICT);
        }
        validateSurveyTemplates(req.surveyTemplateIds); // ken260129-修改内容：验证多个量表ID
        
        ExperimentGroup g = new ExperimentGroup();
        g.setName(req.name);
        g.setDescription(req.description);
        g.setStatus("ACTIVE");
        
        // ken260129-修改内容：设置多个量表关联
        if (req.surveyTemplateIds != null && !req.surveyTemplateIds.isEmpty()) {
            g.setSurveyTemplates(surveyTemplateRepo.findAllById(req.surveyTemplateIds));
        }

        ExperimentGroup savedGroup = groupRepo.save(g);

        // 获取要关联的设备ID列表
        List<Long> deviceIdsToLink;
        if (req.deviceIds != null && !req.deviceIds.isEmpty()) {
            // 如果提供了设备ID列表，使用提供的列表
            deviceIdsToLink = req.deviceIds;
        } else {
            // 如果没有提供设备ID列表，默认添加所有设备
            List<Device> allDevices = deviceRepo.findAll();
            deviceIdsToLink = allDevices.stream()
                    .map(Device::getId)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 关联所有设备
        for (Long deviceId : deviceIdsToLink) {
            ExperimentGroupDevice egd = new ExperimentGroupDevice();
            egd.setGroupId(savedGroup.getId());
            egd.setDeviceId(deviceId);
            experimentGroupDeviceRepo.save(egd);
        }
        return savedGroup;
    }

    @Override
    public ExperimentGroup update(Long id, GroupDtos.UpdateGroupReq req) {
        if (groupRepo.existsByNameAndIdNot(req.name, id)) {
            throw new BizException(40910, "group name exists", HttpStatus.CONFLICT);
        }
        ExperimentGroup g = groupRepo.findById(id)
                .orElseThrow(() -> new BizException(40410, "group not found", HttpStatus.NOT_FOUND));
        validateSurveyTemplates(req.surveyTemplateIds); // ken260129-修改内容：验证多个量表ID
        g.setName(req.name);
        g.setDescription(req.description);
        if (req.status != null && !req.status.trim().isEmpty()) g.setStatus(req.status);
        
        // ken260129-修改内容：更新多个量表关联
        if (req.surveyTemplateIds != null && !req.surveyTemplateIds.isEmpty()) {
            g.setSurveyTemplates(surveyTemplateRepo.findAllById(req.surveyTemplateIds));
        } else {
            g.setSurveyTemplates(null);
        }

        return groupRepo.save(g);
    }

    @Override
    public void delete(Long id) {
    	if (subjectRepo.existsByGroupId(id) || schemeRepo.existsByGroupId(id)) {
            throw new IllegalStateException("group has linked subjects or schemes");
        }
        groupRepo.deleteById(id);
    }
    
    @Override
    @Transactional
    public List<SurveyTemplate> getGroupSurveyTemplates(Long groupId) {
        ExperimentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new BizException(40410, "group not found", HttpStatus.NOT_FOUND));
        // 确保在事务内初始化集合
        if (group.getSurveyTemplates() != null) {
            group.getSurveyTemplates().size();
        }
        // 确保返回非null的列表
        return group.getSurveyTemplates() != null ? group.getSurveyTemplates() : java.util.Collections.emptyList();
    }
    
    private void validateSurveyTemplates(List<Long> surveyTemplateIds) {
        if (surveyTemplateIds == null || surveyTemplateIds.isEmpty()) {
            return; // 允许不绑定量表
        }
        for (Long surveyTemplateId : surveyTemplateIds) {
            if (!surveyTemplateRepo.existsById(surveyTemplateId)) {
                throw new BizException(
                    40450,
                    "survey template not found: " + surveyTemplateId,
                    HttpStatus.NOT_FOUND
                );
            }
        }
    }

}
