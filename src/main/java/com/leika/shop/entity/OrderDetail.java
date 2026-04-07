package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "OrderDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailId")
    private Integer orderDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId") // variant có thể bị null nếu bị xóa sau này
    private ProductVariant productVariant;

    @Column(name = "ProductName", nullable = false, length = 300)
    private String productName;

    @Column(name = "VariantInfo", length = 300)
    private String variantInfo;

    @Column(name = "SKU", length = 100)
    private String sku;

    @Column(name = "UnitPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "Quantity", nullable = false)
    private int quantity;
    
    // LineTotal tính trực tiếp bằng (UnitPrice * Quantity) nên ko cần field map trong Entity (trừ khi cần).
}
