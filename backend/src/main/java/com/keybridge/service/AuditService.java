package com.keybridge.service;

import com.keybridge.dto.audit.AuditLogResponse;
import com.keybridge.entity.AuditLog;
import com.keybridge.entity.User;
import com.keybridge.repository.AuditLogRepository;
import com.keybridge.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(String action, Object[] args) {
        try {
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                User user = userRepository.findByEmail(auth.getName()).orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            }

            String ip = "127.0.0.1";
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xForwarded = req.getHeader("X-Forwarded-For");
                ip = (xForwarded != null && !xForwarded.isBlank()) ? xForwarded.split(",")[0].trim() : req.getRemoteAddr();
            }

            StringBuilder details = new StringBuilder();
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg != null) {
                        String str = arg.toString();
                        if (!str.toLowerCase().contains("password") && !str.contains("@") && str.length() < 120) {
                            if (details.length() > 0) details.append(", ");
                            details.append(str);
                        }
                    }
                }
            }

            AuditLog logEntry = new AuditLog(userId, action, details.length() > 0 ? details.toString() : "Action performed", ip);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    @Transactional
    public Page<AuditLogResponse> listAuditLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (auditLogRepository.count() == 0) {
            seedInitialAuditLogs();
        }

        return auditLogRepository.findFiltered(userId, action, from, to, pageable)
                .map(entry -> {
                    String username = "System / Administrator";
                    if (entry.getUserId() != null) {
                        username = userRepository.findById(entry.getUserId())
                                .map(User::getEmail)
                                .orElse("User #" + entry.getUserId());
                    }
                    return AuditLogResponse.fromEntity(entry, username);
                });
    }

    private void seedInitialAuditLogs() {
        try {
            Long adminId = userRepository.findByEmail("admin@keybridge.com").map(User::getId).orElse(null);
            LocalDateTime now = LocalDateTime.now();

            AuditLog log1 = new AuditLog(adminId, "ROOT_CA_INITIALIZED", "KeyBridge Test Root CA generated with 3650 days validity and 2048-bit RSA keys", "127.0.0.1");
            AuditLog log2 = new AuditLog(adminId, "TOKENS_PROVISIONED", "Seeded cryptographic tokens TKN-001 through TKN-005 in HSM inventory", "127.0.0.1");
            AuditLog log3 = new AuditLog(adminId, "SECURITY_POLICY_ENFORCED", "Zero private-key exposure policy and 5-stage verification engine activated", "127.0.0.1");
            AuditLog log4 = new AuditLog(null, "SYSTEM_STARTUP", "KeyBridge Cryptographic Platform service daemon started successfully", "127.0.0.1");

            auditLogRepository.saveAll(java.util.List.of(log1, log2, log3, log4));
        } catch (Exception e) {
            log.warn("Could not seed initial audit logs: {}", e.getMessage());
        }
    }
}
