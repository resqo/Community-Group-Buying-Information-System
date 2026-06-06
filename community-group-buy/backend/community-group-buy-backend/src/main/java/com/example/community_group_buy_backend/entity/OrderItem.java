package com.example.community_group_buy_backend.entity;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrderItem {
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
}
