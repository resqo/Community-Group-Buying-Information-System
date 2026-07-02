package com.example.community_group_buy_backend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.mapper.OrderMapper;
import com.example.community_group_buy_backend.mapper.ProductMapper;
import com.example.community_group_buy_backend.service.AnalyticsService;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final OllamaAgentService ollamaAgentService;

    public AnalyticsServiceImpl(OrderMapper orderMapper, ProductMapper productMapper,
                                OllamaAgentService ollamaAgentService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.ollamaAgentService = ollamaAgentService;
    }

    @Override
    public Map<String, Object> merchantSales(Long merchantId) {
        List<Map<String, Object>> salesStats = orderMapper.productSalesStats(merchantId);
        List<Map<String, Object>> inventoryStats = productMapper.productInventoryStats(merchantId);

        double totalRevenue = salesStats.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("total_amount", 0)).doubleValue())
                .sum();
        int totalQuantity = salesStats.stream()
                .mapToInt(r -> ((Number) r.getOrDefault("total_quantity", 0)).intValue())
                .sum();

        return Map.of(
                "salesStats", salesStats,
                "inventoryStats", inventoryStats,
                "totalRevenue", totalRevenue,
                "totalQuantity", totalQuantity);
    }

    @Override
    public List<String> merchantSuggestions(Long merchantId) {
        List<Map<String, Object>> salesStats = orderMapper.productSalesStats(merchantId);
        List<Map<String, Object>> inventoryStats = productMapper.productInventoryStats(merchantId);

        // Try Ollama first, fall back to rule engine
        try {
            List<String> ollamaSuggestions = ollamaAgentService.merchantSuggestions(salesStats, inventoryStats);
            if (ollamaSuggestions != null && !ollamaSuggestions.isEmpty()) {
                return ollamaSuggestions;
            }
        } catch (Exception ignored) {
            // Fall back to rule engine
        }

        return ruleBasedMerchantSuggestions(salesStats, inventoryStats);
    }

    @Override
    public Map<String, Object> adminOverview() {
        Map<String, Object> overview = orderMapper.revenueOverview();
        List<Map<String, Object>> salesStats = orderMapper.productSalesStats(null);
        List<Map<String, Object>> userStats = orderMapper.userPurchaseStats();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("overview", overview);
        result.put("salesStats", salesStats);
        result.put("userStats", userStats);
        return result;
    }

    @Override
    public List<String> adminSuggestions() {
        Map<String, Object> overview = orderMapper.revenueOverview();
        List<Map<String, Object>> salesStats = orderMapper.productSalesStats(null);
        List<Map<String, Object>> userStats = orderMapper.userPurchaseStats();

        try {
            List<String> ollamaSuggestions = ollamaAgentService.adminSuggestions(overview, salesStats, userStats);
            if (ollamaSuggestions != null && !ollamaSuggestions.isEmpty()) {
                return ollamaSuggestions;
            }
        } catch (Exception ignored) {
            // Fall back to rule engine
        }

        return ruleBasedAdminSuggestions(overview, salesStats, userStats);
    }

    // ---- Rule-based suggestion engines (fallback when Ollama is unavailable) ----

    private List<String> ruleBasedMerchantSuggestions(List<Map<String, Object>> salesStats,
                                                      List<Map<String, Object>> inventoryStats) {
        List<String> suggestions = new ArrayList<>();

        // Suggestion 1: Stock strategy based on best-seller
        if (!salesStats.isEmpty()) {
            Map<String, Object> top = salesStats.get(0);
            String name = (String) top.getOrDefault("product_name", "该商品");
            suggestions.add("「" + name + "」销量领先，建议优先补货并保持充足库存。");
        }

        // Suggestion 2: Check low-sales products for promotion
        if (inventoryStats.size() > 1) {
            Map<String, Object> last = inventoryStats.get(inventoryStats.size() - 1);
            String name = (String) last.getOrDefault("product_name", "滞销商品");
            int stock = ((Number) last.getOrDefault("stock", 0)).intValue();
            if (stock > 10) {
                suggestions.add("「" + name + "」库存积压（" + stock + "件），建议降价促销或搭配热销商品组合销售。");
            }
        }

        // Suggestion 3: Pricing strategy
        if (salesStats.size() >= 2) {
            Map<String, Object> second = salesStats.get(1);
            String name = (String) second.getOrDefault("product_name", "潜力商品");
            suggestions.add("「" + name + "」有一定销量基础，可考虑适当提高拼团价以增加利润空间。");
        }

        // Fallback
        if (suggestions.isEmpty()) {
            suggestions.add("关注热销商品库存，确保主力商品不断货。");
            suggestions.add("定期推出限时折扣活动，刺激用户下单。");
            suggestions.add("优化商品描述和图片，提升商品吸引力。");
        }

        return suggestions;
    }

    private List<String> ruleBasedAdminSuggestions(Map<String, Object> overview,
                                                    List<Map<String, Object>> salesStats,
                                                    List<Map<String, Object>> userStats) {
        List<String> suggestions = new ArrayList<>();

        // Suggestion 1: Based on active users
        Object userCountObj = overview.get("user_count");
        int userCount = userCountObj != null ? ((Number) userCountObj).intValue() : 0;
        if (userCount < 5) {
            suggestions.add("活跃付费用户偏少（" + userCount + "人），建议发布平台促销通知，吸引更多用户下单。");
        } else {
            suggestions.add("平台活跃用户" + userCount + "人，建议推出会员积分制度，提升用户复购率。");
        }

        // Suggestion 2: Based on top product
        if (!salesStats.isEmpty()) {
            Map<String, Object> top = salesStats.get(0);
            String name = (String) top.getOrDefault("product_name", "热销商品");
            suggestions.add("「" + name + "」为全平台销量冠军，可创建专属拼团活动进一步拉新。");
        }

        // Suggestion 3: Based on user purchase distribution
        if (!userStats.isEmpty()) {
            Map<String, Object> topUser = userStats.get(0);
            String username = (String) topUser.getOrDefault("username", "高价值用户");
            suggestions.add("「" + username + "」贡献最高交易额，建议定向发放优惠券以保持其活跃度。");
        }

        // Suggestion 4: General
        if (overview.get("order_count") != null) {
            int orderCount = ((Number) overview.get("order_count")).intValue();
            if (orderCount < 10) {
                suggestions.add("平台订单总量偏低，建议推出「首单立减」活动降低新用户购买门槛。");
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("定期举办平台级促销活动，如在节假日推出限时优惠。");
            suggestions.add("对长期未下单用户发送关怀通知，激活沉睡用户。");
            suggestions.add("优化团长管理体系，通过团长带动社区用户活跃度。");
        }

        return suggestions;
    }
}
