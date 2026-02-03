package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.dto.SchemeDtos.CreateSchemeWithStagesReq;
import com.lontri.lighttherapy.entity.Scheme;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.enums.SchemeScope;
import com.lontri.lighttherapy.service.SchemeService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping({"/api/schemes", "/api/v1/schemes"})
@PreAuthorize("hasRole('ADMIN')")
public class SchemeController {

    private final SchemeService schemeService;

    public SchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

//    @GetMapping
//    public ApiResponse<?> list(@RequestParam Long groupId,
//                               @RequestParam(defaultValue="0") int page,
//                               @RequestParam(defaultValue="20") int pageSize) {
//        Pageable pageable = PageRequest.of(page, pageSize);
//        return ApiResponse.ok(schemeService.list(groupId, pageable));
//    }
    @GetMapping
    public ApiResponse<Page<Scheme>> list(
            @RequestParam(required = false) SchemeScope scope,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        if (scope == SchemeScope.GLOBAL) {
        	return ApiResponse.ok(schemeService.listGlobalActive(pageable));
        }
        if (scope == SchemeScope.GROUP && groupId != null) {
            // 直接查询所有GROUP类型的方案，不需要groupId参数
            return ApiResponse.ok(schemeService.listGroupActive(groupId, pageable));
        }
        // scope 未传：默认返回推荐方案（GLOBAL + GROUP）
        return ApiResponse.ok(schemeService.listRecommended(groupId, pageable));
    }
    
    @GetMapping("/all")
    @Operation(summary = "查询所有有效方案", description = "不区分scope类型，查询所有状态为ACTIVE的方案，包含stage和scheme_stage_device_config信息")
    public ApiResponse<Page<SchemeDtos.SchemeWithStagesDTO>> listAllActive(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return ApiResponse.ok(schemeService.listAllActiveWithStages(pageable));
    }
    
    @GetMapping("/{id}")
    public ApiResponse<Scheme> get(@PathVariable Long id) {
        System.out.println("[DEBUG] GET /api/schemes/" + id + " 请求到达");
        Scheme scheme = schemeService.getById(id);
        System.out.println("[DEBUG] GET /api/schemes/" + id + " 查询结果: " + scheme);
        if (scheme == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found");
        }
        return ApiResponse.ok(scheme);
    }
    @PostMapping
    public ApiResponse<Scheme> create(@RequestBody @Valid SchemeDtos.CreateSchemeReq req) {
        return ApiResponse.ok(schemeService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Scheme> updateWithStages(@PathVariable Long id, @RequestBody @Valid SchemeDtos.UpdateSchemeWithStagesReq req) {
        return ApiResponse.ok(schemeService.updateWithStages(id, req));
    }

    @PutMapping("/{id}/basic")
    public ApiResponse<Scheme> updateBasic(@PathVariable Long id, @RequestBody @Valid SchemeDtos.UpdateSchemeReq req) {
        return ApiResponse.ok(schemeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        schemeService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{schemeId}/stages")
    public ApiResponse<List<SchemeStage>> listStages(@PathVariable Long schemeId) {
        return ApiResponse.ok(schemeService.listStages(schemeId));
    }

    @PostMapping("/{schemeId}/stages")
    public ApiResponse<SchemeStage> createStage(@PathVariable Long schemeId, @RequestBody @Valid SchemeDtos.CreateStageReq req) {
    	return ApiResponse.ok(schemeService.createStage(schemeId, req));
    }
    
    @PostMapping("/with-stages")
    public ApiResponse<Scheme> createStageWithStages(@RequestBody @Valid CreateSchemeWithStagesReq req) {
    	return ApiResponse.ok(schemeService.createStageWithStages(req));
    }
}
