package com.leika.shop.repository;

import com.leika.shop.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Integer userId);

    Optional<UserProfile> findBySessionId(String sessionId);
}
