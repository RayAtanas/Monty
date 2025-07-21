package com.monty.backend.Repository;

import com.monty.backend.Model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
@Repository
public interface IOtpRepository extends JpaRepository<Otp,Long> {

    Optional<Otp> findByUserIdAndCodeAndVerifiedFalse(Long userId, String code);
    void deleteByExpirationTimeBefore(LocalDateTime now);
    void deleteByUserId(Long userId);
}
