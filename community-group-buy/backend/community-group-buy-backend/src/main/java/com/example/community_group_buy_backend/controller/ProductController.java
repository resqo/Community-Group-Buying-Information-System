package com.example.community_group_buy_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.community_group_buy_backend.common.Result;
import com.example.community_group_buy_backend.dto.ProductDTO;
import com.example.community_group_buy_backend.service.ProductService;
import com.example.community_group_buy_backend.vo.ProductVO;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public Result<List<ProductVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(productService.list(categoryId, keyword));
    }

    @GetMapping("/products/{productId}")
    public Result<ProductVO> detail(@PathVariable Long productId) {
        return Result.success(productService.detail(productId));
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.success(productService.categories());
    }

    @PostMapping({"/merchant/products", "/admin/products"})
    public Result<ProductVO> create(@RequestBody ProductDTO dto) {
        return Result.success(productService.create(dto));
    }

    @PutMapping({"/merchant/products/{productId}", "/admin/products/{productId}"})
    public Result<ProductVO> update(@PathVariable Long productId, @RequestBody ProductDTO dto) {
        return Result.success(productService.update(productId, dto));
    }

    @DeleteMapping({"/merchant/products/{productId}", "/admin/products/{productId}"})
    public Result<Void> delete(@PathVariable Long productId) {
        productService.delete(productId);
        return Result.success(null);
    }

    @PostMapping("/admin/products/{productId}/audit")
    public Result<Void> audit(@PathVariable Long productId, @RequestBody Map<String, Integer> body) {
        productService.audit(productId, body.getOrDefault("auditStatus", 1));
        return Result.success(null);
    }

    @GetMapping("/admin/products/audit-list")
    public Result<List<ProductVO>> auditList() {
        return Result.success(productService.list(null, null));
    }
}
