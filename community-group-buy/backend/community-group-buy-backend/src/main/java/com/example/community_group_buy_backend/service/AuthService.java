package com.example.community_group_buy_backend.service;

import com.example.community_group_buy_backend.dto.LoginDTO;
import com.example.community_group_buy_backend.dto.RegisterDTO;
import com.example.community_group_buy_backend.vo.LoginVO;
import com.example.community_group_buy_backend.vo.UserVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);

    UserVO register(RegisterDTO dto);
}
