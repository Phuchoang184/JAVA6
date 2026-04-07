package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ProductVariants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantId")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "SKU", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "StockQty", nullable = false)
    private int stockQty = 0;

    @Column(name = "ExtraPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(name = "ImageUrl", length = 500)
    private String imageUrl;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;
}
