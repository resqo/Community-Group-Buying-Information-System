package com.example.community_group_buy_backend.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.community_group_buy_backend.entity.Product;
import com.example.community_group_buy_backend.vo.ProductVO;

public interface ProductMapper {
    @Select("""
            select p.*, u.shop_name merchant_name, u.avatar_url merchant_avatar, c.category_name
            from product p
            left join user u on p.merchant_id = u.user_id
            left join category c on p.category_id = c.category_id
            where (#{categoryId} is null or p.category_id = #{categoryId})
              and (#{keyword} is null or #{keyword} = '' or p.product_name like concat('%', #{keyword}, '%'))
            order by p.create_time desc
            """)
    List<ProductVO> findProducts(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);

    @Select("""
            select p.*, u.shop_name merchant_name, u.avatar_url merchant_avatar, c.category_name
            from product p
            left join user u on p.merchant_id = u.user_id
            left join category c on p.category_id = c.category_id
            where p.product_id = #{productId}
            """)
    ProductVO findVOById(Long productId);

    @Select("select * from product where product_id = #{productId}")
    Product findById(Long productId);

    @Select("select * from category order by sort asc, category_id asc")
    List<Map<String, Object>> findCategories();

    @Insert("""
            insert into product(merchant_id,category_id,product_name,description,main_image,detail_images,
            original_price,group_price,single_price,stock,sales_count,status,audit_status,create_time)
            values(#{merchantId},#{categoryId},#{productName},#{description},#{mainImage},#{detailImages},
            #{originalPrice},#{groupPrice},#{singlePrice},#{stock},0,#{status},0,now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "productId")
    int insert(Product product);

    @Update("""
            update product set category_id=#{categoryId}, product_name=#{productName}, description=#{description},
            main_image=#{mainImage}, detail_images=#{detailImages}, original_price=#{originalPrice},
            group_price=#{groupPrice}, single_price=#{singlePrice}, stock=#{stock}, status=#{status}
            where product_id=#{productId}
            """)
    int update(Product product);

    @Update("""
            update product
            set audit_status=#{auditStatus},
                status = case
                    when #{auditStatus} = 2 then 0
                    when #{auditStatus} = 1 then 1
                    else status
                end
            where product_id=#{productId}
            """)
    int audit(@Param("productId") Long productId, @Param("auditStatus") Integer auditStatus);

    @Update("update product set stock = stock - #{quantity}, sales_count = sales_count + #{quantity} where product_id = #{productId} and stock >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Delete("delete from product where product_id=#{productId}")
    int delete(Long productId);
}
