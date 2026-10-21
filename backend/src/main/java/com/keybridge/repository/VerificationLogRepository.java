package com.keybridge.repository;

import com.keybridge.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    List<VerificationLog> findAllByOrderByVerifiedAtDesc();
    List<VerificationLog> findTop10ByOrderByVerifiedAtDesc();
    List<VerificationLog> findByVerifiedByIdOrderByVerifiedAtDesc(Long userId);
    long countByVerifiedAtBetween(LocalDateTime start, LocalDateTime end);
}
