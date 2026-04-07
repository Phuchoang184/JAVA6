package com.leika.shop.service;

import com.leika.shop.dto.auth.*;
import com.leika.shop.entity.Role;
import com.leika.shop.entity.User;
import com.leika.shop.exception.EmailAlreadyExistsException;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.repository.RoleRepository;
import com.leika.shop.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService – xử lý đăng ký và đăng nhập, trả về JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    // ------------------------------------------------------------------
    //  Register
    // ------------------------------------------------------------------
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra email tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email '" + request.getEmail() + "' đã được sử dụng.");
        }

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("USER").build()));

        // Tạo user mới với role USER
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))  // BCrypt
                .phoneNumber(request.getPhone())
                .role(userRole)    // Mặc định register → USER
                .isActive(true)
                .build();

        userRepository.save(user);

        // Tạo token
        String token = jwtService.generateToken(buildSpringUser(user));

        return buildAuthResponse(token, user);
    }

    // ------------------------------------------------------------------
    //  Login
    // ------------------------------------------------------------------
    public AuthResponse login(LoginRequest request) {
        // Xác thực credentials – ném BadCredentialsException nếu sai
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại"));

        String token = jwtService.generateToken(buildSpringUser(user));

        return buildAuthResponse(token, user);
    }

    // ------------------------------------------------------------------
    //  Private helpers
    // ------------------------------------------------------------------
    private UserDetails buildSpringUser(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().getRoleName())   // ROLE_ prefix tự thêm
                .build();
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .fullName(user.getFullName())
                                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getRoleName())
                .expiresIn(jwtExpiration)
                .build();
    }
}
