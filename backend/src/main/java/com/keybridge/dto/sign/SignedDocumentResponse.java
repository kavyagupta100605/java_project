package com.keybridge.dto.sign;

import com.keybridge.entity.SignedDocument;

import java.time.LocalDateTime;

public record SignedDocumentResponse(
    Long id,
    Long certId,
    String certSubject,
    String fileName,
    String docHash,
    String signatureB64,
    String algorithm,
    LocalDateTime signedAt
) {
    public static SignedDocumentResponse fromEntity(SignedDocument d) {
        if (d == null) return null;
        return new SignedDocumentResponse(
            d.getId(),
            d.getCert() != null ? d.getCert().getId() : null,
            d.getCert() != null ? d.getCert().getSubject() : null,
            d.getFileName(),
            d.getDocHash(),
            d.getSignatureB64(),
            d.getAlgorithm(),
            d.getSignedAt()
        );
    }
}
