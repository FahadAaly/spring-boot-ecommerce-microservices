package com.ecommerce.order.services;

import com.ecommerce.order.clients.ProductServiceClient;
import com.ecommerce.order.clients.UserServiceClient;
import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.CartItemResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.dto.UserResponse;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    private final ProductServiceClient productServiceClient;

    private final UserServiceClient userServiceClient;

    public void addtoCart(String userIdHeader, CartItemRequest request) {
        if (userIdHeader == null || userIdHeader.isBlank() || request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("Invalid cart add request");
        }

        String userId = userIdHeader.trim();

        // 2. Validate quantity (prevents negative or zero quantity items)
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // 3. Parse user ID with explicit exception handling
        UserResponse user = userServiceClient
                .getUserDetails(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User with ID " + userId + " does not exist"
                ));

        // 1. Fetch product details or throw exception if missing
        ProductResponse product = productServiceClient
                .getProductDetails(String.valueOf(request.getProductId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product with ID " + request.getProductId() + " does not exist"
                ));


        int qty = request.getQuantity();
        BigDecimal currentPrice = product.getPrice();

        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + qty);
                    existing.setPrice(currentPrice);
                    return existing;
                })
                .orElseGet(() -> {
                    CartItem cartItem = new CartItem();
                    cartItem.setUserId(userId);
                    cartItem.setProductId(request.getProductId());
                    cartItem.setQuantity(qty);
                    cartItem.setPrice(currentPrice);
                    return cartItem;
                });

        cartItemRepository.save(item);
    }

    public boolean removeFromCart(String userIdHeader, Long productId) {
        if (userIdHeader == null || userIdHeader.isBlank() || productId == null) {
            throw new IllegalArgumentException("Invalid cart remove request");
        }
        String userId = userIdHeader.trim();
        //        // Validate user exists to align with add flow semantics
        UserResponse user = userServiceClient
                .getUserDetails(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User with ID " + userId + " does not exist"
                ));
        return cartItemRepository.findByUserIdAndProductId(String.valueOf(userId), productId)
                .map(item -> {
                    cartItemRepository.delete(item);
                    return true;
                })
                .orElse(false);
    }

    public List<CartItem> fetchCart(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("Invalid cart fetch request");
        }
        String userId = userIdHeader.trim();
        // ensure user exists similar to add/remove semantics
        UserResponse user = userServiceClient
                .getUserDetails(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User with ID " + userId + " does not exist"
                ));
        return cartItemRepository.findByUserId(userId);
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse resp = new CartItemResponse();
        resp.setId(String.valueOf(item.getId()));
        if (item.getProductId() != null) {
            resp.setProductId(String.valueOf(item.getProductId()));
        }
        resp.setPrice(item.getPrice());
        resp.setQuantity(item.getQuantity());
        return resp;
    }

    private Long parseUserId(String header) {
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid X-User-ID header");
        }
    }

    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
