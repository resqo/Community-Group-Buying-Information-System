package com.example.community_group_buy_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.entity.GroupBuy;
import com.example.community_group_buy_backend.service.GroupBuyService;

@RestController
@RequestMapping("/api")
public class GroupBuyController {
    private final GroupBuyService groupBuyService;

    public GroupBuyController(GroupBuyService groupBuyService) {
        this.groupBuyService = groupBuyService;
    }

    @GetMapping({"/group-activities", "/admin/group-activities"})
    public Result<List<Map<String, Object>>> activities() {
        return Result.success(groupBuyService.activities());
    }

    @PostMapping("/admin/group-activities")
    public Result<GroupBuy> createActivity(@RequestBody GroupBuy activity) {
        return Result.success(groupBuyService.saveActivity(activity));
    }

    @PutMapping("/admin/group-activities/{activityId}")
    public Result<GroupBuy> updateActivity(@PathVariable Long activityId, @RequestBody GroupBuy activity) {
        activity.setActivityId(activityId);
        return Result.success(groupBuyService.saveActivity(activity));
    }

    @PostMapping("/groups/start")
    public Result<GroupBuy> start(@RequestBody Map<String, Long> body) {
        return Result.success(groupBuyService.start(body.get("activityId"), body.get("userId")));
    }

    @PostMapping("/groups/join")
    public Result<GroupBuy> join(@RequestBody Map<String, Long> body) {
        return Result.success(groupBuyService.join(body.get("groupId"), body.get("userId")));
    }

    @GetMapping("/groups/open")
    public Result<List<Map<String, Object>>> openGroups(@RequestParam(required = false) Long userId) {
        return Result.success(groupBuyService.openGroups(userId));
    }

    @GetMapping("/groups/my")
    public Result<List<Map<String, Object>>> myGroups(@RequestParam Long userId) {
        return Result.success(groupBuyService.myGroups(userId));
    }
}
