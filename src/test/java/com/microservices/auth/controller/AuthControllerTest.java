package com.microservices.auth.controller;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.TemporaryPasswordResponse;
import com.microservices.auth.dto.UserResponse;
import com.microservices.auth.entity.Role;
import com.microservices.auth.exception.GlobalExceptionHandler;
import com.microservices.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void login_deveRetornarToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void criarAluno_deveExigirAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/users/alunos")
                        .header("X-User-Role", "ROLE_ALUNO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Aluno",
                                  "email":"aluno@test.com",
                                  "password":"password123",
                                  "matricula":"MAT-1",
                                  "cursoId":1
                                }"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void criarAluno_deveRetornarContaVinculadaParaAdmin() throws Exception {
        when(authService.criarAluno(any(), eq("corr-1")))
                .thenReturn(new UserResponse(
                        10L, "Aluno", "aluno@test.com", Role.ROLE_ALUNO, 42L));

        mockMvc.perform(post("/api/auth/users/alunos")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Aluno",
                                  "email":"aluno@test.com",
                                  "password":"password123",
                                  "matricula":"MAT-1",
                                  "cursoId":1
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_ALUNO"))
                .andExpect(jsonPath("$.profileId").value(42));

        verify(authService).criarAluno(any(), eq("corr-1"));
    }

    @Test
    void gerarSenhaTemporaria_deveExigirAdminERetornarSenha() throws Exception {
        when(authService.gerarSenhaTemporaria(10L))
                .thenReturn(new TemporaryPasswordResponse(
                        10L, "aluno@test.com", "Temp#123456789"));

        mockMvc.perform(post("/api/auth/users/10/temporary-password")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.email").value("aluno@test.com"))
                .andExpect(jsonPath("$.temporaryPassword").value("Temp#123456789"));
    }
}
