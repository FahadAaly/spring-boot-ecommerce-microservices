package com.app.ecom_application.controller;

import com.app.ecom_application.dto.CartItemRequest;
import com.app.ecom_application.dto.CartItemResponse;
import com.app.ecom_application.model.CartItem;
import com.app.ecom_application.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartItemController {

    private final CartItemService cartService;

    @PostMapping
    public ResponseEntity<String> addToCart(
            @RequestHeader("X-User-ID") String userId,
            @RequestBody CartItemRequest request
    ) {
        try {
            cartService.addtoCart(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Product added to cart");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product not added: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Product not added");
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader("X-User-ID") String userId) {
        try {
            List<CartItem> items = cartService.fetchCart(userId);
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not fetch cart: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not fetch cart");
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<String> removeFromCart(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long productId
    ) {
        try {
            boolean removed = cartService.removeFromCart(userId, productId);
            if (removed) {
                return ResponseEntity.ok("Product removed from cart");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart item not found");
            }
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product not removed: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Product not removed");
        }
    }
}
