package com.leika.shop.repository;

import com.leika.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findByIsActiveTrue();

    Optional<Product> findBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId OR p.category.parent.categoryId = :categoryId) " +
           "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR COALESCE(p.salePrice, p.basePrice) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR COALESCE(p.salePrice, p.basePrice) <= :maxPrice) " +
           "AND (:onSale = false OR (p.salePrice IS NOT NULL AND p.salePrice < p.basePrice))")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("brandId") Integer brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("onSale") boolean onSale,
            Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.salePrice IS NOT NULL AND p.salePrice < p.basePrice ORDER BY p.createdAt DESC")
    List<Product> findSaleProducts();

    List<Product> findByCategoryCategoryIdAndIsActiveTrue(Integer categoryId);
    
    long countByIsActiveTrue();

    @Query("SELECT p FROM Product p LEFT JOIN ProductVariant pv ON pv.product = p " +
           "LEFT JOIN OrderDetail od ON od.productVariant = pv " +
           "WHERE p.isActive = true " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId OR p.category.parent.categoryId = :categoryId) " +
           "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR COALESCE(p.salePrice, p.basePrice) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR COALESCE(p.salePrice, p.basePrice) <= :maxPrice) " +
           "GROUP BY p.productId, p.productName, p.slug, p.description, p.basePrice, p.salePrice, " +
           "p.thumbnailUrl, p.isFeatured, p.isActive, p.createdAt, p.updatedAt, p.category, p.brand " +
           "ORDER BY COALESCE(SUM(od.quantity), 0) DESC")
    Page<Product> findBestSellers(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("brandId") Integer brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
