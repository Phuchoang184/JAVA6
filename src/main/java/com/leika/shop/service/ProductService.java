package com.leika.shop.service;

import com.leika.shop.dto.*;
import com.leika.shop.entity.*;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.*;
import com.leika.shop.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public Page<ProductDto> searchProducts(String keyword, Integer categoryId, Integer brandId,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           String sortBy, int page, int size) {
        String effectiveSort = sortBy != null ? sortBy : "newest";

        // Bestseller uses a special query with ORDER BY SUM(quantity)
        if ("bestseller".equals(effectiveSort)) {
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> products = productRepository.findBestSellers(
                    keyword, categoryId, brandId, minPrice, maxPrice, pageable);
            return products.map(this::toDto);
        }

        boolean onSale = "sale".equals(effectiveSort);
        Sort sort = switch (effectiveSort) {
            case "price-asc" -> Sort.by("basePrice").ascending();
            case "price-desc" -> Sort.by("basePrice").descending();
            case "name-asc" -> Sort.by("productName").ascending();
            case "name-desc" -> Sort.by("productName").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productRepository.searchProducts(
                keyword, categoryId, brandId, minPrice, maxPrice, onSale, pageable);

        return products.map(this::toDto);
    }

    /**
     * Advanced search using JPA Specification for dynamic query composition.
     */
    public Page<ProductDto> advancedSearch(ProductSearchRequest request) {
        Sort sort = switch (request.getSortBy() != null ? request.getSortBy() : "newest") {
            case "price-asc" -> Sort.by("basePrice").ascending();
            case "price-desc" -> Sort.by("basePrice").descending();
            case "name-asc" -> Sort.by("productName").ascending();
            case "name-desc" -> Sort.by("productName").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Specification<Product> spec = ProductSpecification.buildFromRequest(request);
        Page<Product> products = productRepository.findAll(spec, pageable);

        return products.map(this::toDto);
    }

    public ProductDto getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndIsActiveTrue(id);
        return toDto(product, variants, true);
    }

    public ProductDto getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndIsActiveTrue(product.getProductId());
        return toDto(product, variants, true);
    }

    public List<ProductDto> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ProductDto> getSaleProducts() {
        return productRepository.findSaleProducts()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ProductDto> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return productRepository.findByIsActiveTrue(pageable)
                .getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CategoryDto> getAllCategories() {
        List<Category> roots = categoryRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrder();
        return roots.stream().map(this::toCategoryDto).collect(Collectors.toList());
    }

    public List<BrandDto> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByBrandName()
                .stream().map(b -> BrandDto.builder()
                        .brandId(b.getBrandId())
                        .brandName(b.getBrandName())
                        .logoUrl(b.getLogoUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // ---- Mapping helpers ----

    private ProductDto toDto(Product p) {
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndIsActiveTrue(p.getProductId());
        return toDto(p, variants, false);
    }

    private ProductDto toDto(Product p, List<ProductVariant> variants, boolean includeVariants) {
        List<ProductVariant> sortedVariants = variants.stream()
            .sorted(Comparator
                .comparingInt(this::getVariantSortRank)
                .thenComparing(variant -> extractVariantLabel(variant.getSku())))
            .collect(Collectors.toList());

        ProductVariant defaultVariant = sortedVariants.stream()
            .filter(variant -> variant.getStockQty() > 0)
            .findFirst()
            .orElse(sortedVariants.stream().findFirst().orElse(null));

        String displayImageUrl = sortedVariants.stream()
                .map(ProductVariant::getImageUrl)
                .filter(Objects::nonNull)
                .filter(imageUrl -> !imageUrl.isBlank())
                .findFirst()
                .orElse(p.getThumbnailUrl());

        ProductDto.ProductDtoBuilder builder = ProductDto.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .basePrice(p.getBasePrice())
                .salePrice(p.getSalePrice())
                .thumbnailUrl(p.getThumbnailUrl())
                .isFeatured(p.isFeatured())
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getBrandName() : null)
                .brandId(p.getBrand() != null ? p.getBrand().getBrandId() : null)
                .displayImageUrl(displayImageUrl)
                .defaultVariantId(defaultVariant != null ? defaultVariant.getVariantId() : null)
                .totalStockQty(sortedVariants.stream().mapToInt(ProductVariant::getStockQty).sum());

        if (includeVariants) {
            builder.variants(sortedVariants.stream().map(this::toVariantDto).collect(Collectors.toList()));
        }

        return builder.build();
    }

    private VariantDto toVariantDto(ProductVariant v) {
        return VariantDto.builder()
                .variantId(v.getVariantId())
                .sku(v.getSku())
                .displayLabel(extractVariantLabel(v.getSku()))
                .stockQty(v.getStockQty())
                .extraPrice(v.getExtraPrice())
                .imageUrl(v.getImageUrl())
                .build();
    }

    private String extractVariantLabel(String sku) {
        if (sku == null || sku.isBlank()) {
            return "Mac dinh";
        }

        String suffix = sku.contains("-")
                ? sku.substring(sku.lastIndexOf('-') + 1)
                : sku;
        String normalized = suffix.trim().toUpperCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return "Mac dinh";
        }

        return switch (normalized) {
            case "ONE" -> "ONE SIZE";
            default -> normalized;
        };
    }

    private int getVariantSortRank(ProductVariant variant) {
        String label = extractVariantLabel(variant.getSku()).toUpperCase(Locale.ROOT);
        return switch (label) {
            case "XS" -> 10;
            case "S" -> 20;
            case "M" -> 30;
            case "L" -> 40;
            case "XL" -> 50;
            case "XXL" -> 60;
            case "XXXL" -> 70;
            case "ONE SIZE" -> 80;
            default -> label.matches("\\d+") ? 100 + Integer.parseInt(label) : 500;
        };
    }

    private CategoryDto toCategoryDto(Category c) {
        return CategoryDto.builder()
                .categoryId(c.getCategoryId())
                .categoryName(c.getCategoryName())
                .slug(c.getSlug())
                .imageUrl(c.getImageUrl())
                .subCategories(c.getSubCategories() != null
                        ? c.getSubCategories().stream()
                            .filter(Category::isActive)
                            .map(this::toCategoryDto)
                            .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
