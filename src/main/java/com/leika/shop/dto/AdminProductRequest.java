package com.leika.shop.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 300, message = "Tên sản phẩm không được vượt quá 300 ký tự")
    private String productName;

    @NotBlank(message = "Slug không được để trống")
    @Size(max = 350, message = "Slug không được vượt quá 350 ký tự")
    private String slug;

    private String description;

    @NotNull(message = "Giá gốc không được để trống")
    @DecimalMin(value = "0", message = "Giá gốc phải >= 0")
    private BigDecimal basePrice;

    private BigDecimal salePrice;

    private String thumbnailUrl;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    private Integer brandId;

    private boolean isFeatured;

    private List<VariantRequest> variants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantRequest {
        private Integer variantId; // null = create new, present = update

        @NotBlank(message = "SKU không được để trống")
        private String sku;

        @Min(value = 0, message = "Số lượng tồn kho phải >= 0")
        private int stockQty;

        private BigDecimal extraPrice;

        private String imageUrl;
    }
}
