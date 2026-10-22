package com.keybridge.dto.sign;

public record SignResponse(
    Long id,
    String docHash,
    String signatureB64,
    String certFingerprint,
    String documentHash,
    String signatureBase64,
    String documentName
) {
    public SignResponse(Long id, String docHash, String signatureB64, String certFingerprint) {
        this(id, docHash, signatureB64, certFingerprint, docHash, signatureB64, null);
    }

    public SignResponse(Long id, String docHash, String signatureB64, String certFingerprint, String fileName) {
        this(id, docHash, signatureB64, certFingerprint, docHash, signatureB64, fileName);
    }
}
