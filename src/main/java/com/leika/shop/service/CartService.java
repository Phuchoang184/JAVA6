package com.leika.shop.service;

import com.leika.shop.dto.*;
import com.leika.shop.entity.*;
import com.leika.shop.exception.BusinessException;
import com.leika.shop.exception.InsufficientStockException;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartDto getCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartDto(cart);
    }

    @Transactional
    public CartDto addToCart(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản sản phẩm"));

        if (variant.getStockQty() < request.getQuantity()) {
            throw new InsufficientStockException("Sản phẩm không đủ số lượng trong kho");
        }

        // Check if item already exists
        CartItem existingItem = cartItemRepository
                .findByCartCartIdAndProductVariantVariantId(cart.getCartId(), request.getVariantId())
                .orElse(null);

        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + request.getQuantity();
            if (newQty > variant.getStockQty()) {
                throw new InsufficientStockException("Số lượng vượt quá tồn kho");
            }
            existingItem.setQuantity(newQty);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return buildCartDto(cart);
    }

    @Transactional
    public CartDto updateQuantity(Integer userId, Integer cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new BusinessException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (quantity > item.getProductVariant().getStockQty()) {
                throw new InsufficientStockException("Số lượng vượt quá tồn kho");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return buildCartDto(cart);
    }

    @Transactional
    public CartDto removeItem(Integer userId, Integer cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new BusinessException("Không có quyền chỉnh sửa giỏ hàng này");
        }

        cartItemRepository.delete(item);
        return buildCartDto(cart);
    }

    @Transactional
    public void clearCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartCartId(cart.getCartId());
    }

    // ---- Helpers ----

    public Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private CartDto buildCartDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartCartId(cart.getCartId());

        List<CartItemDto> itemDtos = items.stream().map(item -> {
            ProductVariant v = item.getProductVariant();
            Product p = v.getProduct();
            BigDecimal unitPrice = (p.getSalePrice() != null ? p.getSalePrice() : p.getBasePrice())
                    .add(v.getExtraPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            return CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .variantId(v.getVariantId())
                    .productName(p.getProductName())
                    .variantInfo(v.getSku())
                    .thumbnailUrl(v.getImageUrl() != null ? v.getImageUrl() : p.getThumbnailUrl())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .stockQty(v.getStockQty())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal subTotal = itemDtos.stream()
                .map(CartItemDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = subTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;

        int totalItems = itemDtos.stream().mapToInt(CartItemDto::getQuantity).sum();

        return CartDto.builder()
                .cartId(cart.getCartId())
                .items(itemDtos)
                .totalItems(totalItems)
                .subTotal(subTotal)
                .shippingFee(shippingFee)
                .totalAmount(subTotal.add(shippingFee))
                .build();
    }
}
