package com.example.community_group_buy_backend.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderVO {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String username;
    private String userAvatar;
    private Long merchantId;
    private String merchantName;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Long groupId;
    private String orderType;
    private Long pickupPointId;
    private String pointName;
    private String pickupCode;
    private Integer pickupStatus;
    private Integer orderStatus;
    private Integer payStatus;
    private Integer deliveryStatus;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
}
