package com.leika.shop.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BehaviorRequest {

    /** Browser-generated session UUID — required */
    private String sessionId;

    /** Required */
    private Integer productId;

    /** VIEW | CART_ADD | PURCHASE | WISHLIST */
    private String actionType;
}
