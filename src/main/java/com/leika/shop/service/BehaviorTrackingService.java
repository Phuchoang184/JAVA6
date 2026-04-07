package com.leika.shop.service;

import com.leika.shop.dto.UserProfileDto;
import com.leika.shop.entity.Product;
import com.leika.shop.entity.UserBehavior;
import com.leika.shop.entity.UserProfile;
import com.leika.shop.repository.CategoryRepository;
import com.leika.shop.repository.ProductRepository;
import com.leika.shop.repository.UserBehaviorRepository;
import com.leika.shop.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorTrackingService {

    private static final int TOP_CATEGORIES  = 3;
    private static final int TOP_COLORS      = 3;

    private final UserBehaviorRepository behaviorRepository;
    private final UserProfileRepository  profileRepository;
    private final ProductRepository      productRepository;
    private final CategoryRepository     categoryRepository;

    // ------------------------------------------------------------------ //
    //  Track a single interaction
    // ------------------------------------------------------------------ //

    @Transactional
    public void track(Integer userId, String sessionId, Integer productId, String actionType) {
        if (sessionId == null || sessionId.isBlank() || productId == null) return;

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        UserBehavior behavior = UserBehavior.builder()
                .userId(userId)
                .sessionId(sessionId)
                .productId(productId)
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .actionType(actionType.toUpperCase())
                .priceAtAction(product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice())
                .build();

        behaviorRepository.save(behavior);

        // Trigger async profile recompute
        recomputeProfile(userId, sessionId);
    }

    // ------------------------------------------------------------------ //
    //  Async profile recompute (called after each track event)
    // ------------------------------------------------------------------ //

    @Async
    @Transactional
    public void recomputeProfile(Integer userId, String sessionId) {
        try {
            UserProfile profile = loadOrCreate(userId, sessionId);

            // ── Preferred categories ─────────────────────────────────
            List<Object[]> catRows = userId != null
                    ? behaviorRepository.findTopCategoriesByUserId(userId)
                    : behaviorRepository.findTopCategoriesBySessionId(sessionId);

            List<Integer> topCategoryIds = catRows.stream()
                    .limit(TOP_CATEGORIES)
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            List<String> topCategoryNames = topCategoryIds.stream()
                    .map(id -> categoryRepository.findById(id)
                            .map(c -> c.getCategoryName()).orElse(""))
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.toList());

            profile.setPreferredCategoryIds(topCategoryIds.stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
            profile.setPreferredCategoryNames(String.join(", ", topCategoryNames));

            // ── Average spend & price sensitivity ────────────────────
            Double avg = userId != null
                    ? behaviorRepository.findAvgPriceByUserId(userId)
                    : behaviorRepository.findAvgPriceBySessionId(sessionId);

            if (avg != null) {
                profile.setAvgSpend(BigDecimal.valueOf(avg));
                profile.setPriceSensitivity(computeSensitivity(avg));
            }

            // ── Counts ─────────────────────────────────────────────
            long views = userId != null
                    ? behaviorRepository.countByUserId(userId)
                    : behaviorRepository.countBySessionId(sessionId);
            long purchases = userId != null
                    ? behaviorRepository.countPurchasesByUserId(userId) : 0L;

            profile.setTotalViews((int) views);
            profile.setTotalPurchases((int) purchases);

            profileRepository.save(profile);
        } catch (Exception e) {
            log.warn("[Behavior] Profile recompute failed: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Get profile as DTO for AI prompt injection
    // ------------------------------------------------------------------ //

    public UserProfileDto getProfileDto(Integer userId, String sessionId) {
        Optional<UserProfile> opt = userId != null
                ? profileRepository.findByUserId(userId)
                : Optional.empty();
        if (opt.isEmpty() && sessionId != null) {
            opt = profileRepository.findBySessionId(sessionId);
        }
        return opt.map(this::toDto).orElse(UserProfileDto.builder().build());
    }

    /**
     * Append a detected color or style keyword to the stored profile.
     * Called by AiChatService when it detects preferences from the conversation.
     */
    @Transactional
    public void mergeStyleHints(Integer userId, String sessionId, String color, String style) {
        Optional<UserProfile> opt = userId != null
                ? profileRepository.findByUserId(userId)
                : profileRepository.findBySessionId(sessionId);

        if (opt.isEmpty()) return;
        UserProfile profile = opt.get();

        if (color != null && !color.isBlank()) {
            profile.setPreferredColors(mergeTokens(profile.getPreferredColors(), color, TOP_COLORS));
        }
        if (style != null && !style.isBlank()) {
            profile.setStyleKeywords(mergeTokens(profile.getStyleKeywords(), style, 5));
        }
        profileRepository.save(profile);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private UserProfile loadOrCreate(Integer userId, String sessionId) {
        if (userId != null) {
            return profileRepository.findByUserId(userId).orElseGet(() ->
                    UserProfile.builder().userId(userId).sessionId(sessionId).build());
        }
        return profileRepository.findBySessionId(sessionId).orElseGet(() ->
                UserProfile.builder().sessionId(sessionId).build());
    }

    private String computeSensitivity(double avg) {
        if (avg < 300_000) return "low";
        if (avg < 800_000) return "mid";
        return "high";
    }

    /**
     * Merges a new token into a comma-separated list, placing it at the front
     * and capping the list at maxCount items.
     */
    private String mergeTokens(String existing, String newToken, int maxCount) {
        List<String> tokens = new ArrayList<>();
        tokens.add(newToken.trim());
        if (existing != null && !existing.isBlank()) {
            Arrays.stream(existing.split(","))
                    .map(String::trim)
                    .filter(t -> !t.equalsIgnoreCase(newToken.trim()))
                    .forEach(tokens::add);
        }
        return tokens.stream().limit(maxCount).collect(Collectors.joining(", "));
    }

    private UserProfileDto toDto(UserProfile p) {
        return UserProfileDto.builder()
                .preferredCategoryIds(p.getPreferredCategoryIds())
                .preferredCategoryNames(p.getPreferredCategoryNames())
                .preferredColors(p.getPreferredColors())
                .priceSensitivity(p.getPriceSensitivity())
                .avgSpend(p.getAvgSpend())
                .styleKeywords(p.getStyleKeywords())
                .totalViews(p.getTotalViews())
                .totalPurchases(p.getTotalPurchases())
                .build();
    }
}
