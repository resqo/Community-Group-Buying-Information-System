package com.example.community_group_buy_backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.community_group_buy_backend.entity.PickupPoint;

public interface PickupPointMapper {
    @Select("select * from pickup_point where status = 1 order by pickup_point_id desc")
    List<PickupPoint> findEnabled();

    @Select("select * from pickup_point where leader_id = #{leaderId} order by pickup_point_id desc")
    List<PickupPoint> findByLeader(Long leaderId);

    @Select("select * from pickup_point where pickup_point_id = #{pickupPointId}")
    PickupPoint findById(Long pickupPointId);

    @Insert("""
            insert into pickup_point(leader_id,point_name,community_name,address,phone,business_hours,status)
            values(#{leaderId},#{pointName},#{communityName},#{address},#{phone},#{businessHours},#{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "pickupPointId")
    int insert(PickupPoint pickupPoint);

    @Update("""
            update pickup_point set point_name=#{pointName}, community_name=#{communityName}, address=#{address},
            phone=#{phone}, business_hours=#{businessHours}, status=#{status}
            where pickup_point_id=#{pickupPointId}
            """)
    int update(PickupPoint pickupPoint);
}
