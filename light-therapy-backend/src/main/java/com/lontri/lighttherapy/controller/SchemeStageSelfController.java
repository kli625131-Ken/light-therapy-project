package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.service.SchemeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping({"/api/scheme-stages/me", "/api/v1/scheme-stages/me"})
@PreAuthorize("hasRole('SUBJECT')")
public class SchemeStageSelfController {

    private final SchemeService schemeService;

    public SchemeStageSelfController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @PutMapping("/{id}")
    public ApiResponse<SchemeStage> update(@PathVariable Long id, @RequestBody @Valid SchemeDtos.UpdateStageReq req) {
        return ApiResponse.ok(schemeService.updateStage(id, req));
    }
}
