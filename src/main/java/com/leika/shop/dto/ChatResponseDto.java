package com.leika.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Structured response returned from the AI chat endpoint.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    /** Main reply text in Vietnamese */
    private String message;

    /** Product cards to display inside the chat bubble (may be null/empty) */
    private List<ChatProductCard> products;

    /** Quick-reply suggestion chips for the next user turn */
    private List<String> suggestions;
}
