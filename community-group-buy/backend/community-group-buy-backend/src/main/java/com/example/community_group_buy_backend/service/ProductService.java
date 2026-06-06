package com.example.community_group_buy_backend.service;

import java.util.List;
import java.util.Map;

import com.example.community_group_buy_backend.dto.ProductDTO;
import com.example.community_group_buy_backend.vo.ProductVO;

public interface ProductService {
    List<ProductVO> list(Long categoryId, String keyword);

    ProductVO detail(Long productId);

    List<Map<String, Object>> categories();

    ProductVO create(ProductDTO dto);

    ProductVO update(Long productId, ProductDTO dto);

    void audit(Long productId, Integer auditStatus);

    void delete(Long productId);
}
