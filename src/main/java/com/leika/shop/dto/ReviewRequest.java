package com.leika.shop.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {
    private Integer productId;
    private Integer rating;
    private String title;
    private String body;
}
