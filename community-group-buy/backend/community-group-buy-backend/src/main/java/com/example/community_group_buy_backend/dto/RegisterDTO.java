package com.example.community_group_buy_backend.dto;

import lombok.Data;

@Data
public class RegisterDTO {
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
}
