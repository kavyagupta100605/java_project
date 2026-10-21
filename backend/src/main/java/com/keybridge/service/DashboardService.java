package com.keybridge.service;

import com.keybridge.dto.dashboard.*;
import com.keybridge.entity.Certificate;
import com.keybridge.entity.Token;
import com.keybridge.entity.User;
import com.keybridge.entity.VerificationLog;
import com.keybridge.enums.CertStatus;
import com.keybridge.enums.RequestStatus;
import com.keybridge.enums.Role;
import com.keybridge.enums.TokenStatus;
import com.keybridge.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private final CertificateRepository certificateRepository;
    private final TokenRepository tokenRepository;
    private final SignedDocumentRepository signedDocumentRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;

    public DashboardService(
            CertificateRepository certificateRepository,
            TokenRepository tokenRepository,
            SignedDocumentRepository signedDocumentRepository,
            VerificationLogRepository verificationLogRepository,
            ServiceRequestRepository serviceRequestRepository,
            UserRepository userRepository
    ) {
        this.certificateRepository = certificateRepository;
        this.tokenRepository = tokenRepository;
        this.signedDocumentRepository = signedDocumentRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats(User currentUser) {
        List<Certificate> certs = (currentUser.getRole() == Role.USER)
                ? certificateRepository.findByOwnerId(currentUser.getId())
                : certificateRepository.findAll();

        long valid = certs.stream().filter(c -> c.getStatus() == CertStatus.VALID).count();
        long expiring = certs.stream().filter(c -> c.getStatus() == CertStatus.EXPIRING).count();
        long expired = certs.stream().filter(c -> c.getStatus() == CertStatus.EXPIRED).count();
        long revoked = certs.stream().filter(c -> c.getStatus() == CertStatus.REVOKED).count();

        List<Token> tokens = (currentUser.getRole() == Role.USER)
                ? tokenRepository.findByOwnerId(currentUser.getId())
                : tokenRepository.findAll();

        long assignedTokens = tokens.stream().filter(t -> t.getStatus() == TokenStatus.ASSIGNED).count();

        long pendingRequests = (currentUser.getRole() == Role.USER)
                ? serviceRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING).count()
                : serviceRequestRepository.countByStatus(RequestStatus.PENDING);

        List<VerificationLog> recentLogs = (currentUser.getRole() == Role.USER)
                ? verificationLogRepository.findByVerifiedByIdOrderByVerifiedAtDesc(currentUser.getId())
                : verificationLogRepository.findTop10ByOrderByVerifiedAtDesc();

        List<RecentVerificationItem> items = recentLogs.stream()
                .limit(10)
                .map(l -> new RecentVerificationItem(
                        l.getFileName() != null ? l.getFileName() : "Document",
                        l.getResult(),
                        l.getVerifiedAt()
                ))
                .toList();

        return new DashboardStatsDto(
                certs.size(),
                valid,
                expiring,
                expired,
                revoked,
                tokens.size(),
                assignedTokens,
                pendingRequests,
                items
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityPoint> getActivity(int days) {
        int count = days > 0 ? days : 7;
        LocalDate today = LocalDate.now();
        List<ActivityPoint> points = new ArrayList<>();

        for (int i = count - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long signs = signedDocumentRepository.countBySignedAtBetween(start, end);
            long verifications = verificationLogRepository.countByVerifiedAtBetween(start, end);

            points.add(new ActivityPoint(date.toString(), signs, verifications));
        }

        return points;
    }

    @Transactional(readOnly = true)
    public List<ExpiryPoint> getExpiryTimeline(int months) {
        int count = months > 0 ? months : 6;
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<ExpiryPoint> points = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            LocalDate monthStart = now.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthEnd.plusDays(1).atStartOfDay();

            List<Certificate> certs = certificateRepository.findByValidToBetween(start, end);
            points.add(new ExpiryPoint(monthStart.format(formatter), certs.size()));
        }

        return points;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatusDistribution() {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (CertStatus s : CertStatus.values()) {
            dist.put(s.name(), certificateRepository.countByStatus(s));
        }
        return dist;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, User currentUser) {
        if (query == null || query.isBlank()) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }
        String q = query.toLowerCase().trim();

        // 1. Search certificates
        List<Certificate> certs = (currentUser.getRole() == Role.USER)
                ? certificateRepository.findByOwnerId(currentUser.getId())
                : certificateRepository.findAll();

        List<SearchResultItem> certResults = certs.stream()
                .filter(c -> c.getSubject().toLowerCase().contains(q) ||
                             c.getSha256Fingerprint().toLowerCase().contains(q) ||
                             c.getSerialHex().toLowerCase().contains(q))
                .limit(5)
                .map(c -> new SearchResultItem(
                        c.getId(),
                        c.getSubject(),
                        "Status: " + c.getStatus().name() + " | Serial: " + c.getSerialHex(),
                        "Certificate",
                        "/certificates/" + c.getId()
                ))
                .toList();

        // 2. Search tokens
        List<Token> tokens = (currentUser.getRole() == Role.USER)
                ? tokenRepository.findByOwnerId(currentUser.getId())
                : tokenRepository.findAll();

        List<SearchResultItem> tokenResults = tokens.stream()
                .filter(t -> t.getSerialNo().toLowerCase().contains(q) ||
                             (t.getLabel() != null && t.getLabel().toLowerCase().contains(q)))
                .limit(5)
                .map(t -> new SearchResultItem(
                        t.getId(),
                        t.getSerialNo() + (t.getLabel() != null ? " - " + t.getLabel() : ""),
                        "Status: " + t.getStatus().name(),
                        "Token",
                        "/tokens"
                ))
                .toList();

        // 3. Search users (Only for ADMIN)
        List<SearchResultItem> userResults = List.of();
        if (currentUser.getRole() == Role.ADMIN) {
            userResults = userRepository.findAll().stream()
                    .filter(u -> u.getName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q))
                    .limit(5)
                    .map(u -> new SearchResultItem(
                            u.getId(),
                            u.getName(),
                            u.getEmail() + " (" + u.getRole().name() + ")",
                            "User",
                            "/users"
                    ))
                    .toList();
        }

        return new SearchResponse(certResults, tokenResults, userResults);
    }
}
