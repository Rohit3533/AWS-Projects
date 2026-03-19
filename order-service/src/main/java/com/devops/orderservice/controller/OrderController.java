package com.devops.orderservice.controller;

import com.devops.orderservice.model.Order;
import com.devops.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        try {
            Long userId = ((Number) body.get("userId")).longValue();
            Long productId = ((Number) body.get("productId")).longValue();
            Integer quantity = ((Number) body.get("quantity")).intValue();

            log.info("POST /api/orders — userId: {}, productId: {}, quantity: {}",
                    userId, productId, quantity);

            Order order = orderService.createOrder(userId, productId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            log.error("Order creation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            log.info("GET /api/orders/{}", id);
            Order order = orderService.findById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Order lookup error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        log.info("GET /api/orders");
        return ResponseEntity.ok(orderService.findAll());
    }
}
