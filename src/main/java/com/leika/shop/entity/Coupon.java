package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CouponId")
    private Integer couponId;

    @Column(name = "Code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "DiscountType", nullable = false, length = 10)
    private String discountType; // PERCENT | FIXED

    @Column(name = "DiscountValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MaxUses")
    private Integer maxUses;

    @Column(name = "UsedCount", nullable = false)
    private int usedCount = 0;

    @Column(name = "MinOrderValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "ExpiresAt")
    private LocalDateTime expiresAt;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;
}
