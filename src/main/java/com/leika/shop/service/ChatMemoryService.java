package com.leika.shop.service;

import com.leika.shop.entity.ChatHistory;
import com.leika.shop.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    /** How many most-recent messages to return for prompt context */
    private static final int CONTEXT_WINDOW = 10;

    /** Above this threshold we prepend a compact summary note */
    private static final int SUMMARY_THRESHOLD = 30;

    private final ChatHistoryRepository chatHistoryRepository;

    // ------------------------------------------------------------------ //
    //  Save individual message
    // ------------------------------------------------------------------ //

    @Transactional
    public void save(Integer userId, String sessionId, String conversationId,
                     String role, String content, String intent) {
        if (conversationId == null || conversationId.isBlank()) return;
        chatHistoryRepository.save(ChatHistory.builder()
                .userId(userId)
                .sessionId(sessionId)
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .intent(intent)
                .build());
    }

    // ------------------------------------------------------------------ //
    //  Load chat context for prompt injection
    // ------------------------------------------------------------------ //

    /**
     * Returns the last {@code CONTEXT_WINDOW} messages in chronological order.
     * Each map uses OpenAI's "role" / "content" keys.
     */
    public List<Map<String, String>> getContextMessages(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return Collections.emptyList();

        // Repository returns newest-first; we reverse to get chronological order
        List<ChatHistory> rows = chatHistoryRepository.findTopByConversationId(
                conversationId, PageRequest.of(0, CONTEXT_WINDOW));

        Collections.reverse(rows);

        return rows.stream()
                .map(h -> Map.of("role", h.getRole(), "content", h.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * Compact summary note prepended to the system prompt when the
     * conversation is long. Returns empty string when not needed.
     */
    public String getLongTermSummaryNote(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "";

        long total = chatHistoryRepository.countByConversationId(conversationId);
        if (total < SUMMARY_THRESHOLD) return "";

        // Pull oldest 10 messages for summarisation context
        List<ChatHistory> oldest = chatHistoryRepository.findTopByConversationId(
                conversationId, PageRequest.of(0, 10));
        Collections.reverse(oldest);

        String preview = oldest.stream()
                .map(h -> h.getRole() + ": " + h.getContent())
                .collect(Collectors.joining("\n"));

        return "[ĐÂY LÀ CUỘC TRÒ CHUYỆN DÀI. Tóm tắt đầu hội thoại:\n" + preview + "]\n";
    }

    // ------------------------------------------------------------------ //
    //  Clear conversation (used by "clear history" button)
    // ------------------------------------------------------------------ //

    @Transactional
    public void clear(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            chatHistoryRepository.deleteByConversationId(conversationId);
        }
    }
}
