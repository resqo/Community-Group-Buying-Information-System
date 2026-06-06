package com.example.community_group_buy_backend.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.dto.ProductDTO;
import com.example.community_group_buy_backend.entity.Product;
import com.example.community_group_buy_backend.mapper.ProductMapper;
import com.example.community_group_buy_backend.service.ProductService;
import com.example.community_group_buy_backend.vo.ProductVO;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public List<ProductVO> list(Long categoryId, String keyword) {
        return productMapper.findProducts(categoryId, keyword);
    }

    @Override
    public ProductVO detail(Long productId) {
        ProductVO product = productMapper.findVOById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    @Override
    public List<Map<String, Object>> categories() {
        return productMapper.findCategories();
    }

    @Override
    public ProductVO create(ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        if (product.getStatus() == null) {
            product.setStatus(0);
        }
        productMapper.insert(product);
        return detail(product.getProductId());
    }

    @Override
    public ProductVO update(Long productId, ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setProductId(productId);
        productMapper.update(product);
        return detail(productId);
    }

    @Override
    public void audit(Long productId, Integer auditStatus) {
        productMapper.audit(productId, auditStatus);
    }

    @Override
    public void delete(Long productId) {
        productMapper.delete(productId);
    }
}
