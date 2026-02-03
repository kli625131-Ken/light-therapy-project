package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.dto.AuthDtos;

public interface AuthService {
    AuthDtos.LoginResp login(AuthDtos.LoginReq req);
}
