package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.models.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public void addProduct(ProductRequest request) {
        if (request == null) return;
        Product product = new Product();
        updateProductFromRequest(product, request);
        // If active not provided, keep default true from entity
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        productRepository.save(product);
    }

    public Optional<Product> updateProduct(Long id, ProductRequest request) {
        return productRepository.findById(id).map(existing -> {
            updateProductFromRequest(existing, request);
            if (request.getActive() != null) {
                existing.setActive(request.getActive());
            }
            productRepository.save(existing);
            return existing;
        });
    }

    public Optional<ProductResponse> fetchProduct(Long id) {
        return productRepository.findById(id).map(this::mapToProductResponse);
    }

    public List<ProductResponse> fetchAllProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public boolean deleteProduct(Long id) {
        return productRepository.findById(id).map(p -> {
            productRepository.delete(p);
            return true;
        }).orElse(false);
    }

    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private void updateProductFromRequest(Product product, ProductRequest request) {
        if (product == null || request == null) return;
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageURL(request.getImageURL());
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(String.valueOf(product.getId()));
        resp.setName(product.getName());
        resp.setDescription(product.getDescription());
        resp.setPrice(product.getPrice());
        resp.setStockQuantity(product.getStockQuantity());
        resp.setCategory(product.getCategory());
        resp.setImageURL(product.getImageURL());
        resp.setActive(product.getActive());
        return resp;
    }
}
