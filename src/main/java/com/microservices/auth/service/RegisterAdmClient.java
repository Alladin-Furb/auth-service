package com.microservices.auth.service;

import com.microservices.auth.dto.CreateAlunoAccountRequest;
import com.microservices.auth.dto.CreateMotoristaAccountRequest;
import com.microservices.auth.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class RegisterAdmClient {

    private final RestClient restClient;
    private final String baseUrl;

    public RegisterAdmClient(
            RestClient registerAdmRestClient,
            @Value("${services.register-adm.url}") String baseUrl) {
        this.restClient = registerAdmRestClient;
        this.baseUrl = baseUrl;
    }

    public Long criarAluno(CreateAlunoAccountRequest request, String correlationId) {
        return criarPerfil("/admin/alunos", Map.of(
                "matricula", request.matricula(),
                "nome", request.name(),
                "email", request.email(),
                "telefone", request.telefone() == null ? "" : request.telefone(),
                "rotaTransporte", request.rotaTransporte() == null ? "" : request.rotaTransporte(),
                "cursoId", request.cursoId(),
                "nomeCurso", request.nomeCurso() == null ? "" : request.nomeCurso(),
                "faculdade", request.faculdade() == null ? "" : request.faculdade(),
                "confirmouPresenca", false
        ), correlationId);
    }

    public Long criarMotorista(CreateMotoristaAccountRequest request, String correlationId) {
        return criarPerfil("/admin/motoristas", Map.of(
                "nome", request.name(),
                "carteiraMotorista", request.carteiraMotorista()
        ), correlationId);
    }

    private Long criarPerfil(String path, Object body, String correlationId) {
        try {
            ProfileResponse response = restClient.post()
                    .uri(baseUrl + path)
                    .header("X-Correlation-Id", correlationId == null ? "" : correlationId)
                    .body(body)
                    .retrieve()
                    .body(ProfileResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(
                        "RegisterAdm returned an invalid profile",
                        HttpStatus.BAD_GATEWAY);
            }
            return response.id();
        } catch (RestClientException ex) {
            throw new BusinessException(
                    "Could not create profile in RegisterAdm",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private record ProfileResponse(Long id) {
    }
}
