package com.leika.shop.controller;

import com.leika.shop.dto.*;
import com.leika.shop.entity.User;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(
            Authentication auth,
            @Valid @RequestBody OrderRequest request) {
        Integer userId = getUserId(auth);
        OrderDto order = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt hàng thành công!", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders(Authentication auth) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getUserOrders(userId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            Authentication auth,
            @PathVariable Integer orderId) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(orderId, userId)));
    }

    @PutMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<OrderDto>> confirmPayment(
            Authentication auth,
            @PathVariable Integer orderId) {
        Integer userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok("Thanh toán đã được xác nhận",
                orderService.confirmPayment(orderId, userId)));
    }

    private Integer getUserId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getUserId();
    }
}
