package com.example.community_group_buy_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.entity.PickupPoint;
import com.example.community_group_buy_backend.mapper.PickupPointMapper;
import com.example.community_group_buy_backend.service.PickupPointService;

@Service
public class PickupPointServiceImpl implements PickupPointService {
    private final PickupPointMapper mapper;

    public PickupPointServiceImpl(PickupPointMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PickupPoint> enabled() {
        return mapper.findEnabled();
    }

    @Override
    public List<PickupPoint> byLeader(Long leaderId) {
        return mapper.findByLeader(leaderId);
    }

    @Override
    public PickupPoint save(PickupPoint pickupPoint) {
        if (pickupPoint.getStatus() == null) {
            pickupPoint.setStatus(1);
        }
        if (pickupPoint.getPickupPointId() == null) {
            mapper.insert(pickupPoint);
        } else {
            mapper.update(pickupPoint);
        }
        return mapper.findById(pickupPoint.getPickupPointId());
    }
}
