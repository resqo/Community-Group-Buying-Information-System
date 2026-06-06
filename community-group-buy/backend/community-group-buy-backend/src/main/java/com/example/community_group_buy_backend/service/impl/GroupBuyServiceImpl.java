package com.example.community_group_buy_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.entity.GroupBuy;
import com.example.community_group_buy_backend.mapper.GroupBuyMapper;
import com.example.community_group_buy_backend.service.GroupBuyService;

@Service
public class GroupBuyServiceImpl implements GroupBuyService {
    private final GroupBuyMapper mapper;

    public GroupBuyServiceImpl(GroupBuyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Map<String, Object>> activities() {
        return mapper.findActivities();
    }

    @Override
    public GroupBuy saveActivity(GroupBuy activity) {
        if (activity.getGroupSize() == null || activity.getGroupSize() <= 0) {
            throw new IllegalArgumentException("拼团人数必须大于0");
        }
        if (activity.getStatus() == null) {
            activity.setStatus(1);
        }
        if (activity.getAllowFreeGroup() == null) {
            activity.setAllowFreeGroup(0);
        }
        if (activity.getFreeGroupLimit() == null) {
            activity.setFreeGroupLimit(0);
        }
        if (activity.getActivityId() == null) {
            mapper.insertActivity(activity);
        } else {
            mapper.updateActivity(activity);
        }
        return mapper.findActivityById(activity.getActivityId());
    }

    @Override
    public GroupBuy start(Long activityId, Long userId) {
        GroupBuy activity = mapper.findActivityById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("拼团活动不存在");
        }
        if (activity.getGroupSize() == null || activity.getGroupSize() <= 0) {
            throw new IllegalArgumentException("拼团人数必须大于0");
        }
        GroupBuy group = new GroupBuy();
        group.setActivityId(activityId);
        group.setLeaderUserId(userId);
        group.setCurrentCount(0);
        group.setRequiredCount(activity.getGroupSize());
        group.setStatus(0);
        LocalDateTime expireTime = activity.getEndTime();
        if (expireTime == null || expireTime.isAfter(LocalDateTime.now().plusHours(24))) {
            expireTime = LocalDateTime.now().plusHours(24);
        }
        group.setExpireTime(expireTime);
        mapper.insertInstance(group);
        return mapper.findInstanceById(group.getGroupId());
    }

    @Override
    public GroupBuy join(Long groupId, Long userId) {
        GroupBuy group = mapper.findInstanceById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("拼团不存在");
        }
        if (group.getLeaderUserId() != null && group.getLeaderUserId().equals(userId)) {
            throw new IllegalArgumentException("不能参与自己发起的拼团");
        }
        if (group.getStatus() != null && group.getStatus() != 0) {
            throw new IllegalArgumentException("拼团已结束");
        }
        if (group.getExpireTime() != null && group.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("拼团已过期");
        }
        mapper.increaseCount(groupId);
        mapper.markSuccessIfFull(groupId);
        return mapper.findInstanceById(groupId);
    }

    @Override
    public List<Map<String, Object>> openGroups(Long userId) {
        mapper.cancelEmptyGroups();
        return mapper.findOpenGroups(userId);
    }

    @Override
    public List<Map<String, Object>> myGroups(Long userId) {
        return mapper.findMyGroups(userId);
    }
}
