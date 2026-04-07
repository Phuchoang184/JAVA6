package com.leika.shop.controller;

import com.leika.shop.dto.ApiResponse;
import com.leika.shop.dto.BehaviorRequest;
import com.leika.shop.dto.UserProfileDto;
import com.leika.shop.entity.User;
import com.leika.shop.repository.UserRepository;
import com.leika.shop.service.BehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Tracks user behavior events (product views, cart adds, etc.)
 * and exposes the computed user profile for AI personalisation.
 * Both endpoints are public so guest sessions can also be tracked.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorTrackingService behaviorService;
    private final UserRepository          userRepository;

    /**
     * POST /api/track-behavior
     * Body: { sessionId, productId, actionType }
     * actionType: VIEW | CART_ADD | PURCHASE | WISHLIST
     */
    @PostMapping("/track-behavior")
    public ResponseEntity<ApiResponse<Void>> trackBehavior(@RequestBody BehaviorRequest request) {
        behaviorService.track(resolveUserId(),
                request.getSessionId(),
                request.getProductId(),
                request.getActionType());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * GET /api/user-profile?sessionId=xxx
     * Returns the AI-computed preference profile for the current user/session.
     */
    @GetMapping("/user-profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserProfile(
            @RequestParam(required = false) String sessionId) {
        UserProfileDto profile = behaviorService.getProfileDto(resolveUserId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    private Integer resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName())
                .map(User::getUserId).orElse(null);
    }
}
