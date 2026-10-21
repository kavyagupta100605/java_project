package com.keybridge.controller;

import com.keybridge.dto.token.AssignTokenRequest;
import com.keybridge.dto.token.CreateTokenRequest;
import com.keybridge.dto.token.TokenResponse;
import com.keybridge.dto.token.UpdateTokenStatusRequest;
import com.keybridge.entity.User;
import com.keybridge.service.AuthService;
import com.keybridge.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService tokenService;
    private final AuthService authService;

    public TokenController(TokenService tokenService, AuthService authService) {
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<TokenResponse>> listTokens() {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(tokenService.listTokens(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TokenResponse> getToken(@PathVariable Long id) {
        return ResponseEntity.ok(tokenService.getToken(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> createToken(@Valid @RequestBody CreateTokenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenService.createToken(request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> assignToken(
            @PathVariable Long id,
            @Valid @RequestBody AssignTokenRequest request
    ) {
        return ResponseEntity.ok(tokenService.assignToken(id, request.userId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTokenStatusRequest request
    ) {
        return ResponseEntity.ok(tokenService.updateStatus(id, request.status()));
    }
}
