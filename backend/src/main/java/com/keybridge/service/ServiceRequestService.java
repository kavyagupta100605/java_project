package com.keybridge.service;

import com.keybridge.dto.cert.IssueCertRequest;
import com.keybridge.dto.request.CreateRequestDto;
import com.keybridge.dto.request.ServiceRequestResponse;
import com.keybridge.dto.request.UpdateRequestStatusDto;
import com.keybridge.entity.Certificate;
import com.keybridge.entity.ServiceRequest;
import com.keybridge.entity.Token;
import com.keybridge.entity.User;
import com.keybridge.enums.*;
import com.keybridge.exception.BadRequestException;
import com.keybridge.exception.ForbiddenException;
import com.keybridge.exception.InvalidStateTransitionException;
import com.keybridge.exception.NotFoundException;
import com.keybridge.repository.CertificateRepository;
import com.keybridge.repository.ServiceRequestRepository;
import com.keybridge.repository.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository serviceRequestRepository;
    private final CertificateRepository certificateRepository;
    private final TokenRepository tokenRepository;
    private final CertificateService certificateService;

    public ServiceRequestService(
            ServiceRequestRepository serviceRequestRepository,
            CertificateRepository certificateRepository,
            TokenRepository tokenRepository,
            CertificateService certificateService
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.certificateRepository = certificateRepository;
        this.tokenRepository = tokenRepository;
        this.certificateService = certificateService;
    }

    @Transactional
    public ServiceRequestResponse createRequest(CreateRequestDto dto, User currentUser) {
        if (dto.type() == RequestType.RENEW_CERT || dto.type() == RequestType.REVOKE_CERT) {
            if (dto.certId() == null) {
                throw new BadRequestException("certId is required for " + dto.type().name());
            }
            Certificate cert = certificateRepository.findById(dto.certId())
                    .orElseThrow(() -> new NotFoundException("Certificate not found with ID: " + dto.certId()));
            if (!cert.getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You do not own this certificate");
            }
        }

        ServiceRequest request = new ServiceRequest();
        request.setUser(currentUser);
        request.setCertId(dto.certId());
        request.setType(dto.type());
        request.setStatus(RequestStatus.PENDING);
        request.setRemarks(dto.remarks());

        serviceRequestRepository.save(request);
        return ServiceRequestResponse.fromEntity(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listRequests(User currentUser) {
        List<ServiceRequest> list = (currentUser.getRole() == Role.USER)
                ? serviceRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                : serviceRequestRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(ServiceRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getRequest(Long id, User currentUser) {
        ServiceRequest req = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found with ID: " + id));

        if (currentUser.getRole() == Role.USER && !req.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied to this request");
        }

        return ServiceRequestResponse.fromEntity(req);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long id, UpdateRequestStatusDto dto, User adminUser) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can update request status");
        }

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found with ID: " + id));

        RequestStatus currentStatus = request.getStatus();
        RequestStatus targetStatus = dto.status();

        // State Machine validation
        boolean validTransition = false;
        if (currentStatus == RequestStatus.PENDING) {
            validTransition = (targetStatus == RequestStatus.APPROVED || targetStatus == RequestStatus.REJECTED);
        } else if (currentStatus == RequestStatus.APPROVED) {
            validTransition = (targetStatus == RequestStatus.COMPLETED);
        }

        if (!validTransition) {
            throw new InvalidStateTransitionException(
                    "Invalid state transition from " + currentStatus + " to " + targetStatus
            );
        }

        request.setStatus(targetStatus);
        if (dto.adminNote() != null) {
            request.setAdminNote(dto.adminNote().trim());
        }

        // On COMPLETED, perform the action
        if (targetStatus == RequestStatus.COMPLETED) {
            executeCompletionAction(request);
        }

        serviceRequestRepository.save(request);
        return ServiceRequestResponse.fromEntity(request);
    }

    private void executeCompletionAction(ServiceRequest request) {
        User user = request.getUser();
        switch (request.getType()) {
            case RENEW_CERT -> {
                if (request.getCertId() != null) {
                    Certificate oldCert = certificateRepository.findById(request.getCertId()).orElse(null);
                    if (oldCert != null) {
                        oldCert.setStatus(CertStatus.EXPIRED);
                        certificateRepository.save(oldCert);

                        // Issue new test certificate
                        certificateService.issueTestCertificate(new IssueCertRequest(
                                user.getId(),
                                user.getName() + " (Renewed)",
                                365,
                                oldCert.getToken() != null ? oldCert.getToken().getId() : null
                        ));
                    }
                }
            }
            case REVOKE_CERT -> {
                if (request.getCertId() != null) {
                    Certificate cert = certificateRepository.findById(request.getCertId()).orElse(null);
                    if (cert != null) {
                        cert.setStatus(CertStatus.REVOKED);
                        cert.setRevokedAt(LocalDateTime.now());
                        cert.setRevokeReason("Revoked via service request #" + request.getId());
                        certificateRepository.save(cert);
                    }
                }
            }
            case NEW_TOKEN -> {
                List<Token> available = tokenRepository.findByStatus(TokenStatus.AVAILABLE);
                if (!available.isEmpty()) {
                    Token t = available.get(0);
                    t.setOwner(user);
                    t.setStatus(TokenStatus.ASSIGNED);
                    t.setAssignedDate(LocalDate.now());
                    tokenRepository.save(t);
                } else {
                    log.warn("No available tokens found to assign for request #{}", request.getId());
                }
            }
            case LOST_TOKEN -> {
                List<Token> userTokens = tokenRepository.findByOwnerId(user.getId());
                for (Token t : userTokens) {
                    t.setStatus(TokenStatus.LOST);
                }
                tokenRepository.saveAll(userTokens);

                // Revoke certificates linked to user
                List<Certificate> userCerts = certificateRepository.findByOwnerId(user.getId());
                for (Certificate c : userCerts) {
                    if (c.getStatus() != CertStatus.REVOKED) {
                        c.setStatus(CertStatus.REVOKED);
                        c.setRevokedAt(LocalDateTime.now());
                        c.setRevokeReason("Token reported lost via service request #" + request.getId());
                    }
                }
                certificateRepository.saveAll(userCerts);
            }
        }
    }
}
