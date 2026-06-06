package com.example.community_group_buy_backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.dto.LoginDTO;
import com.example.community_group_buy_backend.dto.RegisterDTO;
import com.example.community_group_buy_backend.service.AuthService;
import com.example.community_group_buy_backend.vo.LoginVO;
import com.example.community_group_buy_backend.vo.UserVO;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }
}
