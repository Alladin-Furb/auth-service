package com.microservices.auth.service;

import com.microservices.auth.dto.CreateAlunoAccountRequest;
import com.microservices.auth.dto.CreateMotoristaAccountRequest;
import com.microservices.auth.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public UUID criarAluno(CreateAlunoAccountRequest request, String correlationId) {
        Map<String, Object> body = new HashMap<>();
        body.put("matricula", request.matricula());
        body.put("cpf", request.cpf());
        body.put("nome", request.name());
        body.put("email", request.email());
        body.put("telefone", request.telefone() == null ? "" : request.telefone());
        body.put("rotaTransporte", request.rotaTransporte() == null ? "" : request.rotaTransporte());
        body.put("cursoId", request.cursoId());
        body.put("nomeCurso", request.nomeCurso() == null ? "" : request.nomeCurso());
        body.put("faculdade", request.faculdade() == null ? "" : request.faculdade());
        body.put("confirmouPresenca", false);
        return criarPerfil("/admin/alunos", body, correlationId);
    }

    public UUID criarMotorista(CreateMotoristaAccountRequest request, String correlationId) {
        return criarPerfil("/admin/motoristas", Map.of(
                "nome", request.name(),
                "carteiraMotorista", request.carteiraMotorista()
        ), correlationId);
    }

    private UUID criarPerfil(String path, Object body, String correlationId) {
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

    private record ProfileResponse(UUID id) {
    }
}
