package com.leika.shop.service;

import com.leika.shop.dto.*;
import com.leika.shop.entity.*;
import com.leika.shop.exception.*;
import com.leika.shop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderDto placeOrder(Integer userId, OrderRequest request) {
        // 1. Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // 2. Get cart and validate not empty
        CartDto cart = cartService.getCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Giỏ hàng trống, không thể đặt hàng");
        }

        // 3. Validate stock for ALL items upfront before creating anything
        List<ProductVariant> variantsToUpdate = new ArrayList<>();
        for (CartItemDto item : cart.getItems()) {
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy phiên bản sản phẩm: " + item.getProductName()));

            if (!variant.isActive()) {
                throw new InsufficientStockException(
                        "Sản phẩm '" + item.getProductName() + "' hiện không còn bán");
            }

            if (variant.getStockQty() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Sản phẩm '" + item.getProductName() + "' không đủ tồn kho (còn "
                                + variant.getStockQty() + ", cần " + item.getQuantity() + ")");
            }
            variantsToUpdate.add(variant);
        }

        // 4. Build address snapshot as JSON
        String addressSnapshot;
        try {
            addressSnapshot = objectMapper.writeValueAsString(Map.of(
                    "fullName", request.getFullName(),
                    "phone", request.getPhone(),
                    "address", request.getAddress(),
                    "ward", request.getWard() != null ? request.getWard() : "",
                    "district", request.getDistrict() != null ? request.getDistrict() : "",
                    "province", request.getProvince() != null ? request.getProvince() : ""
            ));
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi xử lý địa chỉ");
        }

        // 5. Apply coupon discount if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discountAmount = applyCoupon(request.getCouponCode(), cart.getSubTotal());
        }

        // 6. Calculate total price
        BigDecimal totalAmount = cart.getSubTotal()
                .subtract(discountAmount)
                .add(cart.getShippingFee());
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 7. Create order
        Order order = Order.builder()
                .user(user)
                .addressSnapshot(addressSnapshot)
                .subTotal(cart.getSubTotal())
                .discountAmount(discountAmount)
                .shippingFee(cart.getShippingFee())
                .totalAmount(totalAmount)
                .orderStatus("PENDING")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD")
                .paymentStatus("UNPAID")
                .note(request.getNote())
                .build();
        order = orderRepository.save(order);

        // 8. Create order details and reduce stock (already validated above)
        List<OrderDetailDto> orderItemDtos = new ArrayList<>();
        for (int i = 0; i < cart.getItems().size(); i++) {
            CartItemDto item = cart.getItems().get(i);
            ProductVariant variant = variantsToUpdate.get(i);

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .productVariant(variant)
                    .productName(item.getProductName())
                    .variantInfo(item.getVariantInfo())
                    .sku(variant.getSku())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .build();
            orderDetailRepository.save(detail);
            orderItemDtos.add(toDetailDto(detail));

            // Reduce stock
            variant.setStockQty(variant.getStockQty() - item.getQuantity());
            variantRepository.save(variant);
        }

        // 9. Clear cart after successful order
        cartService.clearCart(userId);

        // 10. Return full order summary with items
        OrderDto orderDto = toOrderDto(order);
        orderDto.setItems(orderItemDtos);
        return orderDto;
    }

    public List<OrderDto> getUserOrders(Integer userId) {
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toOrderDto).collect(Collectors.toList());
    }

    public OrderDto getOrderById(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getUser() == null || !order.getUser().getUserId().equals(userId)) {
            throw new BusinessException("Không có quyền xem đơn hàng này");
        }

        OrderDto dto = toOrderDto(order);
        List<OrderDetail> details = orderDetailRepository.findByOrderOrderId(orderId);
        dto.setItems(details.stream().map(this::toDetailDto).collect(Collectors.toList()));
        return dto;
    }
    public OrderDto getOrderByIdAdmin(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        OrderDto dto = toOrderDto(order);
        List<OrderDetail> details = orderDetailRepository.findByOrderOrderId(orderId);
        dto.setItems(details.stream().map(this::toDetailDto).collect(Collectors.toList()));
        return dto;
    }
    // Admin methods
    public Page<OrderDto> getAllOrders(String status, int page, int size) {
        Page<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByOrderStatusOrderByCreatedAtDesc(
                    status, PageRequest.of(page, size));
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        return orders.map(this::toOrderDto);
    }

    @Transactional
    public OrderDto updateOrderStatus(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        order.setOrderStatus(status);
        if ("DELIVERED".equals(status)) {
            order.setPaymentStatus("PAID");
        }
        orderRepository.save(order);
        return toOrderDto(order);
    }

    private BigDecimal applyCoupon(String code, BigDecimal subTotal) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new InvalidCouponException("Mã giảm giá không hợp lệ hoặc đã ngừng"));

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCouponException("Mã giảm giá đã hết hạn");
        }

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new InvalidCouponException("Mã giảm giá đã hết lượt sử dụng");
        }

        if (subTotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new InvalidCouponException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá");
        }

        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = subTotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        // Update used count
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return discount;
    }

    @Transactional
    public OrderDto confirmPayment(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new BusinessException("Không có quyền xác nhận thanh toán cho đơn hàng này");
        }

        if (!"UNPAID".equals(order.getPaymentStatus())) {
            throw new BusinessException("Đơn hàng này đã được thanh toán");
        }

        order.setPaymentStatus("PAID");
        orderRepository.save(order);
        return toOrderDto(order);
    }

    private OrderDto toOrderDto(Order o) {
        return OrderDto.builder()
                .orderId(o.getOrderId())
                .orderStatus(o.getOrderStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .subTotal(o.getSubTotal())
                .discountAmount(o.getDiscountAmount())
                .shippingFee(o.getShippingFee())
                .totalAmount(o.getTotalAmount())
                .note(o.getNote())
                .addressSnapshot(o.getAddressSnapshot())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private OrderDetailDto toDetailDto(OrderDetail d) {
        return OrderDetailDto.builder()
                .productName(d.getProductName())
                .variantInfo(d.getVariantInfo())
                .sku(d.getSku())
                .unitPrice(d.getUnitPrice())
                .quantity(d.getQuantity())
                .lineTotal(d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .build();
    }
}
