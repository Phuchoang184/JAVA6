package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantDto {
    private Integer variantId;
    private String sku;
    private String displayLabel;
    private int stockQty;
    private BigDecimal extraPrice;
    private String imageUrl;
}
