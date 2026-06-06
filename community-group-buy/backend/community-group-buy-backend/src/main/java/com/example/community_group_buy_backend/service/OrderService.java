package com.example.community_group_buy_backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.community_group_buy_backend.dto.OrderDTO;
import com.example.community_group_buy_backend.vo.OrderVO;

public interface OrderService {
    OrderVO create(OrderDTO dto);

    List<OrderVO> list(Long userId, Long merchantId, Long pickupPointId);

    OrderVO pay(Long orderId, String payMethod, String paymentPassword);

    OrderVO freeGroupOrder(Long orderId, Long userId);

    void deliver(Long orderId);

    void sendPickupNotice(Long orderId);

    void verify(String pickupCode);

    void applyRefund(Long orderId, Long userId, String reason, BigDecimal amount);

    List<Map<String, Object>> refunds();

    void handleRefund(Long refundId, Long adminId, Integer status);

    List<Map<String, Object>> notices(Long userId);

    void publishNotice(Long userId, String title, String content, String noticeType);

    List<Map<String, Object>> payments();
}
