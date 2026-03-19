package com.devops.orderservice.service;

import com.devops.orderservice.model.Order;
import com.devops.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        log.info("=== ORDER CREATION STARTED === userId: {}, productId: {}, quantity: {}",
                userId, productId, quantity);

        // Step 1: Validate user exists by calling User Service
        log.info("Step 1: Validating user — calling User Service at {}", userServiceUrl);
        Map<String, Object> user;
        try {
            String userUrl = userServiceUrl + "/api/users/" + userId;
            log.info("Calling: GET {}", userUrl);
            user = restTemplate.getForObject(userUrl, Map.class);
            log.info("User validated — name: {}, email: {}", user.get("name"), user.get("email"));
        } catch (HttpClientErrorException e) {
            log.error("User Service returned error — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("User not found: " + userId);
        } catch (Exception e) {
            log.error("Failed to reach User Service: {}", e.getMessage());
            throw new RuntimeException("User Service unavailable: " + e.getMessage());
        }

        // Step 2: Validate product and reserve stock by calling Product Service
        log.info("Step 2: Reserving stock — calling Product Service at {}", productServiceUrl);
        Map<String, Object> product;
        try {
            // First fetch product details
            String productUrl = productServiceUrl + "/api/products/" + productId;
            log.info("Calling: GET {}", productUrl);
            product = restTemplate.getForObject(productUrl, Map.class);
            log.info("Product found — name: {}, price: {}, stock: {}",
                    product.get("name"), product.get("price"), product.get("stock"));

            // Reserve stock
            String reserveUrl = productServiceUrl + "/api/products/" + productId + "/reserve";
            log.info("Calling: POST {} with quantity: {}", reserveUrl, quantity);
            restTemplate.postForObject(reserveUrl, Map.of("quantity", quantity), Map.class);
            log.info("Stock reserved successfully");
        } catch (HttpClientErrorException e) {
            log.error("Product Service returned error — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Product reservation failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to reach Product Service: {}", e.getMessage());
            throw new RuntimeException("Product Service unavailable: " + e.getMessage());
        }

        // Step 3: Create and save the order
        log.info("Step 3: Creating order record");
        Double price = ((Number) product.get("price")).doubleValue();
        Double totalPrice = price * quantity;

        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus("CONFIRMED");
        order.setUserName((String) user.get("name"));
        order.setProductName((String) product.get("name"));

        Order saved = orderRepository.save(order);
        log.info("=== ORDER CREATION COMPLETED === orderId: {}, totalPrice: {}, status: {}",
                saved.getId(), saved.getTotalPrice(), saved.getStatus());

        return saved;
    }

    public Order findById(Long id) {
        log.info("Fetching order by id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found — id: {}", id);
                    return new RuntimeException("Order not found: " + id);
                });
    }

    public List<Order> findAll() {
        log.info("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        log.info("Found {} orders", orders.size());
        return orders;
    }
}
