package com.example.community_group_buy_backend.entity;

import lombok.Data;

@Data
public class PickupPoint {
    private Long pickupPointId;
    private Long leaderId;
    private String pointName;
    private String communityName;
    private String address;
    private String phone;
    private String businessHours;
    private Integer status;
}
