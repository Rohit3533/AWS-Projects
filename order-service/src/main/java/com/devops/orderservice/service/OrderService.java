package com.devops.orderservice.service;

import com.devops.orderservice.dto.order.OrderItemDTO;
import com.devops.orderservice.dto.order.OrderResponse;
import com.devops.orderservice.model.*;
import com.devops.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, CartService cartService,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse checkout(Long userId, Long addressId) {
        log.info("=== CHECKOUT STARTED === userId: {}, addressId: {}", userId, addressId);

        // Step 1: Get cart
        Cart cart = cartService.getCartEntity(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add items before checkout.");
        }

        // Step 2: Calculate total
        double totalPrice = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // Step 3: Process payment
        log.info("Step 2: Processing payment — amount: {}", totalPrice);
        Map<String, String> paymentResult = paymentService.processPayment(totalPrice, userId);

        // Step 4: Create order
        log.info("Step 3: Creating order record");
        Order order = new Order();
        order.setUserId(userId);
        order.setDeliveryAddressId(addressId);
        order.setTotalPrice(totalPrice);
        order.setStatus("CONFIRMED");
        order.setPaymentId(paymentResult.get("paymentId"));
        order.setPaymentStatus(paymentResult.get("status"));

        // Copy cart items to order items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);
        }

        Order saved = orderRepository.save(order);

        // Step 5: Clear cart
        cartService.clearCart(userId);
        log.info("=== CHECKOUT COMPLETED === orderId: {}", saved.getId());

        return toOrderResponse(saved);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toOrderResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        log.info("Updating order {} status to: {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUserId());
        response.setDeliveryAddressId(order.getDeliveryAddressId());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setPaymentId(order.getPaymentId());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        response.setItems(order.getItems().stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setProductId(item.getProductId());
            dto.setProductName(item.getProductName());
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList()));
        return response;
    }
}
