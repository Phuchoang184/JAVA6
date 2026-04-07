package com.leika.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lightweight product card returned inside an AI chat message.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatProductCard {

    private Integer productId;
    private String  productName;
    private String  thumbnailUrl;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String  slug;
    private String  categoryName;
    private Integer discountPercent;

    /** Convenience: returns salePrice if available, otherwise basePrice */
    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : basePrice;
    }
}
