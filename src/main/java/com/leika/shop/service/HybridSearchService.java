package com.leika.shop.service;

import com.leika.shop.dto.ChatProductCard;
import com.leika.shop.dto.UserProfileDto;
import com.leika.shop.entity.Product;
import com.leika.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search service combining:
 *  1. SQL exact/keyword search
 *  2. Vietnamese fashion semantic expansion (ontology-based synonym map)
 *  3. User-preference scoring (category affinity + price sensitivity)
 *  4. Intent-aware ranking (BUYING → boost sale items; OUTFIT → fetch complementary)
 *
 * No external vector database required — runs entirely in-process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int SQL_POOL_SIZE = 20; // candidates per SQL pass
    private static final int TOP_K = 8;           // final result size

    private final ProductRepository productRepository;

    // ── Vietnamese Fashion Ontology ─────────────────────────────────────
    // Keys are user query fragments; values are expanded search terms sent to SQL.
    private static final Map<String, List<String>> FASHION_ONTOLOGY = new LinkedHashMap<>() {{
        put("đi tiệc",      List.of("tiệc", "dạ hội", "cocktail", "đầm", "váy", "sang trọng"));
        put("công sở",      List.of("blazer", "áo sơ mi", "quần tây", "thanh lịch", "lịch sự"));
        put("dạo phố",      List.of("casual", "thoải mái", "áo thun", "quần jean", "everyday"));
        put("mùa hè",       List.of("đầm", "váy ngắn", "crop top", "short", "mát", "nhẹ"));
        put("mùa đông",     List.of("áo khoác", "len", "cardigan", "coat", "ấm", "dày"));
        put("váy",          List.of("đầm", "chân váy", "mini", "midi", "maxi", "body", "flare"));
        put("đầm",          List.of("váy", "chân váy", "dress", "midi", "maxi", "mini"));
        put("áo",           List.of("blouse", "top", "áo kiểu", "áo thun", "crop", "sơ mi"));
        put("quần",         List.of("quần tây", "jeans", "legging", "short", "culottes"));
        put("đen",          List.of("black", "tối màu", "đen tuyền"));
        put("trắng",        List.of("white", "be", "kem", "nude"));
        put("đỏ",           List.of("đỏ đô", "đỏ tươi", "burgundy", "wine", "red"));
        put("bán chạy",     List.of("hot", "bestseller", "phổ biến", "yêu thích", "trend"));
        put("mới",          List.of("new arrival", "mới về", "latest", "trending", "vừa ra"));
        put("sale",         List.of("giảm giá", "khuyến mãi", "ưu đãi", "discount", "off"));
        put("sexy",         List.of("gợi cảm", "body", "cut-out", "hở lưng", "quyến rũ"));
        put("thanh lịch",   List.of("elegant", "tinh tế", "minimalist", "thanh tao", "sang"));
        put("phụ kiện",     List.of("túi", "thắt lưng", "khăn", "mũ", "giày", "belt"));
        put("dễ thương",    List.of("cute", "lolita", "pastel", "kẹo", "bánh bèo", "hồng"));
        put("cá tính",      List.of("streetwear", "oversized", "graphic", "bold", "edgy"));
        put("vintage",      List.of("retro", "cổ điển", "thập niên", "boho", "bohemian"));
        put("thể thao",     List.of("sporty", "active", "gym", "legging", "áo thun"));
        put("đi biển",      List.of("bikini", "đồ tắm", "váy maxi", "resort", "hè"));
        put("outfit",       List.of("set", "combo", "trọn bộ", "áo", "quần", "váy"));
    }};

    // ── Price band boundaries ───────────────────────────────────────────
    private static final BigDecimal PRICE_LOW_MAX  = BigDecimal.valueOf(300_000);
    private static final BigDecimal PRICE_HIGH_MIN = BigDecimal.valueOf(800_000);

    // ── Public search API ───────────────────────────────────────────────

    /**
     * Run hybrid search.
     *
     * @param rawQuery         user's message / AI-extracted query string
     * @param userProfile      computed preference profile (may be empty DTO)
     * @param contextProductId current product page (nullable)
     * @param intent           detected intent: BUYING | BROWSING | OUTFIT | SUPPORT
     * @return ranked list of up to TOP_K product cards
     */
    public List<ChatProductCard> search(String rawQuery, UserProfileDto userProfile,
                                        Integer contextProductId, String intent) {
        if (rawQuery == null) rawQuery = "";
        String query = rawQuery.toLowerCase(new Locale("vi"));

        // ── Step 1: expand query via fashion ontology ─────────────────
        Set<String> expandedTerms = expandQuery(query);
        expandedTerms.add(query); // always include original

        // ── Step 2: collect candidate products via multiple SQL passes ─
        Map<Integer, ScoredProduct> candidates = new LinkedHashMap<>();

        for (String term : expandedTerms) {
            if (term.isBlank()) continue;
            try {
                boolean isSaleQuery = query.contains("sale") || query.contains("giảm") || query.contains("khuyến mãi");
                productRepository.searchProducts(
                        term, null, null, null, null, isSaleQuery,
                        PageRequest.of(0, SQL_POOL_SIZE, Sort.by("createdAt").descending())
                ).forEach(p -> candidates.computeIfAbsent(p.getProductId(), id -> new ScoredProduct(p)));
            } catch (Exception e) {
                log.debug("[HybridSearch] SQL pass failed for term '{}': {}", term, e.getMessage());
            }
        }

        // ── Step 3: category preference pass ─────────────────────────
        if (userProfile.getPreferredCategoryIds() != null
                && !userProfile.getPreferredCategoryIds().isBlank()) {
            Arrays.stream(userProfile.getPreferredCategoryIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(catIdStr -> {
                        try {
                            int catId = Integer.parseInt(catIdStr);
                            productRepository.searchProducts(
                                    null, catId, null, null, null, false,
                                    PageRequest.of(0, 10, Sort.by("createdAt").descending())
                            ).forEach(p -> candidates.computeIfAbsent(p.getProductId(), id -> new ScoredProduct(p)));
                        } catch (Exception ignored) {}
                    });
        }

        // Fallback: load recent products when no candidates found
        if (candidates.isEmpty()) {
            productRepository.findByIsActiveTrue(
                    PageRequest.of(0, SQL_POOL_SIZE, Sort.by("createdAt").descending())
            ).forEach(p -> candidates.put(p.getProductId(), new ScoredProduct(p)));
        }

        // ── Step 4: score every candidate ─────────────────────────────
        for (ScoredProduct sp : candidates.values()) {
            sp.score = score(sp.product, query, expandedTerms, userProfile, intent);
        }

        // ── Step 5: OUTFIT mode – ensure category diversity ───────────
        List<ChatProductCard> results;
        if ("OUTFIT".equals(intent)) {
            results = buildOutfitSet(candidates, TOP_K);
        } else {
            results = candidates.values().stream()
                    .sorted(Comparator.comparingDouble((ScoredProduct sp) -> sp.score).reversed())
                    .limit(TOP_K)
                    .map(sp -> toCard(sp.product))
                    .collect(Collectors.toList());
        }

        log.debug("[HybridSearch] query='{}' candidates={} results={}", rawQuery, candidates.size(), results.size());
        return results;
    }

    // ── Scoring ─────────────────────────────────────────────────────────

    private double score(Product p, String query, Set<String> expandedTerms,
                         UserProfileDto profile, String intent) {
        double score = 0;
        String pName     = p.getProductName().toLowerCase(new Locale("vi"));
        String pCategory = p.getCategory() != null
                ? p.getCategory().getCategoryName().toLowerCase(new Locale("vi")) : "";

        // Exact name match
        if (query.length() > 2 && pName.contains(query))
            score += 5;

        // Expanded term matches in name / category
        for (String term : expandedTerms) {
            if (term.length() > 2) {
                if (pName.contains(term)) score += 2;
                if (pCategory.contains(term)) score += 1.5;
            }
        }

        // Preferred category boost
        if (profile.getPreferredCategoryIds() != null && p.getCategory() != null) {
            String catIdStr = String.valueOf(p.getCategory().getCategoryId());
            if (Arrays.asList(profile.getPreferredCategoryIds().split(",")).contains(catIdStr)) {
                score += 3;
            }
        }

        // Price sensitivity alignment
        BigDecimal effectivePrice = p.getSalePrice() != null ? p.getSalePrice() : p.getBasePrice();
        if (profile.getPriceSensitivity() != null && effectivePrice != null) {
            switch (profile.getPriceSensitivity()) {
                case "low"  -> { if (effectivePrice.compareTo(PRICE_LOW_MAX)  <= 0) score += 2; }
                case "high" -> { if (effectivePrice.compareTo(PRICE_HIGH_MIN) >= 0) score += 2; }
                case "mid"  -> {
                    if (effectivePrice.compareTo(PRICE_LOW_MAX) > 0
                            && effectivePrice.compareTo(PRICE_HIGH_MIN) < 0) score += 2;
                }
            }
        }

        // Sale item boost for BUYING intent
        if ("BUYING".equals(intent) && p.getSalePrice() != null) score += 2;

        // Featured product boost
        if (p.isFeatured()) score += 1;

        return score;
    }

    // ── Outfit diversity builder ─────────────────────────────────────────

    /**
     * For OUTFIT queries: aim to return products from different categories
     * (top + bottom/dress + accessory) so the user sees a complete look.
     */
    private List<ChatProductCard> buildOutfitSet(Map<Integer, ScoredProduct> candidates, int limit) {
        List<ScoredProduct> sorted = candidates.values().stream()
                .sorted(Comparator.comparingDouble((ScoredProduct sp) -> sp.score).reversed())
                .collect(Collectors.toList());

        Map<Integer, ScoredProduct> byCategory = new LinkedHashMap<>();
        List<ScoredProduct> overflow = new ArrayList<>();

        for (ScoredProduct sp : sorted) {
            Integer catId = sp.product.getCategory() != null
                    ? sp.product.getCategory().getCategoryId() : -1;
            if (!byCategory.containsKey(catId)) {
                byCategory.put(catId, sp);
            } else {
                overflow.add(sp);
            }
        }

        List<ScoredProduct> result = new ArrayList<>(byCategory.values());
        if (result.size() < limit) {
            for (ScoredProduct sp : overflow) {
                if (result.size() >= limit) break;
                result.add(sp);
            }
        }

        return result.stream().limit(limit).map(sp -> toCard(sp.product)).collect(Collectors.toList());
    }

    // ── Query expansion ─────────────────────────────────────────────────

    private Set<String> expandQuery(String query) {
        Set<String> expanded = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : FASHION_ONTOLOGY.entrySet()) {
            if (query.contains(entry.getKey())) {
                expanded.addAll(entry.getValue());
            }
        }
        return expanded;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private ChatProductCard toCard(Product p) {
        int discount = 0;
        if (p.getSalePrice() != null && p.getBasePrice() != null
                && p.getBasePrice().compareTo(BigDecimal.ZERO) > 0) {
            discount = p.getBasePrice().subtract(p.getSalePrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(p.getBasePrice(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return ChatProductCard.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .thumbnailUrl(p.getThumbnailUrl())
                .basePrice(p.getBasePrice())
                .salePrice(p.getSalePrice())
                .slug(p.getSlug())
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : "")
                .discountPercent(discount)
                .build();
    }

    // ── Internal scoring container ───────────────────────────────────────

    private static class ScoredProduct {
        final Product product;
        double score = 0;
        ScoredProduct(Product p) { this.product = p; }
    }
}
