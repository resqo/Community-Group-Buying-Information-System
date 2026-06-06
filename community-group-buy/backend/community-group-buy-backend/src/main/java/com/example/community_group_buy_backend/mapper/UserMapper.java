package com.example.community_group_buy_backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.community_group_buy_backend.entity.User;

public interface UserMapper {
    @Select("select * from user where username = #{username} limit 1")
    User findByUsername(String username);

    @Select("select * from user where user_id = #{userId}")
    User findById(Long userId);

    @Select("select * from user order by create_time desc")
    List<User> findAll();

    @Insert("""
            insert into user(username,password,phone,real_name,role,community_name,address,avatar_url,shop_name,shop_address,free_group_count,status,create_time)
            values(#{username},#{password},#{phone},#{realName},#{role},#{communityName},#{address},#{avatarUrl},#{shopName},#{shopAddress},coalesce(#{freeGroupCount},3),1,now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);

    @Update("""
            update user set phone=#{phone}, real_name=#{realName}, role=#{role}, community_name=#{communityName},
            address=#{address}, avatar_url=#{avatarUrl}, shop_name=#{shopName}, shop_address=#{shopAddress},
            free_group_count=coalesce(#{freeGroupCount}, free_group_count), status=#{status}
            where user_id=#{userId}
            """)
    int update(User user);

    @Update("update user set status = #{status} where user_id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Update("update user set free_group_count = free_group_count - 1 where user_id = #{userId} and free_group_count > 0")
    int decreaseFreeGroupCount(Long userId);
}
