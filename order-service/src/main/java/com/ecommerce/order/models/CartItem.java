package com.ecommerce.order.models;

import com.rabbitmq.client.AMQP;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Data
@NoArgsConstructor       // 👈 Required by Hibernate to instantiate the entity
@AllArgsConstructor
@Builder // 👈 Enables CartItem.builder()
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Long productId;

    private BigDecimal price;
    private Integer quantity;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
