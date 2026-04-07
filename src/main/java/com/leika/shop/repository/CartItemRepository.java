package com.leika.shop.repository;

import com.leika.shop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartCartId(Integer cartId);
    Optional<CartItem> findByCartCartIdAndProductVariantVariantId(Integer cartId, Integer variantId);
    void deleteByCartCartId(Integer cartId);
}
