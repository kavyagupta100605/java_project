package com.keybridge.dto.cert;

import java.time.LocalDateTime;

public record CertMeta(
    String subject,
    String issuer,
    String serialHex,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    String fingerprint,
    String pem
) {}
