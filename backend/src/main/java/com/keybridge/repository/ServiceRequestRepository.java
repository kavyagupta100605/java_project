package com.keybridge.repository;

import com.keybridge.entity.ServiceRequest;
import com.keybridge.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(RequestStatus status);
}
