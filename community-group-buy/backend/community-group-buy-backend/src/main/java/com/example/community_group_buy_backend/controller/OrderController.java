package com.example.community_group_buy_backend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.dto.OrderDTO;
import com.example.community_group_buy_backend.service.OrderService;
import com.example.community_group_buy_backend.vo.OrderVO;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public Result<OrderVO> create(@RequestBody OrderDTO dto) {
        return Result.success(orderService.create(dto));
    }

    @GetMapping({"/orders", "/admin/orders"})
    public Result<List<OrderVO>> list(@RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) Long merchantId,
                                      @RequestParam(required = false) Long pickupPointId) {
        return Result.success(orderService.list(userId, merchantId, pickupPointId));
    }

    @GetMapping("/orders/my")
    public Result<List<OrderVO>> my(@RequestParam Long userId) {
        return Result.success(orderService.list(userId, null, null));
    }

    @GetMapping("/merchant/orders")
    public Result<List<OrderVO>> merchantOrders(@RequestParam Long merchantId) {
        return Result.success(orderService.list(null, merchantId, null));
    }

    @GetMapping("/leader/orders/wait-pickup")
    public Result<List<OrderVO>> waitPickup(@RequestParam Long pickupPointId) {
        return Result.success(orderService.list(null, null, pickupPointId));
    }

    @PostMapping("/orders/{orderId}/pay")
    public Result<OrderVO> pay(@PathVariable Long orderId, @RequestBody(required = false) Map<String, String> body) {
        String payMethod = body == null ? "模拟支付" : body.getOrDefault("payMethod", "模拟支付");
        String paymentPassword = body == null ? null : body.get("paymentPassword");
        return Result.success(orderService.pay(orderId, payMethod, paymentPassword));
    }
    @PostMapping("/orders/{orderId}/free-group")
    public Result<OrderVO> freeGroup(@PathVariable Long orderId, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        return Result.success(orderService.freeGroupOrder(orderId, userId));
    }
    @PostMapping("/merchant/orders/{orderId}/deliver")
    public Result<Void> deliver(@PathVariable Long orderId) {
        orderService.deliver(orderId);
        return Result.success(null);
    }

    @PostMapping("/leader/orders/{orderId}/send-code")
    public Result<Void> sendPickupNotice(@PathVariable Long orderId) {
        orderService.sendPickupNotice(orderId);
        return Result.success(null);
    }

    @PostMapping("/leader/orders/verify")
    public Result<Void> verify(@RequestBody Map<String, String> body) {
        orderService.verify(body.get("pickupCode"));
        return Result.success(null);
    }

    @PostMapping("/refunds")
    public Result<Void> applyRefund(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        String reason = String.valueOf(body.getOrDefault("reason", "用户申请退款"));
        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", "0")));
        orderService.applyRefund(orderId, userId, reason, amount);
        return Result.success(null);
    }

    @GetMapping("/admin/refunds")
    public Result<List<Map<String, Object>>> refunds() {
        return Result.success(orderService.refunds());
    }

    @PostMapping("/admin/refunds/{refundId}/handle")
    public Result<Void> handleRefund(@PathVariable Long refundId, @RequestBody Map<String, Object> body) {
        Long adminId = Long.valueOf(String.valueOf(body.get("adminId")));
        Integer status = Integer.valueOf(String.valueOf(body.getOrDefault("status", "1")));
        orderService.handleRefund(refundId, adminId, status);
        return Result.success(null);
    }

    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(@RequestParam Long userId) {
        return Result.success(orderService.notices(userId));
    }

    @PostMapping("/admin/notices")
    public Result<Void> publishNotice(@RequestBody Map<String, Object> body) {
        Object userIdValue = body.get("userId");
        Long userId = userIdValue == null || String.valueOf(userIdValue).isBlank() ? null : Long.valueOf(String.valueOf(userIdValue));
        String title = String.valueOf(body.getOrDefault("title", "系统公告"));
        String content = String.valueOf(body.getOrDefault("content", ""));
        String noticeType = String.valueOf(body.getOrDefault("noticeType", "SYSTEM"));
        orderService.publishNotice(userId, title, content, noticeType);
        return Result.success(null);
    }

    @GetMapping("/admin/payments")
    public Result<List<Map<String, Object>>> payments() {
        return Result.success(orderService.payments());
    }
}
