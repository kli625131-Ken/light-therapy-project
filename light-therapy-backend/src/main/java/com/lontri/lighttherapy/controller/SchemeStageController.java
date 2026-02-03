package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.service.SchemeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping({"/api/scheme-stages", "/api/v1/scheme-stages"})
@PreAuthorize("hasRole('ADMIN')")
public class SchemeStageController {

    private final SchemeService schemeService;

    public SchemeStageController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @PutMapping("/{id}")
    public ApiResponse<SchemeStage> update(@PathVariable Long id, @RequestBody @Valid SchemeDtos.UpdateStageReq req) {
        return ApiResponse.ok(schemeService.updateStage(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        schemeService.deleteStage(id);
        return ApiResponse.ok();
    }
}
