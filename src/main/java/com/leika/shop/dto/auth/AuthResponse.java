package com.leika.shop.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response trả về sau login / register thành công */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private long expiresIn;     // milliseconds
}
