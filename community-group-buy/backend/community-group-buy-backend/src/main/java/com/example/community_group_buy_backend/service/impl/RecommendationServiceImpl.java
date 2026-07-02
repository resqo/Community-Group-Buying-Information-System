package com.example.community_group_buy_backend.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.community_group_buy_backend.mapper.OrderMapper;
import com.example.community_group_buy_backend.mapper.ProductMapper;
import com.example.community_group_buy_backend.service.RecommendationService;
import com.example.community_group_buy_backend.vo.ProductVO;
import com.example.community_group_buy_backend.vo.RecommendationVO;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;

    public RecommendationServiceImpl(ProductMapper productMapper, OrderMapper orderMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationVO> recommend(Long userId) {
        List<ProductVO> allProducts = productMapper.findProducts(null, null);

        Map<Long, ProductVO> productMap = new HashMap<>();
        for (ProductVO p : allProducts) {
            productMap.put(p.getProductId(), p);
        }

        // Directly query user's purchased product IDs from orders (any non-cancelled)
        List<Long> userPurchasedIds = orderMapper.findUserPurchasedProductIds(userId);
        Set<Long> purchasedProductIds = new HashSet<>(userPurchasedIds);

        // Cold start: no purchase history
        if (purchasedProductIds.isEmpty()) {
            return buildResult(productMapper.findPopularProducts(3), "热门商品");
        }

        // Load all purchase records for co-purchase analysis
        List<Map<String, Object>> allPurchases = orderMapper.findAllPurchases();

        // Build productId → userIds mapping
        Map<Long, Set<Long>> productUsers = new HashMap<>();
        for (Map<String, Object> row : allPurchases) {
            Long uid = ((Number) row.get("user_id")).longValue();
            Long pid = ((Number) row.get("product_id")).longValue();
            productUsers.computeIfAbsent(pid, k -> new HashSet<>()).add(uid);
        }

        // Filter candidates
        List<ProductVO> candidates = allProducts.stream()
                .filter(p -> p.getAuditStatus() != null && p.getAuditStatus() == 1)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .filter(p -> p.getStock() != null && p.getStock() > 0)
                .filter(p -> !purchasedProductIds.contains(p.getProductId()))
                .collect(Collectors.toList());

        // Hybrid scoring: Jaccard co-purchase similarity + category-based similarity
        List<RecommendationVO> scored = new ArrayList<>();

        for (ProductVO candidate : candidates) {
            Set<Long> candidateUsers = productUsers.getOrDefault(candidate.getProductId(), Collections.emptySet());
            double totalScore = 0.0;
            double bestSim = -1.0;
            String bestMatchName = null;

            for (Long purchasedId : purchasedProductIds) {
                Set<Long> purchasedUsers = productUsers.getOrDefault(purchasedId, Collections.emptySet());

                // Jaccard similarity
                Set<Long> intersection = new HashSet<>(candidateUsers);
                intersection.retainAll(purchasedUsers);

                Set<Long> union = new HashSet<>(candidateUsers);
                union.addAll(purchasedUsers);

                double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

                // Category bonus: same category as a purchased product
                ProductVO purchased = productMap.get(purchasedId);
                double categoryBonus = 0.0;
                if (purchased != null && candidate.getCategoryId() != null
                        && candidate.getCategoryId().equals(purchased.getCategoryId())) {
                    categoryBonus = 0.3;
                }

                double sim = jaccard + categoryBonus;
                totalScore += sim;

                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatchName = purchased != null ? purchased.getProductName() : null;
                }
            }

            if (totalScore > 0 && bestMatchName != null) {
                RecommendationVO vo = toVO(candidate, totalScore, bestMatchName);
                scored.add(vo);
            }
        }

        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Take top 3
        List<RecommendationVO> result = new ArrayList<>(scored.subList(0, Math.min(3, scored.size())));

        // Pad with popular products if fewer than 3
        if (result.size() < 3) {
            Set<Long> existing = result.stream().map(RecommendationVO::getProductId).collect(Collectors.toSet());
            List<ProductVO> popular = productMapper.findPopularProducts(10);
            for (ProductVO p : popular) {
                if (result.size() >= 3) break;
                if (existing.contains(p.getProductId())) continue;
                if (purchasedProductIds.contains(p.getProductId())) continue;
                result.add(toVO(p, 0.0, "热门商品"));
            }
        }

        return result;
    }

    private List<RecommendationVO> buildResult(List<ProductVO> products, String basedOn) {
        List<RecommendationVO> result = new ArrayList<>();
        for (ProductVO p : products) {
            result.add(toVO(p, 0.0, basedOn));
        }
        return result;
    }

    private RecommendationVO toVO(ProductVO product, double score, String basedOn) {
        RecommendationVO vo = new RecommendationVO();
        BeanUtils.copyProperties(product, vo);
        vo.setScore(Math.round(score * 10000.0) / 10000.0);
        vo.setBasedOnProductName(basedOn);
        return vo;
    }
}
