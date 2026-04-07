package com.leika.shop.specification;

import com.leika.shop.dto.ProductSearchRequest;
import com.leika.shop.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Product> hasCategory(Integer categoryId) {
        return (root, query, cb) ->
                cb.or(
                    cb.equal(root.get("category").get("categoryId"), categoryId),
                    cb.equal(root.get("category").get("parent").get("categoryId"), categoryId)
                );
    }

    public static Specification<Product> hasBrand(Integer brandId) {
        return (root, query, cb) ->
                cb.equal(root.get("brand").get("brandId"), brandId);
    }

    public static Specification<Product> priceGreaterThanOrEqual(java.math.BigDecimal minPrice) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(
                        cb.coalesce(root.get("salePrice"), root.get("basePrice")),
                        minPrice
                );
    }

    public static Specification<Product> priceLessThanOrEqual(java.math.BigDecimal maxPrice) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(
                        cb.coalesce(root.get("salePrice"), root.get("basePrice")),
                        maxPrice
                );
    }

    /**
     * Builds a combined Specification from a ProductSearchRequest.
     * Only active products are returned. Other filters are applied only when non-null.
     */
    public static Specification<Product> buildFromRequest(ProductSearchRequest request) {
        Specification<Product> spec = Specification.where(isActive());

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            spec = spec.and(hasKeyword(request.getKeyword()));
        }
        if (request.getCategoryId() != null) {
            spec = spec.and(hasCategory(request.getCategoryId()));
        }
        if (request.getBrandId() != null) {
            spec = spec.and(hasBrand(request.getBrandId()));
        }
        if (request.getMinPrice() != null) {
            spec = spec.and(priceGreaterThanOrEqual(request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            spec = spec.and(priceLessThanOrEqual(request.getMaxPrice()));
        }

        return spec;
    }
}
