package com.microservices.auth.service;

import com.microservices.auth.dto.CreateAlunoAccountRequest;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.exception.ConflictException;
import com.microservices.auth.exception.UnauthorizedException;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock RegisterAdmClient registerAdmClient;
    @InjectMocks AuthService authService;

    @Test
    void criarAluno_deveCriarPerfilEContaVinculada() {
        var request = new CreateAlunoAccountRequest(
                "Aluno", "aluno@test.com", "password123", "MAT-1",
                "47999999999", "Rota 1", 1L, "BCC", "FURB");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(registerAdmClient.criarAluno(request, "corr-1")).thenReturn(42L);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.criarAluno(request, "corr-1");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_ALUNO);
        assertThat(captor.getValue().getProfileId()).isEqualTo(42L);
        assertThat(response.profileId()).isEqualTo(42L);
    }

    @Test
    void criarAluno_naoDeveCriarPerfilQuandoEmailExiste() {
        var request = new CreateAlunoAccountRequest(
                "Aluno", "aluno@test.com", "password123", "MAT-1",
                null, null, 1L, null, null);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.criarAluno(request, "corr"))
                .isInstanceOf(ConflictException.class);

        verify(registerAdmClient, never()).criarAluno(any(), any());
    }

    @Test
    void login_deveRetornarTokenParaCredenciaisValidas() {
        var request = new LoginRequest("aluno@test.com", "password123");
        var user = new User("Aluno", request.email(), "encoded", Role.ROLE_ALUNO, 42L);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        assertThat(authService.login(request).token()).isEqualTo("jwt-token");
    }

    @Test
    void login_deveRejeitarCredenciaisInvalidas() {
        var request = new LoginRequest("x@test.com", "wrong");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void gerarSenhaTemporaria_deveTrocarHashERetornarSenhaUmaVez() {
        var user = new User(
                "Aluno", "aluno@test.com", "old-hash", Role.ROLE_ALUNO, 42L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        var response = authService.gerarSenhaTemporaria(10L);

        assertThat(response.email()).isEqualTo("aluno@test.com");
        assertThat(response.temporaryPassword()).hasSize(14);
        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(passwordEncoder).encode(response.temporaryPassword());
        verify(userRepository).save(user);
    }
}
