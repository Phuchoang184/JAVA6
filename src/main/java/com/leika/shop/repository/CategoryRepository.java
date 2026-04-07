package com.leika.shop.repository;

import com.leika.shop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByIsActiveTrueOrderBySortOrder();
    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrder();
    List<Category> findByParentCategoryIdAndIsActiveTrueOrderBySortOrder(Integer parentId);
    Optional<Category> findBySlug(String slug);
}
