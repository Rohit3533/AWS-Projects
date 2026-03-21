package com.devops.orderservice.controller;

import com.devops.orderservice.dto.cart.*;
import com.devops.orderservice.dto.common.ApiRequest;
import com.devops.orderservice.dto.common.ApiResponse;
import com.devops.orderservice.dto.order.CheckoutRequest;
import com.devops.orderservice.dto.order.OrderResponse;
import com.devops.orderservice.service.CartService;
import com.devops.orderservice.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserOrderController {

    private static final Logger log = LoggerFactory.getLogger(UserOrderController.class);
    private final CartService cartService;
    private final OrderService orderService;

    public UserOrderController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    // ============ CART ENDPOINTS ============

    @PostMapping("/cart/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @RequestBody ApiRequest<AddToCartRequest> request,
            HttpServletRequest httpRequest) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/user/cart/add", correlationId);

        String authToken = extractToken(httpRequest);
        CartResponse cart = cartService.addToCart(request.getData(), authToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, cart));
    }

    @GetMapping("/cart/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable Long userId) {
        log.info("GET /api/user/cart/{}", userId);
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null, cart));
    }

    @PutMapping("/cart/item/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody ApiRequest<UpdateCartItemRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] PUT /api/user/cart/item/{}", correlationId, cartItemId);

        CartResponse cart = cartService.updateCartItem(cartItemId, request.getData());
        return ResponseEntity.ok(ApiResponse.success(correlationId, cart));
    }

    @DeleteMapping("/cart/item/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable Long cartItemId) {
        log.info("DELETE /api/user/cart/item/{}", cartItemId);
        cartService.removeCartItem(cartItemId);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    @DeleteMapping("/cart/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long userId) {
        log.info("DELETE /api/user/cart/{}/clear", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }

    // ============ ORDER ENDPOINTS ============

    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestBody ApiRequest<CheckoutRequest> request) {
        String correlationId = request.getCorrelationId();
        log.info("[{}] POST /api/user/orders/checkout", correlationId);

        CheckoutRequest data = request.getData();
        OrderResponse order = orderService.checkout(data.getUserId(), data.getAddressId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(correlationId, order));
    }

    @GetMapping("/orders/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(@PathVariable Long userId) {
        log.info("GET /api/user/orders/{}", userId);
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(null, orders));
    }

    @GetMapping("/orders/detail/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable Long orderId) {
        log.info("GET /api/user/orders/detail/{}", orderId);
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, order));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
