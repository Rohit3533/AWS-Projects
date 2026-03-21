package com.devops.orderservice.controller;

import com.devops.orderservice.dto.common.ApiRequest;
import com.devops.orderservice.dto.common.ApiResponse;
import com.devops.orderservice.dto.order.OrderResponse;
import com.devops.orderservice.dto.order.UpdateOrderStatusRequest;
import com.devops.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        log.info("GET /api/admin/orders");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(null, orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/admin/orders/{}", orderId);
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, order));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody ApiRequest<UpdateOrderStatusRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] PUT /api/admin/orders/{}/status", correlationId, orderId);

        OrderResponse order = orderService.updateOrderStatus(orderId, request.getData().getStatus());
        return ResponseEntity.ok(ApiResponse.success(correlationId, order));
    }
}
