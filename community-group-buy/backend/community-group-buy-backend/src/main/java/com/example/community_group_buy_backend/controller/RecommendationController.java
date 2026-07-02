package com.example.community_group_buy_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.service.RecommendationService;
import com.example.community_group_buy_backend.vo.RecommendationVO;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public Result<List<RecommendationVO>> recommend(@RequestParam Long userId) {
        return Result.success(recommendationService.recommend(userId));
    }
}
