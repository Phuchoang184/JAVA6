package com.leika.shop.repository;

import com.leika.shop.entity.Cart;
import com.leika.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findBySessionId(String sessionId);
    Optional<Cart> findByUserUserId(Integer userId);
}
