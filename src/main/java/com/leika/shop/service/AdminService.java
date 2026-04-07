package com.leika.shop.service;

import com.leika.shop.dto.*;
import com.leika.shop.entity.*;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    // ====================================================================
    //  PRODUCT CRUD
    // ====================================================================

    public Page<ProductDto> listProducts(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products;
        if (keyword != null && !keyword.isBlank()) {
            products = productRepository.searchProducts(keyword, null, null, null, null, false, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::toProductDto);
    }

    public ProductDto getProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        ProductDto dto = toProductDto(product);
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndIsActiveTrue(id);
        dto.setVariants(variants.stream().map(this::toVariantDto).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public ProductDto createProduct(AdminProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu"));
        }

        Product product = Product.builder()
                .productName(request.getProductName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .thumbnailUrl(request.getThumbnailUrl())
                .category(category)
                .brand(brand)
                .isFeatured(request.isFeatured())
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // Create variants if provided
        if (request.getVariants() != null) {
            for (AdminProductRequest.VariantRequest vReq : request.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(vReq.getSku())
                        .stockQty(vReq.getStockQty())
                        .extraPrice(vReq.getExtraPrice() != null ? vReq.getExtraPrice() : BigDecimal.ZERO)
                        .imageUrl(vReq.getImageUrl())
                        .isActive(true)
                        .build();
                variantRepository.save(variant);
            }
        }

        return getProduct(product.getProductId());
    }

    @Transactional
    public ProductDto updateProduct(Integer id, AdminProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu"));
        }

        product.setProductName(request.getProductName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setFeatured(request.isFeatured());

        productRepository.save(product);

        // Update variants if provided
        if (request.getVariants() != null) {
            for (AdminProductRequest.VariantRequest vReq : request.getVariants()) {
                if (vReq.getVariantId() != null) {
                    // Update existing variant
                    ProductVariant variant = variantRepository.findById(vReq.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể"));
                    variant.setSku(vReq.getSku());
                    variant.setStockQty(vReq.getStockQty());
                    variant.setExtraPrice(vReq.getExtraPrice() != null ? vReq.getExtraPrice() : BigDecimal.ZERO);
                    variant.setImageUrl(vReq.getImageUrl());
                    variantRepository.save(variant);
                } else {
                    // Create new variant
                    ProductVariant variant = ProductVariant.builder()
                            .product(product)
                            .sku(vReq.getSku())
                            .stockQty(vReq.getStockQty())
                            .extraPrice(vReq.getExtraPrice() != null ? vReq.getExtraPrice() : BigDecimal.ZERO)
                            .imageUrl(vReq.getImageUrl())
                            .isActive(true)
                            .build();
                    variantRepository.save(variant);
                }
            }
        }

        return getProduct(id);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        // Soft delete
        product.setActive(false);
        productRepository.save(product);

        // Soft delete all variants
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndIsActiveTrue(id);
        variants.forEach(v -> {
            v.setActive(false);
            variantRepository.save(v);
        });
    }

    // ====================================================================
    //  USER MANAGEMENT (Admin)
    // ====================================================================

    public Page<UserDto> listUsers(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    keyword, keyword, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(userService::toDto);
    }

    public UserDto getUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return userService.toDto(user);
    }

    @Transactional
    public UserDto updateUserRole(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò: " + roleName));

        user.setRole(role);
        userRepository.save(user);
        return userService.toDto(user);
    }

    @Transactional
    public UserDto toggleUserActive(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return userService.toDto(user);
    }

    // ====================================================================
    //  Mapping Helpers
    // ====================================================================

    private ProductDto toProductDto(Product p) {
        return ProductDto.builder()
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
                .build();
    }

    private VariantDto toVariantDto(ProductVariant v) {
        return VariantDto.builder()
                .variantId(v.getVariantId())
                .sku(v.getSku())
                .stockQty(v.getStockQty())
                .extraPrice(v.getExtraPrice())
                .imageUrl(v.getImageUrl())
                .build();
    }
}
