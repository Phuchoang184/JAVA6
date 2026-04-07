package com.leika.shop.controller;

import com.leika.shop.dto.*;
import com.leika.shop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication auth) {
        UserDto user = userService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserDto user = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công", user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }
}
