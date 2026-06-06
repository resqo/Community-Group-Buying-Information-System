package com.example.community_group_buy_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Product {
    private Long productId;
    private Long merchantId;
    private Long categoryId;
    private String productName;
    private String description;
    private String mainImage;
    private String detailImages;
    private BigDecimal originalPrice;
    private BigDecimal groupPrice;
    private BigDecimal singlePrice;
    private Integer stock;
    private Integer salesCount;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createTime;
}
