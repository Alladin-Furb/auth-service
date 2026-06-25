package com.microservices.auth.controller;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.CreateAdminRequest;
import com.microservices.auth.dto.CreateAlunoAccountRequest;
import com.microservices.auth.dto.CreateMotoristaAccountRequest;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.TemporaryPasswordResponse;
import com.microservices.auth.dto.UserResponse;
import com.microservices.auth.entity.Role;
import com.microservices.auth.exception.BusinessException;
import com.microservices.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/users/alunos")
    public ResponseEntity<UserResponse> criarAluno(
            @Valid @RequestBody CreateAlunoAccountRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader("X-User-Role") String requesterRole) {
        exigirAdmin(requesterRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.criarAluno(request, correlationId));
    }

    @PostMapping("/users/motoristas")
    public ResponseEntity<UserResponse> criarMotorista(
            @Valid @RequestBody CreateMotoristaAccountRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader("X-User-Role") String requesterRole) {
        exigirAdmin(requesterRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.criarMotorista(request, correlationId));
    }

    @PostMapping("/users/admins")
    public ResponseEntity<UserResponse> criarAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            @RequestHeader("X-User-Role") String requesterRole) {
        exigirAdmin(requesterRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.criarAdmin(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listarUsuarios(
            @RequestHeader("X-User-Role") String requesterRole) {
        exigirAdmin(requesterRole);
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @PostMapping("/users/{id}/temporary-password")
    public ResponseEntity<TemporaryPasswordResponse> gerarSenhaTemporaria(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String requesterRole) {
        exigirAdmin(requesterRole);
        return ResponseEntity.ok(authService.gerarSenhaTemporaria(id));
    }

    private void exigirAdmin(String requesterRole) {
        if (!Role.ROLE_ADMIN.name().equals(requesterRole)) {
            throw new BusinessException("Insufficient permissions", HttpStatus.FORBIDDEN);
        }
    }
}
