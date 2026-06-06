package com.example.community_group_buy_backend.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Long groupId;
    private String orderType;
    private Long pickupPointId;
}
