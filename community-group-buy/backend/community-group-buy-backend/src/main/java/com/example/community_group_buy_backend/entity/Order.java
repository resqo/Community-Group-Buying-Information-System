package com.example.community_group_buy_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Order {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Long groupId;
    private String orderType;
    private Long pickupPointId;
    private String pickupCode;
    private Integer pickupStatus;
    private Integer orderStatus;
    private Integer payStatus;
    private Integer deliveryStatus;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
}
