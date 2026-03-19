package com.devops.productservice.controller;

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
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product product) {
        try {
            log.info("POST /api/products — name: {}", product.getName());
            Product created = productService.create(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("Product creation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            log.info("GET /api/products/{}", id);
            Product product = productService.findById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            log.error("Product lookup error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        log.info("GET /api/products");
        return ResponseEntity.ok(productService.findAll());
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<?> reserveStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer quantity = body.get("quantity");
            log.info("POST /api/products/{}/reserve — quantity: {}", id, quantity);
            Product updated = productService.reserveStock(id, quantity);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Stock reservation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
