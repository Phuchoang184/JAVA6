package com.leika.shop.controller;

import com.leika.shop.dto.ApiResponse;
import com.leika.shop.dto.ChatRequest;
import com.leika.shop.dto.ChatResponseDto;
import com.leika.shop.entity.User;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.service.AiChatService;
import com.leika.shop.service.ChatMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService     aiChatService;
    private final ChatMemoryService chatMemoryService;
    private final UserRepository    userRepository;

    /**
     * POST /api/chat
     * Public endpoint — works for both guests (userId=null) and authenticated users.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(@RequestBody ChatRequest request) {
        ChatResponseDto response = aiChatService.chat(resolveUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * DELETE /api/chat/history/{conversationId}
     * Clear server-side conversation memory for a given conversation.
     */
    @DeleteMapping("/history/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> clearHistory(@PathVariable String conversationId) {
        chatMemoryService.clear(conversationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Integer resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName())
                .map(User::getUserId).orElse(null);
    }
}
