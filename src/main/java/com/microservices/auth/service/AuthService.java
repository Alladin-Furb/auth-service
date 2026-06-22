package com.microservices.auth.service;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.CreateAdminRequest;
import com.microservices.auth.dto.CreateAlunoAccountRequest;
import com.microservices.auth.dto.CreateMotoristaAccountRequest;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.TemporaryPasswordResponse;
import com.microservices.auth.dto.UserResponse;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.exception.BusinessException;
import com.microservices.auth.exception.ConflictException;
import com.microservices.auth.exception.UnauthorizedException;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class AuthService {

    private static final String TEMPORARY_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final int TEMPORARY_PASSWORD_LENGTH = 14;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RegisterAdmClient registerAdmClient;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RegisterAdmClient registerAdmClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.registerAdmClient = registerAdmClient;
    }

    @Transactional
    public UserResponse criarAluno(CreateAlunoAccountRequest request, String correlationId) {
        validarEmailDisponivel(request.email());
        Long profileId = registerAdmClient.criarAluno(request, correlationId);
        return salvarUsuario(
                request.name(), request.email(), request.password(),
                Role.ROLE_ALUNO, profileId);
    }

    @Transactional
    public UserResponse criarMotorista(
            CreateMotoristaAccountRequest request,
            String correlationId) {
        validarEmailDisponivel(request.email());
        Long profileId = registerAdmClient.criarMotorista(request, correlationId);
        return salvarUsuario(
                request.name(), request.email(), request.password(),
                Role.ROLE_MOTORISTA, profileId);
    }

    @Transactional
    public UserResponse criarAdmin(CreateAdminRequest request) {
        validarEmailDisponivel(request.email());
        return salvarUsuario(
                request.name(), request.email(), request.password(),
                Role.ROLE_ADMIN, null);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> listarUsuarios() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        return new AuthResponse(jwtUtil.generateToken(user));
    }

    @Transactional
    public TemporaryPasswordResponse gerarSenhaTemporaria(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "User not found", HttpStatus.NOT_FOUND));

        String temporaryPassword = gerarSenhaAleatoria();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);

        return new TemporaryPasswordResponse(
                user.getId(), user.getEmail(), temporaryPassword);
    }

    private void validarEmailDisponivel(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
    }

    private UserResponse salvarUsuario(
            String name,
            String email,
            String password,
            Role role,
            Long profileId) {
        User user = new User(
                name,
                email,
                passwordEncoder.encode(password),
                role,
                profileId
        );
        return UserResponse.from(userRepository.save(user));
    }

    private String gerarSenhaAleatoria() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
            password.append(TEMPORARY_PASSWORD_CHARS.charAt(
                    SECURE_RANDOM.nextInt(TEMPORARY_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
