package com.example.community_group_buy_backend.service;

import java.util.List;

import com.example.community_group_buy_backend.entity.PickupPoint;

public interface PickupPointService {
    List<PickupPoint> enabled();

    List<PickupPoint> byLeader(Long leaderId);

    PickupPoint save(PickupPoint pickupPoint);
}
