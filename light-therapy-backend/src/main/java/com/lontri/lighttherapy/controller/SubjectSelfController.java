package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.entity.Subject;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.service.GroupService;
import com.lontri.lighttherapy.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/subject", "/api/v1/subject"})
@PreAuthorize("hasRole('SUBJECT')")
public class SubjectSelfController {

    private final SubjectRepository subjectRepo;
    private final GroupService groupService; // ken260129-修改内容：注入GroupService

    public SubjectSelfController(SubjectRepository subjectRepo, GroupService groupService) {
        this.subjectRepo = subjectRepo;
        this.groupService = groupService;
    }

    @GetMapping("/me")
    public ApiResponse<Subject> me() {
        Long userId = SecurityUtils.currentUserId();
        Subject s = subjectRepo.findByUserId(userId)
                .orElseThrow(() -> new BizException(40430, "subject not found", HttpStatus.NOT_FOUND));
        return ApiResponse.ok(s);
    }
    
    // ken260129-修改内容：添加获取受试者分组关联的所有量表的API接口
    @GetMapping("/my-survey-templates")
    public ApiResponse<List<SurveyTemplate>> getMySurveyTemplates() {
        Long userId = SecurityUtils.currentUserId();
        Subject subject = subjectRepo.findByUserId(userId)
                .orElseThrow(() -> new BizException(40430, "subject not found", HttpStatus.NOT_FOUND));
        
        Long groupId = subject.getGroupId();
        if (groupId == null) {
            throw new BizException(40431, "subject not in any group", HttpStatus.NOT_FOUND);
        }
        
        List<SurveyTemplate> surveyTemplates = groupService.getGroupSurveyTemplates(groupId);
        return ApiResponse.ok(surveyTemplates);
    }
}
