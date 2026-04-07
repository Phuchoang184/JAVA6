package com.leika.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leika.shop.dto.*;
import com.leika.shop.entity.Product;
import com.leika.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced AI Chat Service
 * Flow: user message → fetch user profile → load chat history →
 *       detect intent → hybrid search → build prompt → call OpenAI →
 *       parse response → save to memory → update behavior hints
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${openai.base.url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    private final RestTemplate              restTemplate;
    private final ProductRepository         productRepository;
    private final ObjectMapper              objectMapper;
    private final BehaviorTrackingService   behaviorService;
    private final ChatMemoryService         memoryService;
    private final HybridSearchService       hybridSearch;

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    public ChatResponseDto chat(Integer userId, ChatRequest request) {
        String sessionId       = request.getSessionId();
        String conversationId  = request.getConversationId();
        String userMessage     = request.getMessage();

        // ── 1. Fetch user profile ──────────────────────────────────────
        UserProfileDto profile = behaviorService.getProfileDto(userId, sessionId);

        // ── 2. Load DB chat history (with localStorage fallback) ──────
        List<Map<String, String>> dbHistory = conversationId != null
                ? memoryService.getContextMessages(conversationId)
                : Collections.emptyList();
        String longTermNote = conversationId != null
                ? memoryService.getLongTermSummaryNote(conversationId) : "";

        List<Map<String, String>> history = (!dbHistory.isEmpty())
                ? dbHistory
                : (request.getHistory() != null ? request.getHistory() : Collections.emptyList());

        // ── 3. Persist user message ────────────────────────────────────
        if (conversationId != null && sessionId != null) {
            memoryService.save(userId, sessionId, conversationId, "user", userMessage, null);
        }

        // ── 4. Detect intent ──────────────────────────────────────────
        String intent = detectIntent(userMessage, history);

        // ── 5. Hybrid product search ──────────────────────────────────
        List<ChatProductCard> products = hybridSearch.search(
                userMessage, profile, request.getCurrentProductId(), intent);

        // ── 6. AI call ────────────────────────────────────────────────
        boolean hasKey = openAiApiKey != null && !openAiApiKey.isBlank()
                && !openAiApiKey.equals("YOUR_OPENAI_API_KEY_HERE");

        if (!hasKey) {
            ChatResponseDto offline = offlineReply(userMessage, profile, products);
            if (conversationId != null && sessionId != null) {
                memoryService.save(userId, sessionId, conversationId,
                        "assistant", offline.getMessage(), intent);
            }
            return offline;
        }

        String aiReply;
        List<String> suggestions;

        try {
            String productCatalog = buildProductCatalogSnippet(products);
            List<Map<String, String>> messages = buildMessages(
                    userMessage, profile, history, productCatalog,
                    longTermNote, request.getCurrentProductId(), intent);

            String rawJson = callOpenAI(messages);
            JsonNode root  = parseJsonSafe(rawJson);

            aiReply     = root.path("message").asText(null);
            if (aiReply == null || aiReply.isBlank())
                aiReply = "Tôi có thể giúp gì thêm cho bạn? 💎";

            // Optional AI-generated re-query
            String aiQuery = root.has("query") && !root.get("query").isNull()
                    ? root.get("query").asText(null) : null;
            if (aiQuery != null && !aiQuery.isBlank() && !aiQuery.equalsIgnoreCase(userMessage)) {
                List<ChatProductCard> reSearched = hybridSearch.search(
                        aiQuery, profile, request.getCurrentProductId(), intent);
                if (!reSearched.isEmpty()) products = reSearched;
            }

            suggestions = parseStringArray(root, "suggestions");

            // Persist style hints extracted by AI
            String color = root.has("detectedColor") ? root.get("detectedColor").asText(null) : null;
            String style  = root.has("detectedStyle") ? root.get("detectedStyle").asText(null) : null;
            if (sessionId != null) {
                behaviorService.mergeStyleHints(userId, sessionId, color, style);
            }

        } catch (Exception e) {
            log.warn("[Chat] OpenAI call failed: {}", e.getMessage());
            aiReply     = "Xin lỗi, tôi đang bận một chút 🙏 Bạn thử lại sau nhé!";
            suggestions = defaultSuggestions();
        }

        // ── 7. Save assistant reply ────────────────────────────────────
        if (conversationId != null && sessionId != null) {
            memoryService.save(userId, sessionId, conversationId, "assistant", aiReply, intent);
        }

        // ── 8. Track page view ─────────────────────────────────────────
        if (request.getCurrentProductId() != null && sessionId != null) {
            try { behaviorService.track(userId, sessionId, request.getCurrentProductId(), "VIEW"); }
            catch (Exception ignored) {}
        }

        return ChatResponseDto.builder()
                .message(aiReply)
                .products(products.isEmpty() ? null : products)
                .suggestions(suggestions.isEmpty() ? defaultSuggestions() : suggestions)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Intent detection (rule-based)
    // ------------------------------------------------------------------ //

    private String detectIntent(String message, List<Map<String, String>> history) {
        String lower = message.toLowerCase(new Locale("vi"));
        if (containsAny(lower, "mua", "đặt hàng", "order", "thêm vào giỏ"))      return "BUYING";
        if (containsAny(lower, "outfit", "phối đồ", "combo", "full set", "mix"))  return "OUTFIT";
        if (containsAny(lower, "đổi trả", "giao hàng", "ship", "thanh toán",
                "đơn hàng", "bảo hành", "hoàn tiền", "liên hệ"))                  return "SUPPORT";
        return "BROWSING";
    }

    private boolean containsAny(String text, String... kws) {
        for (String kw : kws) if (text.contains(kw)) return true;
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Build OpenAI messages list
    // ------------------------------------------------------------------ //

    private List<Map<String, String>> buildMessages(
            String userMessage, UserProfileDto profile,
            List<Map<String, String>> history, String productCatalog,
            String longTermNote, Integer contextProductId, String intent) {

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                buildSystemPrompt(profile, productCatalog, longTermNote, intent)));

        int start = Math.max(0, history.size() - 8);
        messages.addAll(history.subList(start, history.size()));

        StringBuilder userCtx = new StringBuilder();
        if (contextProductId != null)
            userCtx.append("[Đang xem sản phẩm ID=").append(contextProductId).append("]\n");
        if ("OUTFIT".equals(intent))
            userCtx.append("[Intent: OUTFIT – gợi ý combo đầy đủ]\n");
        else if ("BUYING".equals(intent))
            userCtx.append("[Intent: BUYING – ưu tiên sản phẩm đang sale]\n");
        userCtx.append(userMessage);
        messages.add(Map.of("role", "user", "content", userCtx.toString()));

        return messages;
    }

    // ------------------------------------------------------------------ //
    //  System prompt builder
    // ------------------------------------------------------------------ //

    private String buildSystemPrompt(UserProfileDto profile, String productCatalog,
                                     String longTermNote, String intent) {
        StringBuilder sb = new StringBuilder();
        sb.append(longTermNote);
        sb.append("""
                Bạn là TRỢ LÝ THỜI TRANG CAO CẤP của thương hiệu LEIKA LUXURY — brand minimalism sang trọng.

                NHIỆM VỤ: Tư vấn cá nhân hoá, gợi ý sản phẩm phù hợp, khuyến khích mua hàng tự nhiên.

                PHONG CÁCH: Tiếng Việt · thân thiện · tinh tế · ngắn gọn · emoji tối đa 2/tin
                QUY TẮC: CHỈ gợi ý sản phẩm có trong DANH SÁCH bên dưới. KHÔNG bịa sản phẩm.

                """);

        sb.append("PROFILE KHÁCH HÀNG:\n");
        if (hasValue(profile.getPreferredCategoryNames()))
            sb.append("- Danh mục yêu thích: ").append(profile.getPreferredCategoryNames()).append("\n");
        if (hasValue(profile.getPreferredColors()))
            sb.append("- Màu yêu thích: ").append(profile.getPreferredColors()).append("\n");
        if (hasValue(profile.getStyleKeywords()))
            sb.append("- Phong cách: ").append(profile.getStyleKeywords()).append("\n");
        if (hasValue(profile.getPriceSensitivity()))
            sb.append("- Ngưỡng giá: ").append(priceLabel(profile.getPriceSensitivity())).append("\n");
        if (profile.getTotalPurchases() > 0)
            sb.append("- Đã mua ").append(profile.getTotalPurchases()).append(" đơn tại LEIKA\n");
        sb.append("\n");

        if ("OUTFIT".equals(intent))
            sb.append("NHIỆM VỤ ĐẶC BIỆT: Gợi ý COMBO trang phục đầy đủ (áo + quần/váy + phụ kiện).\n\n");
        else if ("SUPPORT".equals(intent))
            sb.append("""
                    CHÍNH SÁCH (dùng để trả lời hỗ trợ):
                    - Miễn phí giao hàng ≥500.000đ; nội thành 1–2 ngày, tỉnh 3–5 ngày
                    - Đổi trả 30 ngày, còn tag chưa sử dụng
                    - Thanh toán: VNPAY, COD, chuyển khoản | Hotline: 1900 1234

                    """);
        else if ("BUYING".equals(intent))
            sb.append("NHIỆM VỤ ĐẶC BIỆT: Khách muốn MUA. Ưu tiên sản phẩm sale, tạo urgency nhẹ.\n\n");

        sb.append("DANH SÁCH SẢN PHẨM:\n").append(productCatalog).append("\n\n");

        sb.append("""
                OUTPUT — JSON thuần, KHÔNG có text ngoài:
                {
                  "message": "Nội dung tiếng Việt",
                  "query": "từ khóa tìm sản phẩm hoặc null",
                  "detectedColor": "màu phát hiện hoặc null",
                  "detectedStyle": "phong cách phát hiện hoặc null",
                  "suggestions": ["chip 1", "chip 2", "chip 3"]
                }
                """);
        return sb.toString();
    }

    private boolean hasValue(String s) { return s != null && !s.isBlank(); }

    private String priceLabel(String sensitivity) {
        return switch (sensitivity) {
            case "low"  -> "bình dân (dưới 300K)";
            case "high" -> "cao cấp (trên 800K)";
            default     -> "trung bình (300K–800K)";
        };
    }

    // ------------------------------------------------------------------ //
    //  Compact product catalogue for prompt
    // ------------------------------------------------------------------ //

    private String buildProductCatalogSnippet(List<ChatProductCard> products) {
        if (products.isEmpty()) {
            return productRepository.findByIsActiveTrue(
                    PageRequest.of(0, 20, Sort.by("createdAt").descending())
            ).stream()
                    .map(p -> {
                        BigDecimal price = p.getSalePrice() != null ? p.getSalePrice() : p.getBasePrice();
                        String cat = p.getCategory() != null ? p.getCategory().getCategoryName() : "";
                        return String.format("ID:%d | %s | %s | %,.0fđ",
                                p.getProductId(), p.getProductName(), cat, price);
                    })
                    .collect(Collectors.joining("\n"));
        }
        return products.stream()
                .map(c -> String.format("ID:%d | %s | %s | %,.0fđ%s",
                        c.getProductId(), c.getProductName(), c.getCategoryName(),
                        c.getEffectivePrice(),
                        c.getDiscountPercent() > 0 ? " (-" + c.getDiscountPercent() + "%)" : ""))
                .collect(Collectors.joining("\n"));
    }

    // ------------------------------------------------------------------ //
    //  OpenAI HTTP call
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private String callOpenAI(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", openAiModel);
        body.put("messages", messages);
        body.put("temperature", 0.72);
        body.put("max_tokens", 700);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                openAiBaseUrl + "/chat/completions", HttpMethod.POST, entity, Map.class);

        Map<String, Object> rb = response.getBody();
        if (rb == null) throw new RuntimeException("Empty response from OpenAI");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) rb.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    // ------------------------------------------------------------------ //
    //  Offline / fallback replies
    // ------------------------------------------------------------------ //

    private ChatResponseDto offlineReply(String message, UserProfileDto profile,
                                          List<ChatProductCard> products) {
        String lower = message.toLowerCase(new Locale("vi"));
        String reply;

        if (lower.contains("váy") || lower.contains("đầm"))
            reply = "LEIKA có nhiều mẫu váy đẹp lắm ạ! Từ váy đi làm đến đầm tiệc sang trọng 💃";
        else if (lower.contains("áo"))
            reply = "Áo LEIKA phong cách minimalism rất tinh tế, phù hợp đi làm hoặc dạo phố ✨";
        else if (lower.contains("giao hàng") || lower.contains("ship"))
            reply = "Miễn phí giao hàng đơn từ 500.000đ. Nội thành 1–2 ngày, tỉnh 3–5 ngày 🚚";
        else if (lower.contains("đổi trả") || lower.contains("hoàn"))
            reply = "LEIKA đổi trả trong 30 ngày. Sản phẩm giữ nguyên tag, chưa qua sử dụng 💛";
        else if (lower.contains("sale") || lower.contains("khuyến mãi"))
            reply = "Đang có nhiều ưu đãi! Xem bộ sưu tập SALE để không bỏ lỡ 🎉";
        else if (lower.contains("outfit") || lower.contains("phối đồ"))
            reply = "Để phối đồ đẹp, chọn 1–2 tông màu chính. Bạn đi dịp gì vậy ạ? ✨";
        else if (hasValue(profile.getPreferredCategoryNames()))
            reply = "Dựa trên sở thích của bạn (" + profile.getPreferredCategoryNames() + "), đây là vài gợi ý 💎";
        else
            reply = "Xin chào! Tôi là trợ lý thời trang LEIKA 💎 Tôi tư vấn trang phục, gợi ý outfit và giải đáp mọi thắc mắc. Bạn cần gì ạ?";

        return ChatResponseDto.builder()
                .message(reply)
                .products(products.isEmpty() ? null : products)
                .suggestions(defaultSuggestions())
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private JsonNode parseJsonSafe(String content) {
        try { return objectMapper.readTree(content); }
        catch (Exception e) {
            log.warn("[Chat] JSON parse failed");
            try { return objectMapper.createObjectNode().put("message", content); }
            catch (Exception ex) { return objectMapper.createObjectNode(); }
        }
    }

    private List<String> parseStringArray(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray())
            root.get(field).forEach(n -> result.add(n.asText()));
        return result;
    }

    private List<String> defaultSuggestions() {
        return List.of("Gợi ý outfit", "Sản phẩm bán chạy", "Chính sách đổi trả");
    }
}
