package com.example.community_group_buy_backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.community_group_buy_backend.entity.GroupBuy;

public interface GroupBuyMapper {
    @Select("""
            select ga.*, p.product_name, p.main_image
            from group_activity ga
            left join product p on ga.product_id = p.product_id
            order by ga.create_time desc
            """)
    List<Map<String, Object>> findActivities();

    @Select("select * from group_activity where activity_id = #{activityId}")
    GroupBuy findActivityById(Long activityId);

    @Select("""
            select *
            from group_activity
            where product_id = #{productId}
              and status = 1
              and start_time <= now()
              and end_time >= now()
            order by create_time desc
            limit 1
            """)
    GroupBuy findActiveActivityByProductId(Long productId);

    @Insert("""
            insert into group_activity(product_id,group_price,group_size,start_time,end_time,allow_free_group,free_group_limit,status,create_time)
            values(#{productId},#{groupPrice},#{groupSize},#{startTime},#{endTime},#{allowFreeGroup},#{freeGroupLimit},#{status},now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "activityId")
    int insertActivity(GroupBuy activity);

    @Update("""
            update group_activity set product_id=#{productId}, group_price=#{groupPrice}, group_size=#{groupSize},
            start_time=#{startTime}, end_time=#{endTime}, allow_free_group=#{allowFreeGroup},
            free_group_limit=#{freeGroupLimit}, status=#{status}
            where activity_id=#{activityId}
            """)
    int updateActivity(GroupBuy activity);

    @Insert("""
            insert into group_instance(activity_id,leader_user_id,current_count,required_count,status,expire_time,create_time)
            values(#{activityId},#{leaderUserId},#{currentCount},#{requiredCount},#{status},#{expireTime},now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "groupId")
    int insertInstance(GroupBuy group);

    @Select("select * from group_instance where group_id = #{groupId}")
    GroupBuy findInstanceById(Long groupId);

    @Select("""
            select gi.*, ga.product_id, ga.group_price, p.product_name, p.main_image,
                   lu.username leader_username, lu.avatar_url leader_avatar,
                   coalesce(group_concat(distinct ou.avatar_url order by o.create_time separator ','), '') participant_avatars,
                   coalesce(group_concat(distinct ou.username order by o.create_time separator ','), '') participant_names
            from group_instance gi
            left join group_activity ga on gi.activity_id = ga.activity_id
            left join product p on ga.product_id = p.product_id
            left join user lu on gi.leader_user_id = lu.user_id
            left join orders o on gi.group_id = o.group_id and o.order_status <> 7
            left join user ou on o.user_id = ou.user_id
            where gi.status = 0
              and gi.expire_time > now()
              and gi.current_count > 0
              and gi.current_count < gi.required_count
              and (#{userId} is null or gi.leader_user_id <> #{userId})
            group by gi.group_id, ga.product_id, ga.group_price, p.product_name, p.main_image, lu.username, lu.avatar_url
            order by gi.create_time desc
            """)
    List<Map<String, Object>> findOpenGroups(@Param("userId") Long userId);

    @Select("""
            select gi.*, ga.product_id, ga.group_price, p.product_name, p.main_image,
                   lu.username leader_username,
                   case when gi.leader_user_id = #{userId} then 'STARTED' else 'JOINED' end relation_type,
                   coalesce(group_concat(distinct ou.username order by o.create_time separator ','), '') participant_names
            from group_instance gi
            left join group_activity ga on gi.activity_id = ga.activity_id
            left join product p on ga.product_id = p.product_id
            left join user lu on gi.leader_user_id = lu.user_id
            left join orders o on gi.group_id = o.group_id and o.order_status <> 7
            left join user ou on o.user_id = ou.user_id
            where gi.leader_user_id = #{userId}
               or exists (
                    select 1 from orders mo
                    where mo.group_id = gi.group_id
                      and mo.user_id = #{userId}
                      and mo.order_status <> 7
               )
            group by gi.group_id, ga.product_id, ga.group_price, p.product_name, p.main_image, lu.username
            order by gi.create_time desc
            """)
    List<Map<String, Object>> findMyGroups(Long userId);

    @Update("update group_instance set current_count = current_count + 1 where group_id = #{groupId} and current_count < required_count")
    int increaseCount(Long groupId);

    @Update("""
            update group_instance
            set current_count = greatest(current_count - 1, 0),
                status = case
                    when status = 1 and greatest(current_count - 1, 0) < required_count then 0
                    else status
                end
            where group_id = #{groupId}
            """)
    int decreaseCount(Long groupId);

    @Update("update group_instance set status = 1 where group_id = #{groupId} and current_count >= required_count")
    int markSuccessIfFull(Long groupId);

    @Update("update group_instance set status = 2 where status = 0 and current_count = 0 and create_time < date_sub(now(), interval 30 minute)")
    int cancelEmptyGroups();

    @Select("select group_price from group_activity where activity_id = #{activityId}")
    BigDecimal findGroupPrice(Long activityId);

    @Select("select now()")
    LocalDateTime now();
}
