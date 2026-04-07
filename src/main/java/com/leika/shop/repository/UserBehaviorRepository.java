package com.leika.shop.repository;

import com.leika.shop.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    List<UserBehavior> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<UserBehavior> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /** Most-interacted category IDs for a registered user */
    @Query("SELECT b.categoryId, COUNT(b) as cnt FROM UserBehavior b " +
           "WHERE b.userId = :userId AND b.categoryId IS NOT NULL " +
           "GROUP BY b.categoryId ORDER BY cnt DESC")
    List<Object[]> findTopCategoriesByUserId(@Param("userId") Integer userId);

    /** Most-interacted category IDs for a guest session */
    @Query("SELECT b.categoryId, COUNT(b) as cnt FROM UserBehavior b " +
           "WHERE b.sessionId = :sessionId AND b.categoryId IS NOT NULL " +
           "GROUP BY b.categoryId ORDER BY cnt DESC")
    List<Object[]> findTopCategoriesBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT AVG(b.priceAtAction) FROM UserBehavior b " +
           "WHERE b.userId = :userId AND b.priceAtAction IS NOT NULL")
    Double findAvgPriceByUserId(@Param("userId") Integer userId);

    @Query("SELECT AVG(b.priceAtAction) FROM UserBehavior b " +
           "WHERE b.sessionId = :sessionId AND b.priceAtAction IS NOT NULL")
    Double findAvgPriceBySessionId(@Param("sessionId") String sessionId);

    long countByUserId(Integer userId);

    long countBySessionId(String sessionId);

    @Query("SELECT COUNT(b) FROM UserBehavior b WHERE b.userId = :userId AND b.actionType = 'PURCHASE'")
    long countPurchasesByUserId(@Param("userId") Integer userId);
}
