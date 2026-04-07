package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted user preference profile, auto-computed from UserBehavior events.
 * One row per user (or per session for guests).
 */
@Entity
@Table(name = "UserProfile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProfileId")
    private Long profileId;

    /** Null for guest; unique per registered user */
    @Column(name = "UserId", unique = true)
    private Integer userId;

    /** Always present; guests identified by session only */
    @Column(name = "SessionId", length = 100)
    private String sessionId;

    /** Comma-separated category IDs in preference order, e.g. "3,7,2" */
    @Column(name = "PreferredCategoryIds", length = 500)
    private String preferredCategoryIds;

    /** Comma-separated category names for human-readable prompt injection */
    @Column(name = "PreferredCategoryNames", length = 500, columnDefinition = "NVARCHAR(500)")
    private String preferredCategoryNames;

    /** Comma-separated detected colors, e.g. "đen,trắng,be" */
    @Column(name = "PreferredColors", length = 300, columnDefinition = "NVARCHAR(300)")
    private String preferredColors;

    /** LOW | MID | HIGH – computed from avg spend */
    @Column(name = "PriceSensitivity", length = 10)
    private String priceSensitivity;

    /** Rolling average spend across all interactions */
    @Column(name = "AvgSpend", precision = 18, scale = 2)
    private BigDecimal avgSpend;

    /** Comma-separated style keywords extracted from chat, e.g. "công sở,thanh lịch" */
    @Column(name = "StyleKeywords", length = 500, columnDefinition = "NVARCHAR(500)")
    private String styleKeywords;

    @Column(name = "TotalViews")
    private int totalViews;

    @Column(name = "TotalPurchases")
    private int totalPurchases;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
