package com.example.community_group_buy_backend.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductDTO {
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
    private Integer status;
}
