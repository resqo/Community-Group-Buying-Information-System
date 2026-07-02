package com.example.community_group_buy_backend.service;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    Map<String, Object> merchantSales(Long merchantId);
    List<String> merchantSuggestions(Long merchantId);
    Map<String, Object> adminOverview();
    List<String> adminSuggestions();
}
