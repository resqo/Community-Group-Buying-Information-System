package com.example.community_group_buy_backend.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class User {
    private Long userId;
    private String username;
    private String password;
    private String phone;
    private String realName;
    private String role;
    private String communityName;
    private String address;
    private String avatarUrl;
    private String shopName;
    private String shopAddress;
    private Integer freeGroupCount;
    private Integer status;
    private LocalDateTime createTime;
}
