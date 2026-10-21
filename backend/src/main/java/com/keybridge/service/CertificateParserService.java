package com.keybridge.service;

import com.keybridge.dto.cert.CertMeta;
import com.keybridge.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class CertificateParserService {

    private final CryptoService crypto;

    public CertificateParserService(CryptoService crypto) {
        this.crypto = crypto;
    }

    public X509Certificate parse(byte[] bytes) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            throw new BadRequestException("Invalid certificate file: " + e.getMessage());
        }
    }

    public CertMeta extract(X509Certificate c) throws CertificateEncodingException {
        return new CertMeta(
            c.getSubjectX500Principal().getName(),
            c.getIssuerX500Principal().getName(),
            c.getSerialNumber().toString(16),
            LocalDateTime.ofInstant(c.getNotBefore().toInstant(), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(c.getNotAfter().toInstant(), ZoneId.systemDefault()),
            crypto.sha256Hex(c.getEncoded()),
            toPem(c)
        );
    }

    public String toPem(X509Certificate c) throws CertificateEncodingException {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(c.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
    }
}
