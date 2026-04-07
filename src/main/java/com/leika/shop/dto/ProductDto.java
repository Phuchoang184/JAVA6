package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Integer productId;
    private String productName;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String thumbnailUrl;
    private boolean isFeatured;
    private String categoryName;
    private Integer categoryId;
    private String brandName;
    private Integer brandId;
    private String displayImageUrl;
    private Integer defaultVariantId;
    private Integer totalStockQty;
    private List<VariantDto> variants;

    // Convenience: effective price
    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : basePrice;
    }

    public Integer getDiscountPercent() {
        if (salePrice != null && basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
            return basePrice.subtract(salePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(basePrice, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }
}
