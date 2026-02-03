package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.entity.*;
import com.lontri.lighttherapy.enums.*;
import com.lontri.lighttherapy.service.SchemeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import java.util.List;

@RestController
@RequestMapping({"/api/schemes/me", "/api/v1/schemes/me"})
// @PreAuthorize("hasRole('SUBJECT')")
public class SchemeSelfController {

    private final SchemeService schemeService;

    public SchemeSelfController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @GetMapping
    public ApiResponse<Page<SchemeDtos.SchemeWithStagesDTO>> listscheme(
            @RequestParam(required = false) SchemeScope scope,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        if (scope == SchemeScope.GLOBAL) {
        	return ApiResponse.ok(schemeService.listGlobalActiveWithStages(pageable));
        }
        if (scope == SchemeScope.GROUP && groupId != null) {
            // 直接查询所有GROUP类型的方案，不需要groupId参数
            return ApiResponse.ok(schemeService.listGroupActiveWithStages(groupId, pageable));
        }
        // scope 未传：默认返回推荐方案（GLOBAL + GROUP）
        return ApiResponse.ok(schemeService.listRecommendedWithStages(groupId, pageable));
    }

    @GetMapping("/{schemeId}/stages")
    public ApiResponse<List<SchemeStage>> listStages(@PathVariable Long schemeId) {
        return ApiResponse.ok(schemeService.listStages(schemeId));
    }
}
