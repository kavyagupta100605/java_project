package com.keybridge.service;

import com.keybridge.dto.verify.VerificationLogResponse;
import com.keybridge.dto.verify.VerificationStepDto;
import com.keybridge.dto.verify.VerifyResponse;
import com.keybridge.entity.Certificate;
import com.keybridge.entity.User;
import com.keybridge.entity.VerificationLog;
import com.keybridge.enums.CertStatus;
import com.keybridge.enums.Role;
import com.keybridge.enums.VerifyResult;
import com.keybridge.exception.BadRequestException;
import com.keybridge.repository.CertificateRepository;
import com.keybridge.repository.VerificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class VerificationService {

    private final CryptoService cryptoService;
    private final CertificateParserService parserService;
    private final TestCaService testCaService;
    private final CertificateRepository certificateRepository;
    private final VerificationLogRepository verificationLogRepository;

    public VerificationService(
            CryptoService cryptoService,
            CertificateParserService parserService,
            TestCaService testCaService,
            CertificateRepository certificateRepository,
            VerificationLogRepository verificationLogRepository
    ) {
        this.cryptoService = cryptoService;
        this.parserService = parserService;
        this.testCaService = testCaService;
        this.certificateRepository = certificateRepository;
        this.verificationLogRepository = verificationLogRepository;
    }

    @Transactional
    public VerifyResponse verify(
            MultipartFile file,
            String signatureB64,
            byte[] certBytes,
            Long certId,
            User current
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file to verify cannot be empty");
        }
        if (signatureB64 == null || signatureB64.isBlank()) {
            throw new BadRequestException("Digital signature cannot be empty");
        }

        List<VerificationStepDto> steps = new ArrayList<>();
        VerifyResult result;
        String reason;
        Long matchedCertId = certId;

        try {
            // Obtain certificate bytes
            byte[] rawCertBytes = certBytes;
            if ((rawCertBytes == null || rawCertBytes.length == 0) && certId != null) {
                Certificate dbC = certificateRepository.findById(certId).orElse(null);
                if (dbC != null) {
                    rawCertBytes = dbC.getPem().getBytes(StandardCharsets.UTF_8);
                }
            }

            if (rawCertBytes == null || rawCertBytes.length == 0) {
                steps.add(new VerificationStepDto("PARSE", "FAILED", "No certificate provided or found"));
                steps.add(new VerificationStepDto("CHAIN", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("REVOCATION", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("VALIDITY", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, null, VerifyResult.ERROR, "Certificate data missing", steps);
            }

            // Step 1: Parse Certificate
            X509Certificate cert;
            try {
                cert = parserService.parse(rawCertBytes);
                steps.add(new VerificationStepDto("PARSE", "PASSED", "Certificate parsed successfully"));
            } catch (Exception e) {
                steps.add(new VerificationStepDto("PARSE", "FAILED", "Invalid certificate format"));
                steps.add(new VerificationStepDto("CHAIN", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("REVOCATION", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("VALIDITY", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, null, VerifyResult.ERROR, "Could not parse certificate file: " + e.getMessage(), steps);
            }

            String fp = cryptoService.sha256Hex(cert.getEncoded());
            Optional<Certificate> dbCert = certificateRepository.findBySha256Fingerprint(fp);
            if (matchedCertId == null && dbCert.isPresent()) {
                matchedCertId = dbCert.get().getId();
            }

            // Step 2: Chain Check (Root CA Signature verification)
            try {
                cert.verify(testCaService.rootCert().getPublicKey());
                steps.add(new VerificationStepDto("CHAIN", "PASSED", "Issued by trusted KeyBridge Root CA"));
            } catch (GeneralSecurityException e) {
                steps.add(new VerificationStepDto("CHAIN", "FAILED", "Not issued by the trusted root"));
                steps.add(new VerificationStepDto("REVOCATION", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("VALIDITY", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, matchedCertId, VerifyResult.UNTRUSTED, "Certificate was not issued by the trusted KeyBridge Root CA", steps);
            }

            // Step 3: Revocation Check
            if (dbCert.isPresent() && dbCert.get().getStatus() == CertStatus.REVOKED) {
                steps.add(new VerificationStepDto("REVOCATION", "FAILED", "Certificate has been revoked: " + dbCert.get().getRevokeReason()));
                steps.add(new VerificationStepDto("VALIDITY", "SKIPPED", "Skipped"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, matchedCertId, VerifyResult.REVOKED, "Certificate has been revoked", steps);
            }
            steps.add(new VerificationStepDto("REVOCATION", "PASSED", "Certificate is not revoked"));

            // Step 4: Validity Check
            try {
                cert.checkValidity();
                steps.add(new VerificationStepDto("VALIDITY", "PASSED", "Within active validity dates"));
            } catch (CertificateExpiredException e) {
                steps.add(new VerificationStepDto("VALIDITY", "FAILED", "Certificate has expired"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, matchedCertId, VerifyResult.EXPIRED, "Certificate has expired", steps);
            } catch (CertificateNotYetValidException e) {
                steps.add(new VerificationStepDto("VALIDITY", "FAILED", "Certificate not yet valid"));
                steps.add(new VerificationStepDto("SIGNATURE", "SKIPPED", "Skipped"));
                return log(current, file, matchedCertId, VerifyResult.NOT_YET_VALID, "Certificate is not yet valid", steps);
            }

            // Step 5: Signature Check
            byte[] sigBytes;
            try {
                String cleanSig = signatureB64.trim().replaceAll("\\s+", "");
                sigBytes = Base64.getDecoder().decode(cleanSig);
            } catch (IllegalArgumentException e) {
                steps.add(new VerificationStepDto("SIGNATURE", "FAILED", "Malformed Base64 signature string"));
                return log(current, file, matchedCertId, VerifyResult.TAMPERED, "Invalid Base64 signature payload", steps);
            }

            boolean ok = cryptoService.verify(file.getBytes(), sigBytes, cert.getPublicKey());
            if (ok) {
                steps.add(new VerificationStepDto("SIGNATURE", "PASSED", "Cryptographic signature matches document digest"));
                result = VerifyResult.VALID;
                reason = "Signature is valid and document is intact";
            } else {
                steps.add(new VerificationStepDto("SIGNATURE", "FAILED", "Signature mismatch or document content altered"));
                result = VerifyResult.TAMPERED;
                reason = "Document was modified or signature does not match";
            }

        } catch (Exception e) {
            result = VerifyResult.ERROR;
            reason = "Could not process verification: " + e.getMessage();
        }

        return log(current, file, matchedCertId, result, reason, steps);
    }

    private VerifyResponse log(
            User u,
            MultipartFile f,
            Long certId,
            VerifyResult r,
            String reason,
            List<VerificationStepDto> steps
    ) {
        VerificationLog l = new VerificationLog();
        l.setVerifiedBy(u);
        l.setFileName(f != null ? f.getOriginalFilename() : "unknown");
        l.setCertId(certId);
        l.setResult(r);
        l.setReason(reason);
        verificationLogRepository.save(l);

        String signerSubject = null;
        String issuerSubject = null;
        String serial = null;
        String docHash = null;

        if (certId != null) {
            certificateRepository.findById(certId).ifPresent(c -> {
                // c.getSubject(), c.getIssuer(), c.getSerialHex()
            });
            Optional<Certificate> optCert = certificateRepository.findById(certId);
            if (optCert.isPresent()) {
                Certificate c = optCert.get();
                signerSubject = c.getSubject();
                issuerSubject = c.getIssuer();
                serial = c.getSerialHex();
            }
        }

        if (f != null) {
            try {
                docHash = cryptoService.sha256Hex(f.getBytes());
            } catch (Exception ignored) {}
        }

        return new VerifyResponse(r, reason, steps, signerSubject, issuerSubject, serial, docHash, java.time.LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<VerificationLogResponse> listVerifications(User current) {
        List<VerificationLog> logs = (current.getRole() == Role.USER)
                ? verificationLogRepository.findByVerifiedByIdOrderByVerifiedAtDesc(current.getId())
                : verificationLogRepository.findAllByOrderByVerifiedAtDesc();

        return logs.stream()
                .map(VerificationLogResponse::fromEntity)
                .toList();
    }
}
