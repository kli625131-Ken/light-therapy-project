package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.SurveyDtos;
import com.lontri.lighttherapy.dto.SurveyDtos.SubmitSurveyReq;
import com.lontri.lighttherapy.entity.SurveyResult;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.service.SurveyResultService;
import com.lontri.lighttherapy.service.SurveyService;
import com.lontri.lighttherapy.service.SurveyTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping({ "/api/surveys", "/api/v1/surveys" })
public class SurveyController {

    private final SurveyTemplateService surveyTemplateService;
    private final SurveyResultService surveyResultService;
    private final SurveyService surveyService;

    public SurveyController(SurveyTemplateService surveyTemplateService,
            SurveyResultService surveyResultService,
            SurveyService surveyService) {
        this.surveyTemplateService = surveyTemplateService;
        this.surveyResultService = surveyResultService;
        this.surveyService = surveyService;
        System.out.println("### SurveyController LOADED ###");
    }

    // =========================
    // Templates (for "量表模板管理" UI)
    // =========================

    /** 左侧列表：id/name/version/questionCount */
    @GetMapping("/templates")
    public ApiResponse<List<SurveyDtos.TemplateSummary>> listTemplates() {
        return ApiResponse.ok(surveyTemplateService.listTemplates());
    }

    /** 右侧详情：模板 + questions（用于编辑回填） */
    @GetMapping("/templates/{id}")
    public ApiResponse<SurveyDtos.TemplateDetail> templateDetail(@PathVariable Long id) {
        return ApiResponse.ok(surveyTemplateService.getTemplate(id));
    }

    /** 获取默认模板 */
    @GetMapping("/templates/default")
    public ApiResponse<SurveyDtos.TemplateDetail> getDefaultTemplate() {
        return ApiResponse.ok(surveyTemplateService.getDefaultTemplate());
    }

    /** 新建：一次提交模板 + 全量问题 */
    @PostMapping("/templates")
    public ApiResponse<SurveyTemplate> createTemplate(@RequestBody @Valid SurveyDtos.SaveTemplateReq req) {
        return ApiResponse.ok(surveyTemplateService.saveTemplate(req));
    }

    /** 更新：一次提交模板 + 全量问题（后端先删旧 questions 再重建） */
    @PutMapping("/templates/{id}")
    public ApiResponse<SurveyTemplate> updateTemplate(@PathVariable Long id,
            @RequestBody @Valid SurveyDtos.UpdateTemplateReq req) {
        return ApiResponse.ok(surveyTemplateService.updateTemplate(id, req));
    }

    /** 删除模板：同时删除关联的问题 */
    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        surveyTemplateService.deleteTemplate(id);
        return ApiResponse.ok();
    }

    // =========================
    // Survey Result (submit filled survey)
    // =========================

    /** 受试者提交问卷：PENDING -> SUBMITTED */
    @PostMapping("/results/{id}/submit")
    public ApiResponse<SurveyResult> submit(@PathVariable Long id,
            @RequestBody @Valid SubmitSurveyReq req) {
        return ApiResponse.ok(surveyResultService.submit(id, req.rawJson, req.score));
    }

    /** 获取当前用户的问卷反馈历史 */
    @GetMapping("/my")
    public ApiResponse<List<SurveyDtos.SurveyResult>> getMySurveyResults(Principal principal) {
        return ApiResponse.ok(surveyService.getMySurveyResults(principal));
    }

    /** 获取指定用户的问卷反馈历史（仅管理员或研究人员可访问） */
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESEARCHER')")
    @GetMapping("/results/user/{userId}")
    public ApiResponse<List<SurveyDtos.SurveyResult>> getUserSurveyResults(@PathVariable Long userId) {
        return ApiResponse.ok(surveyService.getUserSurveyResults(userId));
    }

    /** 根据 sessionId 获取问卷反馈 */
    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<SurveyDtos.SurveyResult>> getSurveyResultBySession(@PathVariable Long sessionId) {
        return ApiResponse.ok(surveyService.getSurveyResultBySessionId(sessionId));
    }
}
