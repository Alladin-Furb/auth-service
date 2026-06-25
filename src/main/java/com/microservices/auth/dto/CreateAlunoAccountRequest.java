package com.microservices.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAlunoAccountRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String matricula,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos") String cpf,
        String telefone,
        String rotaTransporte,
        UUID cursoId,
        String nomeCurso,
        String faculdade
) {
}
