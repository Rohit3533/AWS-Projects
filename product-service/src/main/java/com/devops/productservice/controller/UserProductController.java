package com.devops.productservice.controller;

import com.devops.productservice.dto.common.ApiResponse;
import com.devops.productservice.model.Product;
import com.devops.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/products")
public class UserProductController {

    private static final Logger log = LoggerFactory.getLogger(UserProductController.class);
    private final ProductService productService;

    public UserProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAll() {
        log.info("GET /api/user/products");
        return ResponseEntity.ok(ApiResponse.success(null, productService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        log.info("GET /api/user/products/{}", id);
        return ResponseEntity.ok(ApiResponse.success(null, productService.findById(id)));
    }
}
