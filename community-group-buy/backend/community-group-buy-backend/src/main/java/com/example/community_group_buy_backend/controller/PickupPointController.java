package com.example.community_group_buy_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.entity.PickupPoint;
import com.example.community_group_buy_backend.service.PickupPointService;

@RestController
@RequestMapping("/api")
public class PickupPointController {
    private final PickupPointService pickupPointService;

    public PickupPointController(PickupPointService pickupPointService) {
        this.pickupPointService = pickupPointService;
    }

    @GetMapping("/pickup-points")
    public Result<List<PickupPoint>> enabled() {
        return Result.success(pickupPointService.enabled());
    }

    @GetMapping("/leader/pickup-points")
    public Result<List<PickupPoint>> byLeader(@RequestParam Long leaderId) {
        return Result.success(pickupPointService.byLeader(leaderId));
    }

    @PutMapping({"/leader/pickup-point", "/admin/pickup-points/{pickupPointId}"})
    public Result<PickupPoint> save(@PathVariable(required = false) Long pickupPointId, @RequestBody PickupPoint point) {
        if (pickupPointId != null) {
            point.setPickupPointId(pickupPointId);
        }
        return Result.success(pickupPointService.save(point));
    }
}
