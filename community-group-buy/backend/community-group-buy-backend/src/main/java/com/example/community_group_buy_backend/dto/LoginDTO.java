package com.example.community_group_buy_backend.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private String role;
}
