package com.example.community_group_buy_backend.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserVO {
    private Long userId;
    private String username;
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
