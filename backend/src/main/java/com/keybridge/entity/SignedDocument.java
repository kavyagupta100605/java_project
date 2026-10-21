package com.keybridge.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signed_documents")
public class SignedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cert_id", nullable = false)
    private Certificate cert;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "doc_hash", nullable = false, length = 64)
    private String docHash;

    @Lob
    @Column(name = "signature_b64", nullable = false, columnDefinition = "LONGTEXT")
    private String signatureB64;

    @Column(nullable = false, length = 30)
    private String algorithm = "SHA256withRSA";

    @CreationTimestamp
    @Column(name = "signed_at", updatable = false)
    private LocalDateTime signedAt;

    public SignedDocument() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Certificate getCert() {
        return cert;
    }

    public void setCert(Certificate cert) {
        this.cert = cert;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocHash() {
        return docHash;
    }

    public void setDocHash(String docHash) {
        this.docHash = docHash;
    }

    public String getSignatureB64() {
        return signatureB64;
    }

    public void setSignatureB64(String signatureB64) {
        this.signatureB64 = signatureB64;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }
}
