package com.example.community_group_buy_backend.service;

import java.util.List;
import java.util.Map;

import com.example.community_group_buy_backend.entity.GroupBuy;

public interface GroupBuyService {
    List<Map<String, Object>> activities();

    GroupBuy saveActivity(GroupBuy activity);

    GroupBuy start(Long activityId, Long userId);

    GroupBuy join(Long groupId, Long userId);

    List<Map<String, Object>> openGroups(Long userId);

    List<Map<String, Object>> myGroups(Long userId);
}
