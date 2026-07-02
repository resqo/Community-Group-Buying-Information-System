package com.example.community_group_buy_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.service.AnalyticsService;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/merchant/analytics/sales")
    public Result<Map<String, Object>> merchantSales(@RequestParam Long merchantId) {
        return Result.success(analyticsService.merchantSales(merchantId));
    }

    @GetMapping("/merchant/analytics/suggestions")
    public Result<List<String>> merchantSuggestions(@RequestParam Long merchantId) {
        return Result.success(analyticsService.merchantSuggestions(merchantId));
    }

    @GetMapping("/admin/analytics/overview")
    public Result<Map<String, Object>> adminOverview() {
        return Result.success(analyticsService.adminOverview());
    }

    @GetMapping("/admin/analytics/suggestions")
    public Result<List<String>> adminSuggestions() {
        return Result.success(analyticsService.adminSuggestions());
    }
}
