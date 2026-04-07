package com.leika.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Read-only DTO returned to the frontend / injected into AI prompts.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String preferredCategoryIds;   // "3,7,2"
    private String preferredCategoryNames; // "Váy, Áo"
    private String preferredColors;        // "đen, trắng"
    private String priceSensitivity;       // "low" | "mid" | "high"
    private BigDecimal avgSpend;
    private String styleKeywords;          // "công sở, thanh lịch"
    private int totalViews;
    private int totalPurchases;
}
