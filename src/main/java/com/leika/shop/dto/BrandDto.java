package com.leika.shop.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDto {
    private Integer brandId;
    private String brandName;
    private String logoUrl;
}
