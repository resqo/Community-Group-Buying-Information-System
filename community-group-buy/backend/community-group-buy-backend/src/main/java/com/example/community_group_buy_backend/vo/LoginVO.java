package com.example.community_group_buy_backend.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String avatarUrl;
    private String homePath;
}
