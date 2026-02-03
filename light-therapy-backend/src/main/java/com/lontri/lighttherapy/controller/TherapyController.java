package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/therapy", "/api/v1/therapy"})
public class TherapyController {

    @GetMapping("/my")
    public ApiResponse<?> getMyTherapy() {
        // 返回空的成功响应，避免404错误
        return ApiResponse.ok(null);
    }
}
