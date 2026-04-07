package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDto {
    private String productName;
    private String variantInfo;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
