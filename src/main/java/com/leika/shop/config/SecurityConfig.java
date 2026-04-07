package com.leika.shop.config;

import com.leika.shop.security.JwtAuthenticationFilter;
import com.leika.shop.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * - Sử dụng JWT (Stateless)
 * - BCrypt password encoding
 * - Phân quyền: USER (đặt hàng), ADMIN (quản lý sản phẩm)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Bật @PreAuthorize ở Controller/Service
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    // ------------------------------------------------------------------ //
    //  Public endpoints – không cần xác thực
    // ------------------------------------------------------------------ //
    private static final String[] PUBLIC_PAGES = {
            "/",
            "/shop",
            "/products",
            "/products/**",
            "/categories/**",
            "/login",
            "/register",
            "/cart",
            "/checkout",
            "/payment/vnpay",
            "/profile",
            "/orders",
            "/orders/**",
            "/admin",
            "/admin/**",
            "/sale",
            "/collections",
            "/new-arrivals",
            "/category/**",
            "/policy/**",
            "/guide/**",
            "/error"
    };

    private static final String[] PUBLIC_RESOURCES = {
            "/images/**",
            "/css/**",
            "/js/**",
            "/webjars/**",
            "/favicon.ico"
    };

    private static final String[] PUBLIC_API = {
            "/api/products/**",
            "/api/reviews/product/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/chat",
            "/api/chat/history/**",
            "/api/track-behavior",
            "/api/user-profile"
    };

    // ------------------------------------------------------------------ //
    //  Security Filter Chain
    // ------------------------------------------------------------------ //
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF vì dùng JWT (stateless)
            .csrf(AbstractHttpConfigurer::disable)

            // Cấu hình phân quyền URL
            .authorizeHttpRequests(auth -> auth
                // Static resources – không cần xác thực
                .requestMatchers(PUBLIC_RESOURCES).permitAll()

                // Trang Thymeleaf public (GET)
                .requestMatchers(HttpMethod.GET, PUBLIC_PAGES).permitAll()

                // API public
                .requestMatchers(PUBLIC_API).permitAll()

                // ---- ADMIN: Quản lý sản phẩm ----
                .requestMatchers(HttpMethod.POST,   "/api/admin/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/admin/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/products/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ---- USER / ADMIN: Đặt hàng & xem đơn ----
                .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/orders", "/api/orders/**").hasAnyRole("USER", "ADMIN")

                // ---- Cart (USER trở lên) ----
                .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")

                // ---- User Profile (USER trở lên) ----
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")

                // ---- Reviews ----
                .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/reviews/check/**").hasAnyRole("USER", "ADMIN")

                // Tất cả request còn lại phải xác thực
                .anyRequest().authenticated()
            )

            // Stateless – không dùng Session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Đăng ký Authentication Provider
            .authenticationProvider(authenticationProvider())

            // Thêm JWT Filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ------------------------------------------------------------------ //
    //  Beans
    // ------------------------------------------------------------------ //
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);   // strength = 12
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
