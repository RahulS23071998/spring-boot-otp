package com.starter.springboot.repository;

import com.starter.springboot.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing refresh tokens.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find active refresh token by token value
     */
    Optional<RefreshToken> findByTokenAndRevokedAtIsNullAndExpiresAtAfter(String token, Instant now);

    /**
     * Revoke all active refresh tokens for a user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt, rt.isActive = false WHERE rt.userId = :userId AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt, @Param("now") Instant now);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

}