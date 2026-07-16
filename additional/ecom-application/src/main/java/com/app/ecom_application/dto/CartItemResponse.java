package com.app.ecom_application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private BigDecimal price; // unit price
    private Integer quantity;
}
