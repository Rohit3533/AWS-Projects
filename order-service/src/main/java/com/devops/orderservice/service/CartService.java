package com.devops.orderservice.service;

import com.devops.orderservice.dto.cart.*;
import com.devops.orderservice.model.Cart;
import com.devops.orderservice.model.CartItem;
import com.devops.orderservice.repository.CartItemRepository;
import com.devops.orderservice.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @CacheEvict(value = "carts", key = "#request.userId")
    public CartResponse addToCart(AddToCartRequest request, String authToken) {
        log.info("Adding to cart — userId: {}, productId: {}, qty: {}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        // Fetch product details from product-service
        String productUrl = productServiceUrl + "/api/user/products/" + request.getProductId();
        Map<String, Object> productResponse;
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + authToken);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            var responseEntity = restTemplate.exchange(productUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
            if (responseEntity.getBody() == null || responseEntity.getBody().get("data") == null) {
                throw new RuntimeException("Product data not found in response for id: " + request.getProductId());
            }
            productResponse = (Map<String, Object>) responseEntity.getBody().get("data");
        } catch (Exception e) {
            log.error("Failed to fetch product: {}", e.getMessage());
            throw new RuntimeException("Product not found or product service unavailable: " + request.getProductId());
        }

        String productName = (String) productResponse.get("name");
        Double price = ((Number) productResponse.get("price")).doubleValue();

        // Get or create cart
        Cart cart = cartRepository.findByUserId(request.getUserId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(request.getUserId());
                    return cartRepository.save(newCart);
                });

        // Check if product already in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPrice(price);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(productName);
            newItem.setPrice(price);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
            cartRepository.save(cart);
        }

        return toCartResponse(cartRepository.findByUserId(request.getUserId()).get());
    }

    @Cacheable(value = "carts", key = "#userId")
    public CartResponse getCart(Long userId) {
        log.info("Fetching cart for userId: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        return toCartResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", allEntries = true)
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        log.info("Updating cart item {} — qty: {}", cartItemId, request.getQuantity());
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return toCartResponse(cartRepository.findById(item.getCart().getId()).get());
    }

    @Transactional
    @CacheEvict(value = "carts", allEntries = true)
    public void removeCartItem(Long cartItemId) {
        log.info("Removing cart item: {}", cartItemId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));
        cartItemRepository.delete(item);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userId")
    public void clearCart(Long userId) {
        log.info("Clearing cart for userId: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public Cart getCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart is empty for user: " + userId));
    }

    private CartResponse toCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUserId());
        response.setItems(cart.getItems().stream().map(item -> {
            CartItemDTO dto = new CartItemDTO();
            dto.setCartItemId(item.getId());
            dto.setProductId(item.getProductId());
            dto.setProductName(item.getProductName());
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            dto.setSubtotal(item.getPrice() * item.getQuantity());
            return dto;
        }).collect(Collectors.toList()));

        double total = response.getItems().stream()
                .mapToDouble(CartItemDTO::getSubtotal).sum();
        response.setTotalPrice(total);

        return response;
    }
}
