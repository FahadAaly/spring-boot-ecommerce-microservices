package com.app.ecom_application.service;

import com.app.ecom_application.dto.CartItemRequest;
import com.app.ecom_application.dto.CartItemResponse;
import com.app.ecom_application.model.CartItem;
import com.app.ecom_application.model.Product;
import com.app.ecom_application.model.User;
import com.app.ecom_application.repository.CartItemRepository;
import com.app.ecom_application.repository.ProductRepository;
import com.app.ecom_application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void addtoCart(String userIdHeader, CartItemRequest request) {
        if (userIdHeader == null || userIdHeader.isBlank() || request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("Invalid cart add request");
        }
        Long userId = parseUserId(userIdHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        int qty = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();

        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + qty);
                    // keep the current price; or update to latest product price
                    existing.setPrice(product.getPrice());
                    return existing;
                })
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setUser(user);
                    ci.setProduct(product);
                    ci.setQuantity(qty);
                    ci.setPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
                    return ci;
                });

        cartItemRepository.save(item);
    }

    public boolean removeFromCart(String userIdHeader, Long productId) {
        if (userIdHeader == null || userIdHeader.isBlank() || productId == null) {
            throw new IllegalArgumentException("Invalid cart remove request");
        }
        Long userId = parseUserId(userIdHeader);
        // Validate user exists to align with add flow semantics
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return cartItemRepository.findByUserIdAndProductId(userId, productId)
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
        Long userId = parseUserId(userIdHeader);
        // ensure user exists similar to add/remove semantics
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return cartItemRepository.findByUserId(userId);
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse resp = new CartItemResponse();
        resp.setId(String.valueOf(item.getId()));
        if (item.getProduct() != null) {
            resp.setProductId(String.valueOf(item.getProduct().getId()));
            resp.setProductName(item.getProduct().getName());
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
        userRepository.findById(Long.valueOf(userId)).ifPresent(cartItemRepository::deleteByUser);
    }
}
