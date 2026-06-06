package com.example.community_group_buy_backend.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.dto.LoginDTO;
import com.example.community_group_buy_backend.dto.RegisterDTO;
import com.example.community_group_buy_backend.entity.User;
import com.example.community_group_buy_backend.mapper.UserMapper;
import com.example.community_group_buy_backend.service.AuthService;
import com.example.community_group_buy_backend.utils.JwtUtil;
import com.example.community_group_buy_backend.vo.LoginVO;
import com.example.community_group_buy_backend.vo.UserVO;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        if (dto.getUsername() == null || dto.getPassword() == null || dto.getRole() == null) {
            throw new IllegalArgumentException("用户名、密码和角色不能为空");
        }
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !dto.getPassword().equals(user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!dto.getRole().equals(user.getRole())) {
            throw new IllegalArgumentException("登录角色与账号角色不一致");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setHomePath(homePath(user.getRole()));
        vo.setToken(JwtUtil.createToken(user.getUserId(), user.getUsername(), user.getRole()));
        return vo;
    }

    @Override
    public UserVO register(RegisterDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        userMapper.insert(user);
        return toVO(user);
    }

    private String homePath(String role) {
        return switch (role) {
            case "LEADER" -> "/leader/dashboard";
            case "MERCHANT" -> "/merchant/products";
            case "ADMIN" -> "/admin/dashboard";
            default -> "/user/home";
        };
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
