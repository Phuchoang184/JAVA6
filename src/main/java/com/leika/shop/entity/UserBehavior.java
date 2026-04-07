package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks every product interaction (view, cart-add, purchase, wishlist).
 * Works for both authenticated users (userId set) and guests (sessionId only).
 */
@Entity
@Table(name = "UserBehavior", indexes = {
        @Index(name = "idx_ub_user",    columnList = "UserId"),
        @Index(name = "idx_ub_session", columnList = "SessionId"),
        @Index(name = "idx_ub_product", columnList = "ProductId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BehaviorId")
    private Long behaviorId;

    /** Null for guest users */
    @Column(name = "UserId")
    private Integer userId;

    /** Browser-generated UUID — always present */
    @Column(name = "SessionId", length = 100)
    private String sessionId;

    @Column(name = "ProductId", nullable = false)
    private Integer productId;

    @Column(name = "CategoryId")
    private Integer categoryId;

    /** VIEW | CART_ADD | PURCHASE | WISHLIST */
    @Column(name = "ActionType", length = 20, nullable = false)
    private String actionType;

    @Column(name = "PriceAtAction", precision = 18, scale = 2)
    private BigDecimal priceAtAction;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;
}
