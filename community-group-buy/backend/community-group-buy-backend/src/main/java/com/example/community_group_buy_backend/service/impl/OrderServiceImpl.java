package com.example.community_group_buy_backend.service.impl;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.community_group_buy_backend.dto.OrderDTO;
import com.example.community_group_buy_backend.entity.GroupBuy;
import com.example.community_group_buy_backend.entity.Order;
import com.example.community_group_buy_backend.entity.Product;
import com.example.community_group_buy_backend.mapper.GroupBuyMapper;
import com.example.community_group_buy_backend.mapper.OrderMapper;
import com.example.community_group_buy_backend.mapper.ProductMapper;
import com.example.community_group_buy_backend.mapper.UserMapper;
import com.example.community_group_buy_backend.service.OrderService;
import com.example.community_group_buy_backend.vo.OrderVO;

@Service
public class OrderServiceImpl implements OrderService {
    private static final String DEFAULT_PAYMENT_PASSWORD = "123456";

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final GroupBuyMapper groupBuyMapper;
    private final UserMapper userMapper;
    private final Random random = new Random();

    public OrderServiceImpl(OrderMapper orderMapper, ProductMapper productMapper, GroupBuyMapper groupBuyMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.groupBuyMapper = groupBuyMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public OrderVO create(OrderDTO dto) {
        Product product = productMapper.findById(dto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        int quantity = dto.getQuantity() == null || dto.getQuantity() <= 0 ? 1 : dto.getQuantity();
        BigDecimal price = product.getSinglePrice();
        String orderType = dto.getOrderType() == null ? "SINGLE" : dto.getOrderType();
        GroupBuy group = null;
        if ("GROUP".equals(orderType) && dto.getGroupId() != null) {
            group = groupBuyMapper.findInstanceById(dto.getGroupId());
            if (group == null) {
                throw new IllegalArgumentException("拼团不存在");
            }
            if (group.getStatus() != null && group.getStatus() != 0) {
                throw new IllegalArgumentException("拼团已结束");
            }
            if (group.getCurrentCount() != null
                    && group.getRequiredCount() != null
                    && group.getCurrentCount() >= group.getRequiredCount()) {
                throw new IllegalArgumentException("拼团人数已满");
            }
            GroupBuy activity = groupBuyMapper.findActivityById(group.getActivityId());
            price = activity.getGroupPrice();
        } else if ("GROUP".equals(orderType)) {
            price = product.getGroupPrice();
        } else if ("FREE_GROUP".equals(orderType)) {
            GroupBuy activity = groupBuyMapper.findActiveActivityByProductId(product.getProductId());
            if (activity == null || activity.getAllowFreeGroup() == null || activity.getAllowFreeGroup() != 1) {
                throw new IllegalArgumentException("该商品暂不支持免拼");
            }
            price = activity.getGroupPrice() != null && activity.getGroupPrice().compareTo(BigDecimal.ZERO) > 0
                    ? activity.getGroupPrice()
                    : product.getGroupPrice();
        }
        if ("FREE_GROUP".equals(orderType) && userMapper.decreaseFreeGroupCount(dto.getUserId()) == 0) {
            throw new IllegalArgumentException("免拼次数不足");
        }
        Order order = new Order();
        order.setOrderNo("GB" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (1000 + random.nextInt(9000)));
        order.setUserId(dto.getUserId());
        order.setMerchantId(product.getMerchantId());
        order.setProductId(product.getProductId());
        order.setProductName(product.getProductName());
        order.setProductPrice(price);
        order.setQuantity(quantity);
        order.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
        order.setGroupId(dto.getGroupId());
        order.setOrderType(orderType);
        order.setPickupPointId(dto.getPickupPointId());
        order.setOrderStatus("GROUP".equals(orderType) ? 2 : 0);
        orderMapper.insert(order);
        if ("GROUP".equals(orderType) && dto.getGroupId() != null) {
            boolean isLeader = group != null && group.getLeaderUserId() != null && group.getLeaderUserId().equals(dto.getUserId());
            if (!isLeader) {
                if (groupBuyMapper.increaseCount(dto.getGroupId()) == 0) {
                    throw new IllegalArgumentException("拼团人数已满");
                }
            }
            groupBuyMapper.markSuccessIfFull(dto.getGroupId());
            notifyGroupIfFull(dto.getGroupId());
        }
        return list(dto.getUserId(), null, null).stream()
                .filter(item -> item.getOrderId().equals(order.getOrderId()))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public List<OrderVO> list(Long userId, Long merchantId, Long pickupPointId) {
        return orderMapper.findOrders(userId, merchantId, pickupPointId);
    }

    @Override
    @Transactional
    public OrderVO pay(Long orderId, String payMethod, String paymentPassword) {
        if (paymentPassword == null || paymentPassword.isBlank()) {
            throw new IllegalArgumentException("请输入支付密码");
        }
        if (!DEFAULT_PAYMENT_PASSWORD.equals(paymentPassword)) {
            throw new IllegalArgumentException("支付密码错误");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            throw new IllegalArgumentException("订单已支付，请勿重复支付");
        }
        if (productMapper.decreaseStock(order.getProductId(), order.getQuantity()) == 0) {
            throw new IllegalArgumentException("库存不足");
        }
        int nextStatus = "GROUP".equals(order.getOrderType()) ? 2 : 3;
        orderMapper.pay(orderId, nextStatus);
        orderMapper.insertPayment(orderId, "PAY" + System.currentTimeMillis(), payMethod == null ? "模拟支付" : payMethod, order.getTotalAmount());
        orderMapper.insertNotice(order.getUserId(), "支付成功", "订单 " + order.getOrderNo() + " 已支付成功", "ORDER");
        if ("GROUP".equals(order.getOrderType()) && order.getGroupId() != null && orderMapper.countUnpaidInGroup(order.getGroupId()) == 0) {
            orderMapper.markGroupReadyToShip(order.getGroupId());
            for (Order groupOrder : orderMapper.findByGroupId(order.getGroupId())) {
                orderMapper.insertNotice(groupOrder.getUserId(), "拼团支付完成", "拼团订单 " + groupOrder.getOrderNo() + " 已进入待发货", "GROUP");
            }
        }
        return orderMapper.findOrders(order.getUserId(), null, null).stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    @Transactional
    public OrderVO freeGroupOrder(Long orderId, Long userId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"GROUP".equals(order.getOrderType())) {
            throw new IllegalArgumentException("只有拼团订单可以转为免拼");
        }
        if (order.getOrderStatus() == null || (order.getOrderStatus() != 0 && order.getOrderStatus() != 2)) {
            throw new IllegalArgumentException("当前订单状态不能免拼");
        }
        if (userMapper.decreaseFreeGroupCount(userId) == 0) {
            throw new IllegalArgumentException("免拼次数不足");
        }
        Long groupId = order.getGroupId();
        if (orderMapper.convertGroupOrderToFreeGroup(orderId, userId) == 0) {
            throw new IllegalArgumentException("免拼失败，请刷新后重试");
        }
        if (groupId != null) {
            groupBuyMapper.decreaseCount(groupId);
        }
        orderMapper.insertNotice(userId, "免拼成功", "订单 " + order.getOrderNo() + " 已转为免拼，商家可继续处理发货", "GROUP");
        return orderMapper.findOrders(userId, null, null).stream()
                .filter(item -> item.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public void deliver(Long orderId) {
        orderMapper.deliver(orderId);
        Order order = orderMapper.findById(orderId);
        if (order != null) {
            orderMapper.insertNotice(order.getUserId(), "订单运输中", "订单 " + order.getOrderNo() + " 已由商家发货，正在配送至自提点", "ORDER");
        }
    }

    @Override
    public void sendPickupNotice(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        String pickupCode = order.getPickupCode();
        if (pickupCode == null || pickupCode.isBlank()) {
            pickupCode = String.valueOf(100000 + random.nextInt(900000));
            orderMapper.markArrived(orderId, pickupCode);
        }
        orderMapper.insertNotice(order.getUserId(), "取件通知", "订单 " + order.getOrderNo() + " 已到达自提点，取件码：" + pickupCode, "PICKUP");
    }

    @Override
    public void verify(String pickupCode) {
        if (orderMapper.verify(pickupCode) == 0) {
            throw new IllegalArgumentException("取件码无效或订单不可核销");
        }
    }

    @Override
    public void applyRefund(Long orderId, Long userId, String reason, BigDecimal amount) {
        orderMapper.applyRefund(orderId, userId, reason, amount);
        orderMapper.updateStatus(orderId, 8);
    }

    @Override
    public List<Map<String, Object>> refunds() {
        return orderMapper.findRefunds();
    }

    @Override
    public void handleRefund(Long refundId, Long adminId, Integer status) {
        Map<String, Object> refund = orderMapper.findRefundById(refundId);
        if (refund == null) {
            throw new IllegalArgumentException("退款申请不存在");
        }
        Long orderId = getLong(refund, "order_id", "orderId");
        Long userId = getLong(refund, "user_id", "userId");
        if (status != null && status == 1) {
            orderMapper.handleRefund(refundId, adminId, 3);
            orderMapper.updateStatus(orderId, 9);
            orderMapper.updatePayStatus(orderId, 3);
            orderMapper.insertNotice(userId, "退款通过", "您的订单退款申请已通过并处理完成", "REFUND");
        } else {
            orderMapper.handleRefund(refundId, adminId, 2);
            orderMapper.updateStatus(orderId, 1);
            orderMapper.insertNotice(userId, "退款拒绝", "您的订单退款申请未通过审核", "REFUND");
        }
    }

    @Override
    public List<Map<String, Object>> notices(Long userId) {
        return orderMapper.findNotices(userId);
    }

    @Override
    public void publishNotice(Long userId, String title, String content, String noticeType) {
        orderMapper.insertNotice(userId, title, content, noticeType == null ? "SYSTEM" : noticeType);
    }

    @Override
    public List<Map<String, Object>> payments() {
        return orderMapper.findPayments();
    }

    private Long getLong(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return Long.valueOf(String.valueOf(value));
            }
        }
        return null;
    }

    private void notifyGroupIfFull(Long groupId) {
        GroupBuy group = groupBuyMapper.findInstanceById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            return;
        }
        orderMapper.activateGroupPayment(groupId);
        for (Order groupOrder : orderMapper.findByGroupId(groupId)) {
            if (groupOrder.getPayStatus() == null || groupOrder.getPayStatus() == 0) {
                orderMapper.insertNotice(groupOrder.getUserId(), "拼团已成团", "拼团订单 " + groupOrder.getOrderNo() + " 已成团，请尽快完成支付", "GROUP");
            }
        }
    }
}
