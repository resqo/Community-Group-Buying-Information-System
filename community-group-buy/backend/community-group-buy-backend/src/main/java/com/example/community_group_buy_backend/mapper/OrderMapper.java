package com.example.community_group_buy_backend.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.community_group_buy_backend.entity.Order;
import com.example.community_group_buy_backend.vo.OrderVO;

public interface OrderMapper {
    @Insert("""
            insert into orders(order_no,user_id,merchant_id,product_id,product_name,product_price,quantity,total_amount,
            group_id,order_type,pickup_point_id,pickup_code,pickup_status,order_status,pay_status,delivery_status,create_time)
            values(#{orderNo},#{userId},#{merchantId},#{productId},#{productName},#{productPrice},#{quantity},#{totalAmount},
            #{groupId},#{orderType},#{pickupPointId},null,0,#{orderStatus},0,0,now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    int insert(Order order);

    @Select("select * from orders where order_id = #{orderId}")
    Order findById(Long orderId);

    @Select("""
            select o.*, u.username, u.avatar_url user_avatar, mu.shop_name merchant_name, pp.point_name
            from orders o
            left join user u on o.user_id = u.user_id
            left join user mu on o.merchant_id = mu.user_id
            left join pickup_point pp on o.pickup_point_id = pp.pickup_point_id
            where (#{userId} is null or o.user_id = #{userId})
              and (#{merchantId} is null or o.merchant_id = #{merchantId})
              and (#{pickupPointId} is null or o.pickup_point_id = #{pickupPointId})
            order by o.create_time desc
            """)
    List<OrderVO> findOrders(@Param("userId") Long userId, @Param("merchantId") Long merchantId, @Param("pickupPointId") Long pickupPointId);

    @Update("update orders set pay_status=1, order_status=#{orderStatus}, pay_time=now() where order_id=#{orderId}")
    int pay(@Param("orderId") Long orderId, @Param("orderStatus") Integer orderStatus);

    @Update("update orders set delivery_status=1, order_status=4 where order_id=#{orderId} and pay_status=1")
    int deliver(Long orderId);

    @Update("update orders set pickup_code=#{pickupCode}, pickup_status=1, order_status=5 where order_id=#{orderId} and delivery_status=1")
    int markArrived(@Param("orderId") Long orderId, @Param("pickupCode") String pickupCode);

    @Update("update orders set pickup_status=2, order_status=6, finish_time=now() where pickup_code=#{pickupCode} and pickup_status=1")
    int verify(String pickupCode);

    @Update("update orders set order_status=#{status} where order_id=#{orderId}")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") Integer status);

    @Update("update orders set pay_status=#{payStatus} where order_id=#{orderId}")
    int updatePayStatus(@Param("orderId") Long orderId, @Param("payStatus") Integer payStatus);

    @Update("""
            update orders
            set order_type = 'FREE_GROUP',
                group_id = null,
                order_status = case when pay_status = 1 then 3 else 0 end
            where order_id = #{orderId}
              and user_id = #{userId}
              and order_type = 'GROUP'
              and order_status in (0, 2)
            """)
    int convertGroupOrderToFreeGroup(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Select("select * from orders where group_id = #{groupId} order by create_time")
    List<Order> findByGroupId(Long groupId);

    @Update("update orders set order_status = 0 where group_id = #{groupId} and pay_status = 0 and order_status = 2")
    int activateGroupPayment(Long groupId);

    @Update("update orders set order_status = 3 where group_id = #{groupId} and pay_status = 1 and order_status in (0,2)")
    int markGroupReadyToShip(Long groupId);

    @Select("select count(*) from orders where group_id = #{groupId} and pay_status <> 1 and order_status <> 7")
    int countUnpaidInGroup(Long groupId);

    @Insert("""
            insert into payment(order_id,pay_no,pay_method,pay_amount,pay_status,pay_time)
            values(#{orderId},#{payNo},#{payMethod},#{payAmount},1,now())
            """)
    int insertPayment(@Param("orderId") Long orderId, @Param("payNo") String payNo, @Param("payMethod") String payMethod, @Param("payAmount") BigDecimal payAmount);

    @Insert("""
            insert into notice(user_id,title,content,notice_type,read_status,create_time)
            values(#{userId},#{title},#{content},#{noticeType},0,now())
            """)
    int insertNotice(@Param("userId") Long userId, @Param("title") String title, @Param("content") String content, @Param("noticeType") String noticeType);

    @Insert("""
            insert into refund(order_id,user_id,refund_reason,refund_amount,refund_status,apply_time)
            values(#{orderId},#{userId},#{reason},#{amount},0,now())
            """)
    int applyRefund(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("reason") String reason, @Param("amount") BigDecimal amount);

    @Select("select * from refund order by apply_time desc")
    List<Map<String, Object>> findRefunds();

    @Select("select * from refund where refund_id = #{refundId}")
    Map<String, Object> findRefundById(Long refundId);

    @Update("update refund set refund_status=#{status}, admin_id=#{adminId}, handle_time=now() where refund_id=#{refundId}")
    int handleRefund(@Param("refundId") Long refundId, @Param("adminId") Long adminId, @Param("status") Integer status);

    @Select("select * from notice where user_id = #{userId} or user_id is null order by create_time desc")
    List<Map<String, Object>> findNotices(Long userId);

    @Select("""
            select p.*, o.order_no, o.product_name, u.username
            from payment p
            left join orders o on p.order_id = o.order_id
            left join user u on o.user_id = u.user_id
            order by p.pay_time desc
            """)
    List<Map<String, Object>> findPayments();

    @Select("""
            SELECT DISTINCT o.user_id, o.product_id
            FROM orders o
            WHERE o.order_status <> 7
            ORDER BY o.user_id
            """)
    List<Map<String, Object>> findAllPurchases();

    @Select("""
            SELECT DISTINCT product_id
            FROM orders
            WHERE user_id = #{userId}
              AND order_status <> 7
            """)
    List<Long> findUserPurchasedProductIds(Long userId);

    @Select("""
            SELECT o.product_id, o.product_name, SUM(o.quantity) AS total_quantity,
                   SUM(o.total_amount) AS total_amount
            FROM orders o
            WHERE o.order_status NOT IN (7, 8, 9)
              AND (#{merchantId} IS NULL OR o.merchant_id = #{merchantId})
            GROUP BY o.product_id, o.product_name
            ORDER BY total_quantity DESC
            """)
    List<Map<String, Object>> productSalesStats(@Param("merchantId") Long merchantId);

    @Select("""
            SELECT o.user_id, u.username, SUM(o.total_amount) AS total_amount,
                   COUNT(*) AS order_count
            FROM orders o
            LEFT JOIN user u ON o.user_id = u.user_id
            WHERE o.order_status NOT IN (7, 8, 9) AND o.pay_status = 1
            GROUP BY o.user_id, u.username
            ORDER BY total_amount DESC
            """)
    List<Map<String, Object>> userPurchaseStats();

    @Select("""
            SELECT COALESCE(SUM(o.total_amount), 0) AS total_revenue,
                   COUNT(*) AS order_count,
                   COUNT(DISTINCT o.user_id) AS user_count
            FROM orders o
            WHERE o.order_status NOT IN (7, 8, 9) AND o.pay_status = 1
            """)
    Map<String, Object> revenueOverview();
}
