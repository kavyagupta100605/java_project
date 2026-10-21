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

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findFiltered(userId, action, from, to, pageable)
                .map(AuditLogResponse::fromEntity);
    }
}
