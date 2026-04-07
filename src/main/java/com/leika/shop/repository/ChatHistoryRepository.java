package com.leika.shop.repository;

import com.leika.shop.entity.ChatHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    /** Most-recent messages first — caller must reverse for chronological order */
    @Query("SELECT c FROM ChatHistory c WHERE c.conversationId = :convId " +
           "ORDER BY c.createdAt DESC")
    List<ChatHistory> findTopByConversationId(
            @Param("convId") String conversationId, Pageable pageable);

    long countByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}
