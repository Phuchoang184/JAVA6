package com.leika.shop.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private Integer orderId;
    private String orderStatus;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String note;
    private String addressSnapshot;
    private LocalDateTime createdAt;
    private List<OrderDetailDto> items;
}
