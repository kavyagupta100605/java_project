package com.keybridge.controller;

import com.keybridge.dto.request.CreateRequestDto;
import com.keybridge.dto.request.ServiceRequestResponse;
import com.keybridge.dto.request.UpdateRequestStatusDto;
import com.keybridge.entity.User;
import com.keybridge.service.AuthService;
import com.keybridge.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final ServiceRequestService serviceRequestService;
    private final AuthService authService;

    public RequestController(ServiceRequestService serviceRequestService, AuthService authService) {
        this.serviceRequestService = serviceRequestService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> createRequest(@Valid @RequestBody CreateRequestDto dto) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRequestService.createRequest(dto, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> listRequests() {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(serviceRequestService.listRequests(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getRequest(@PathVariable Long id) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(serviceRequestService.getRequest(id, currentUser));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestStatusDto dto
    ) {
        User adminUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(serviceRequestService.updateStatus(id, dto, adminUser));
    }
}
