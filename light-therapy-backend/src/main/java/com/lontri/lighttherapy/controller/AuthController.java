package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.AuthDtos;
import com.lontri.lighttherapy.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResp> login(@RequestBody @Valid AuthDtos.LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }
}
