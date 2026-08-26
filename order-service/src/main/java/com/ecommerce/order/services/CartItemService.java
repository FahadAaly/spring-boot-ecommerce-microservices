package com.ecommerce.order.services;

import com.ecommerce.order.clients.ProductServiceClient;
import com.ecommerce.order.clients.UserServiceClient;
import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.dto.UserResponse;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.repository.CartItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;

    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "addToCartFallback")
    public void addToCart(String userIdHeader, CartItemRequest request) {
        String userId = validateAndGetUserId(userIdHeader);

        if (request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("Invalid cart add request: product ID is required");
        }

        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // 1. Verify User exists
        validateUserExists(userId);

        // 2. Verify Product exists and fetch price
        ProductResponse product = productServiceClient.getProductDetails(String.valueOf(request.getProductId()))
                .orElseThrow(() -> new IllegalArgumentException("Product with ID " + request.getProductId() + " does not exist"));

        int qty = request.getQuantity();
        BigDecimal currentPrice = product.getPrice();

        // 3. Upsert cart item
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + qty);
                    existing.setPrice(currentPrice);
                    return existing;
                })
                .orElseGet(() -> CartItem.builder()
                        .userId(userId)
                        .productId(request.getProductId())
                        .quantity(qty)
                        .price(currentPrice)
                        .build());

        cartItemRepository.save(item);
    }

    /**
     * Fallback for addToCart when downstream services fail or circuit is OPEN.
     */
    public void addToCartFallback(String userIdHeader, CartItemRequest request, Throwable throwable) {
        log.error("Fallback triggered for userId: {}, productId: {}. Reason: {}",
                userIdHeader,
                request != null ? request.getProductId() : "N/A",
                throwable.getMessage());

        // Do not mask client-side bad requests (400)
        if (throwable instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) throwable;
        }

        throw new IllegalStateException("Product or User service is currently unavailable. Please try again later.");
    }

    @Transactional
    public boolean removeFromCart(String userIdHeader, Long productId) {
        String userId = validateAndGetUserId(userIdHeader);

        if (productId == null) {
            throw new IllegalArgumentException("Invalid cart remove request: product ID is required");
        }

        validateUserExists(userId);

        return cartItemRepository.findByUserIdAndProductId(userId, productId)
                .map(item -> {
                    cartItemRepository.delete(item);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<CartItem> fetchCart(String userIdHeader) {
        String userId = validateAndGetUserId(userIdHeader);
        validateUserExists(userId);
        return cartItemRepository.findByUserId(userId);
    }

    @Transactional
    public void clearCart(String userIdHeader) {
        String userId = validateAndGetUserId(userIdHeader);
        cartItemRepository.deleteByUserId(userId);
    }

    // ==========================================
    // Internal Helper Methods
    // ==========================================

    private String validateAndGetUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("X-User-ID header cannot be null or empty");
        }
        return userIdHeader.trim();
    }

    private UserResponse validateUserExists(String userId) {
        return userServiceClient.getUserDetails(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " does not exist"));
    }
}