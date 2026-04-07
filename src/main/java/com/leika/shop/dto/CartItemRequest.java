package com.leika.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    @NotNull(message = "Mã biến thể sản phẩm không được để trống")
    private Integer variantId;

    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    @Max(value = 99, message = "Số lượng không được vượt quá 99")
    private int quantity = 1;
}
