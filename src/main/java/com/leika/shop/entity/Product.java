package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BrandId")
    private Brand brand;

    @Column(name = "ProductName", nullable = false, columnDefinition = "NVARCHAR(300)")
    private String productName;

    @Column(name = "Slug", nullable = false, unique = true, length = 350)
    private String slug;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "BasePrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "SalePrice", precision = 18, scale = 2)
    private BigDecimal salePrice; // Nullable

    @Column(name = "ThumbnailUrl", length = 500)
    private String thumbnailUrl;

    @Column(name = "IsFeatured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
