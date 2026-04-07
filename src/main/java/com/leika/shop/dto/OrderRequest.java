package com.leika.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$", message = "Số điện thoại phải gồm 10 số và bắt đầu bằng đầu số Việt Nam hợp lệ")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 5, max = 500, message = "Địa chỉ phải từ 5-500 ký tự")
    private String address;

    @Size(max = 100, message = "Phường/xã không được vượt quá 100 ký tự")
    private String ward;

    @Size(max = 100, message = "Quận/huyện không được vượt quá 100 ký tự")
    private String district;

    @Size(max = 100, message = "Tỉnh/thành phố không được vượt quá 100 ký tự")
    private String province;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(regexp = "^(COD|VNPAY|MOMO)$", message = "Phương thức thanh toán không hợp lệ (COD, VNPAY, MOMO)")
    private String paymentMethod;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @Size(max = 50, message = "Mã giảm giá không được vượt quá 50 ký tự")
    private String couponCode;
}
