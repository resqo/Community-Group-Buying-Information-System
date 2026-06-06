package com.example.community_group_buy_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.entity.User;
import com.example.community_group_buy_backend.service.UserService;
import com.example.community_group_buy_backend.vo.UserVO;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/admin/users", "/users"})
    public Result<List<UserVO>> list() {
        return Result.success(userService.list());
    }

    @GetMapping("/users/{userId}")
    public Result<UserVO> get(@PathVariable Long userId) {
        return Result.success(userService.get(userId));
    }

    @PutMapping("/users/{userId}")
    public Result<UserVO> update(@PathVariable Long userId, @RequestBody User user) {
        user.setUserId(userId);
        return Result.success(userService.update(user));
    }

    @PutMapping("/admin/users/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(userId, body.getOrDefault("status", 1));
        return Result.success(null);
    }
}
