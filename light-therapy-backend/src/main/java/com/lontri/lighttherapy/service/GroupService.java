package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.dto.GroupDtos;
import com.lontri.lighttherapy.dto.GroupResponseDto;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupService {
    Page<GroupResponseDto> list(Pageable pageable);
    ExperimentGroup create(GroupDtos.CreateGroupReq req);
    ExperimentGroup update(Long id, GroupDtos.UpdateGroupReq req);
    void delete(Long id);
    List<SurveyTemplate> getGroupSurveyTemplates(Long groupId); // ken260129-修改内容：添加获取分组关联的所有量表的方法
}
