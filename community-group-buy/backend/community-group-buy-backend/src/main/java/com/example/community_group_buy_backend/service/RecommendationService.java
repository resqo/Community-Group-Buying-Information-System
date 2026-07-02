package com.example.community_group_buy_backend.service;

import java.util.List;

import com.example.community_group_buy_backend.vo.RecommendationVO;

public interface RecommendationService {
    List<RecommendationVO> recommend(Long userId);
}
