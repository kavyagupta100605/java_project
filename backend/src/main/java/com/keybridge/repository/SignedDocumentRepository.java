package com.keybridge.repository;

import com.keybridge.entity.SignedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignedDocumentRepository extends JpaRepository<SignedDocument, Long> {
    List<SignedDocument> findByUserIdOrderBySignedAtDesc(Long userId);
    List<SignedDocument> findAllByOrderBySignedAtDesc();
    List<SignedDocument> findByCertId(Long certId);
    long countByUserId(Long userId);
    long countBySignedAtBetween(LocalDateTime start, LocalDateTime end);
}
