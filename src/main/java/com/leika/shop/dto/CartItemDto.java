package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Integer cartItemId;
    private Integer variantId;
    private String productName;
    private String variantInfo; // SKU or size/color
    private String thumbnailUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private int stockQty;
}
