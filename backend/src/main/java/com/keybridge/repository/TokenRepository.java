package com.keybridge.repository;

import com.keybridge.entity.Token;
import com.keybridge.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findBySerialNo(String serialNo);
    List<Token> findByOwnerId(Long ownerId);
    List<Token> findByStatus(TokenStatus status);
    long countByStatus(TokenStatus status);
}
