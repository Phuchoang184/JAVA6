package com.leika.shop.controller;

import com.leika.shop.dto.*;
import com.leika.shop.entity.User;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDto>> getCart(Authentication auth) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartDto>> addToCart(
            Authentication auth,
            @Valid @RequestBody CartItemRequest request) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào giỏ hàng", cartService.addToCart(userId, request)));
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDto>> updateQuantity(
            Authentication auth,
            @PathVariable Integer cartItemId,
            @RequestParam int quantity) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateQuantity(userId, cartItemId, quantity)));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDto>> removeItem(
            Authentication auth,
            @PathVariable Integer cartItemId) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(cartService.removeItem(userId, cartItemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication auth) {
        Integer userId = getUserId(auth);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa giỏ hàng", null));
    }

    private Integer getUserId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getUserId();
    }
}
