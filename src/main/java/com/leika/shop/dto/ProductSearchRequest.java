package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String keyword;
    private Integer categoryId;
    private Integer brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    @Builder.Default
    private String sortBy = "newest";

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 12;
}
