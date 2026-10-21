package com.keybridge.service;

import com.keybridge.dto.cert.CertMeta;
import com.keybridge.dto.cert.CertResponse;
import com.keybridge.dto.cert.IssueCertRequest;
import com.keybridge.entity.Certificate;
import com.keybridge.entity.Token;
import com.keybridge.entity.User;
import com.keybridge.enums.CertStatus;
import com.keybridge.enums.Role;
import com.keybridge.exception.BadRequestException;
import com.keybridge.exception.ForbiddenException;
import com.keybridge.exception.NotFoundException;
import com.keybridge.repository.CertificateRepository;
import com.keybridge.repository.TokenRepository;
import com.keybridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final TestCaService testCaService;
    private final CertificateParserService parserService;

    public CertificateService(
            CertificateRepository certificateRepository,
            UserRepository userRepository,
            TokenRepository tokenRepository,
            TestCaService testCaService,
            CertificateParserService parserService
    ) {
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.testCaService = testCaService;
        this.parserService = parserService;
    }

    @Transactional(readOnly = true)
    public List<CertResponse> listCertificates(CertStatus status, String query, User currentUser) {
        List<Certificate> certs = (currentUser.getRole() == Role.USER)
                ? certificateRepository.findByOwnerId(currentUser.getId())
                : certificateRepository.findAll();

        return certs.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase();
                    return c.getSubject().toLowerCase().contains(q) ||
                           c.getIssuer().toLowerCase().contains(q) ||
                           c.getSerialHex().toLowerCase().contains(q) ||
                           c.getSha256Fingerprint().toLowerCase().contains(q);
                })
                .map(CertResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertResponse getCertificate(Long id, User currentUser) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certificate not found with ID: " + id));

        if (currentUser.getRole() == Role.USER && !cert.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only view your own certificates");
        }

        return CertResponse.fromEntity(cert);
    }

    @Transactional(readOnly = true)
    public List<CertResponse> getExpiringCertificates(User currentUser) {
        return listCertificates(CertStatus.EXPIRING, null, currentUser);
    }

    @Transactional
    public CertResponse issueTestCertificate(IssueCertRequest req, User currentUser) {
        User owner = null;
        if (req.userId() != null) {
            owner = userRepository.findById(req.userId())
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + req.userId()));
        } else if (currentUser != null) {
            owner = currentUser;
        } else {
            owner = userRepository.findByEmail("admin@keybridge.com")
                    .orElseThrow(() -> new NotFoundException("Default user not found"));
        }

        Token token = null;
        if (req.tokenId() != null) {
            token = tokenRepository.findById(req.tokenId())
                    .orElseThrow(() -> new NotFoundException("Token not found with ID: " + req.tokenId()));
        }

        try {
            TestCaService.IssuedCert issued = testCaService.issue(req.commonName(), owner.getEmail(), req.resolvedDays());
            CertMeta meta = parserService.extract(issued.cert());

            if (certificateRepository.findBySha256Fingerprint(meta.fingerprint()).isPresent()) {
                throw new BadRequestException("Certificate with identical fingerprint already exists");
            }

            Certificate cert = new Certificate();
            cert.setOwner(owner);
            cert.setToken(token);
            cert.setSubject(meta.subject());
            cert.setIssuer(meta.issuer());
            cert.setSerialHex(meta.serialHex());
            cert.setValidFrom(meta.validFrom());
            cert.setValidTo(meta.validTo());
            cert.setSha256Fingerprint(meta.fingerprint());
            cert.setPem(meta.pem());
            cert.setKeystoreFile(issued.keystoreFile());
            cert.setStatus(meta.validTo().isBefore(LocalDateTime.now()) ? CertStatus.EXPIRED : CertStatus.VALID);

            certificateRepository.save(cert);
            return CertResponse.fromEntity(cert);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue test certificate: " + e.getMessage(), e);
        }
    }

    public CertResponse issueTestCertificate(IssueCertRequest req) {
        return issueTestCertificate(req, null);
    }

    @Transactional
    public CertResponse uploadCertificate(MultipartFile file, Long userId, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        User owner = null;
        if (userId != null) {
            owner = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        } else if (currentUser != null) {
            owner = currentUser;
        } else {
            owner = userRepository.findByEmail("admin@keybridge.com")
                    .orElseThrow(() -> new NotFoundException("Default user not found"));
        }

        try {
            X509Certificate x509 = parserService.parse(file.getBytes());
            CertMeta meta = parserService.extract(x509);

            if (certificateRepository.findBySha256Fingerprint(meta.fingerprint()).isPresent()) {
                throw new BadRequestException("Certificate with fingerprint " + meta.fingerprint() + " already exists");
            }

            Certificate cert = new Certificate();
            cert.setOwner(owner);
            cert.setSubject(meta.subject());
            cert.setIssuer(meta.issuer());
            cert.setSerialHex(meta.serialHex());
            cert.setValidFrom(meta.validFrom());
            cert.setValidTo(meta.validTo());
            cert.setSha256Fingerprint(meta.fingerprint());
            cert.setPem(meta.pem());
            cert.setKeystoreFile(null); // External certificate: public only
            cert.setStatus(meta.validTo().isBefore(LocalDateTime.now()) ? CertStatus.EXPIRED : CertStatus.VALID);

            certificateRepository.save(cert);
            return CertResponse.fromEntity(cert);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse uploaded certificate: " + e.getMessage());
        }
    }

    @Transactional
    public CertResponse revokeCertificate(Long id, String reason, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can revoke certificates");
        }

        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certificate not found with ID: " + id));

        if (cert.getStatus() == CertStatus.REVOKED) {
            throw new BadRequestException("Certificate is already revoked");
        }

        cert.setStatus(CertStatus.REVOKED);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevokeReason(reason);
        certificateRepository.save(cert);

        return CertResponse.fromEntity(cert);
    }

    @Transactional(readOnly = true)
    public String downloadPem(Long id, User currentUser) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Certificate not found with ID: " + id));

        if (currentUser.getRole() == Role.USER && !cert.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only download your own certificates");
        }

        return cert.getPem();
    }
}
