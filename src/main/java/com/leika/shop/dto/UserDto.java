package com.leika.shop.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String roleName;
    private boolean isActive;
    private LocalDateTime createdAt;
}
