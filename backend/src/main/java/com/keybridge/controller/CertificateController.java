package com.keybridge.controller;

import com.keybridge.dto.cert.CertResponse;
import com.keybridge.dto.cert.IssueCertRequest;
import com.keybridge.dto.cert.RevokeCertRequest;
import com.keybridge.entity.User;
import com.keybridge.enums.CertStatus;
import com.keybridge.service.AuthService;
import com.keybridge.service.CertificateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/certs")
public class CertificateController {

    private final CertificateService certificateService;
    private final AuthService authService;

    public CertificateController(CertificateService certificateService, AuthService authService) {
        this.certificateService = certificateService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<CertResponse>> listCertificates(
            @RequestParam(required = false) CertStatus status,
            @RequestParam(required = false) String q
    ) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(certificateService.listCertificates(status, q, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertResponse> getCertificate(@PathVariable Long id) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(certificateService.getCertificate(id, currentUser));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<CertResponse>> getExpiringCertificates() {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(certificateService.getExpiringCertificates(currentUser));
    }

    @PostMapping({"/issue", "/issue-test"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertResponse> issueTestCertificate(@Valid @RequestBody IssueCertRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.issueTestCertificate(request, currentUser));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertResponse> uploadCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.uploadCertificate(file, userId, currentUser));
    }

    @RequestMapping(value = "/{id}/revoke", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertResponse> revokeCertificate(
            @PathVariable Long id,
            @RequestBody(required = false) RevokeCertRequest request,
            @RequestParam(value = "reason", required = false) String paramReason
    ) {
        User currentUser = authService.getCurrentUserEntity();
        String finalReason = "KEY_COMPROMISE";
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            finalReason = request.reason();
        } else if (paramReason != null && !paramReason.isBlank()) {
            finalReason = paramReason;
        }
        return ResponseEntity.ok(certificateService.revokeCertificate(id, finalReason, currentUser));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id) {
        User currentUser = authService.getCurrentUserEntity();
        String pem = certificateService.downloadPem(id, currentUser);
        byte[] bytes = pem.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cert-" + id + ".pem\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
