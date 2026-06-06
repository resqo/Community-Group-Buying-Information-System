package com.example.community_group_buy_backend.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("backend", "UP");
        data.put("time", LocalDateTime.now());
        Integer databaseOk = jdbcTemplate.queryForObject("select 1", Integer.class);
        data.put("database", databaseOk != null && databaseOk == 1 ? "UP" : "DOWN");
        data.put("databaseName", "isdemo");
        return Result.success("服务正常", data);
    }
}
