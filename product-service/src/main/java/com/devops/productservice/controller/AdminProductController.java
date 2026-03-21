package com.devops.productservice.controller;

import com.devops.productservice.dto.common.ApiRequest;
import com.devops.productservice.dto.common.ApiResponse;
import com.devops.productservice.dto.product.ProductRequest;
import com.devops.productservice.model.Product;
import com.devops.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestBody ApiRequest<ProductRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/admin/products", correlationId);

        ProductRequest data = request.getData();
        Product product = new Product(data.getName(), data.getDescription(), data.getPrice(), data.getStock());
        Product created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(
            @PathVariable Long id,
            @RequestBody ApiRequest<ProductRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] PUT /api/admin/products/{}", correlationId, id);

        Product updated = productService.update(id, request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/admin/products/{}", id);
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAll() {
        log.info("GET /api/admin/products");
        return ResponseEntity.ok(ApiResponse.success(null, productService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        log.info("GET /api/admin/products/{}", id);
        return ResponseEntity.ok(ApiResponse.success(null, productService.findById(id)));
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<ApiResponse<Product>> reserveStock(
            @PathVariable Long id,
            @RequestBody ApiRequest<Map<String, Integer>> request) {
        String correlationId = request.getCorrelationId();
        Integer quantity = request.getData().get("quantity");
        log.info("[{}] POST /api/admin/products/{}/reserve — qty: {}", correlationId, id, quantity);

        Product updated = productService.reserveStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(correlationId, updated));
    }
}
