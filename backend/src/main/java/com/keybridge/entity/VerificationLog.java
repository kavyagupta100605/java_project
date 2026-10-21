package com.keybridge.entity;

import com.keybridge.enums.VerifyResult;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs")
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", nullable = false)
    private User verifiedBy;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "cert_id")
    private Long certId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerifyResult result;

    @Column(length = 300)
    private String reason;

    @CreationTimestamp
    @Column(name = "verified_at", updatable = false)
    private LocalDateTime verifiedAt;

    public VerificationLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getCertId() {
        return certId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public VerifyResult getResult() {
        return result;
    }

    public void setResult(VerifyResult result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
