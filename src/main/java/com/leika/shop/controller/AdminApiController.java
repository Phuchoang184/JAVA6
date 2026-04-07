package com.leika.shop.controller;

import com.leika.shop.dto.*;
import com.leika.shop.repository.OrderRepository;
import com.leika.shop.repository.ProductRepository;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.service.AdminService;
import com.leika.shop.service.OrderService;
import com.leika.shop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final OrderService orderService;
    private final AdminService adminService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    // ======================== DASHBOARD ========================

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProducts", productRepository.countByIsActiveTrue());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByOrderStatus("PENDING"));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ======================== PRODUCTS ========================

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listProducts(keyword, page, size)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getProduct(id)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody AdminProductRequest request) {
        ProductDto product = adminService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo sản phẩm thành công", product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody AdminProductRequest request) {
        ProductDto product = adminService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sản phẩm thành công", product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
        adminService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm thành công", null));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllCategories()));
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<BrandDto>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllBrands()));
    }

    // ======================== ORDERS ========================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(status, page, size)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByIdAdmin(orderId)));
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
                orderService.updateOrderStatus(orderId, status)));
    }

    // ======================== USERS ========================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(keyword, page, size)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getUser(id)));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<UserDto>> changeUserRole(
            @PathVariable Integer userId,
            @RequestParam String roleName) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật vai trò thành công",
                adminService.updateUserRole(userId, roleName)));
    }

    @PutMapping("/users/{userId}/toggle-active")
    public ResponseEntity<ApiResponse<UserDto>> toggleUserActive(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
                adminService.toggleUserActive(userId)));
    }
}
