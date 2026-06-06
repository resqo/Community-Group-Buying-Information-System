package com.example.community_group_buy_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GroupBuy {
    private Long activityId;
    private Long productId;
    private BigDecimal groupPrice;
    private Integer groupSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allowFreeGroup;
    private Integer freeGroupLimit;
    private Integer status;
    private LocalDateTime createTime;

    private Long groupId;
    private Long leaderUserId;
    private Integer currentCount;
    private Integer requiredCount;
    private LocalDateTime expireTime;
}
