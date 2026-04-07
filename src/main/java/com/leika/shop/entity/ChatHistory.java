package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persistent chat message store.
 * ConversationId groups messages into a single session window.
 */
@Entity
@Table(name = "ChatHistory", indexes = {
        @Index(name = "idx_ch_conv",    columnList = "ConversationId"),
        @Index(name = "idx_ch_user",    columnList = "UserId"),
        @Index(name = "idx_ch_session", columnList = "SessionId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChatId")
    private Long chatId;

    /** Null for guests */
    @Column(name = "UserId")
    private Integer userId;

    @Column(name = "SessionId", length = 100)
    private String sessionId;

    /** Groups a sequence of messages for one conversation window */
    @Column(name = "ConversationId", length = 100, nullable = false)
    private String conversationId;

    /** "user" | "assistant" */
    @Column(name = "Role", length = 20, nullable = false)
    private String role;

    @Column(name = "Content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    /**
     * AI-detected intent from the PREVIOUS user turn.
     * Values: BUYING | BROWSING | SUPPORT | OUTFIT | UNKNOWN
     */
    @Column(name = "Intent", length = 20)
    private String intent;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;
}
