package com.keybridge.dto.cert;

import com.keybridge.entity.Certificate;
import com.keybridge.enums.CertStatus;

import java.time.LocalDateTime;

public record CertResponse(
    Long id,
    Long tokenId,
    String tokenSerial,
    Long ownerId,
    String ownerName,
    String ownerEmail,
    String subject,
    String issuer,
    String serialHex,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    String sha256Fingerprint,
    CertStatus status,
    LocalDateTime revokedAt,
    String revokeReason,
    boolean hasPrivateKey,
    String pem,
    String subjectDn,
    String issuerDn,
    String serialNumber,
    String fingerprintSha256
) {
    public static CertResponse fromEntity(Certificate cert) {
        if (cert == null) return null;
        return new CertResponse(
            cert.getId(),
            cert.getToken() != null ? cert.getToken().getId() : null,
            cert.getToken() != null ? cert.getToken().getSerialNo() : null,
            cert.getOwner() != null ? cert.getOwner().getId() : null,
            cert.getOwner() != null ? cert.getOwner().getName() : null,
            cert.getOwner() != null ? cert.getOwner().getEmail() : null,
            cert.getSubject(),
            cert.getIssuer(),
            cert.getSerialHex(),
            cert.getValidFrom(),
            cert.getValidTo(),
            cert.getSha256Fingerprint(),
            cert.getStatus(),
            cert.getRevokedAt(),
            cert.getRevokeReason(),
            cert.getKeystoreFile() != null,
            cert.getPem(),
            // Aliases:
            cert.getSubject(),
            cert.getIssuer(),
            cert.getSerialHex(),
            cert.getSha256Fingerprint()
        );
    }
}
