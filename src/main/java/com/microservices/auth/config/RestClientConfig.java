package com.microservices.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient registerAdmRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
