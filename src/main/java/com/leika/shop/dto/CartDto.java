package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {
    private Integer cartId;
    private List<CartItemDto> items;
    private int totalItems;
    private BigDecimal subTotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
}
