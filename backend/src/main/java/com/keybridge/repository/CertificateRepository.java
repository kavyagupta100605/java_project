package com.keybridge.repository;

import com.keybridge.entity.Certificate;
import com.keybridge.enums.CertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findBySha256Fingerprint(String fingerprint);
    List<Certificate> findByOwnerId(Long ownerId);
    List<Certificate> findByStatus(CertStatus status);
    List<Certificate> findByStatusNot(CertStatus status);
    List<Certificate> findByOwnerIdAndStatus(Long ownerId, CertStatus status);
    List<Certificate> findByValidToBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(CertStatus status);
}
