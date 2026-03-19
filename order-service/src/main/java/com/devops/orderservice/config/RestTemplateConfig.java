package com.devops.orderservice.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Interceptor to forward correlation ID to downstream services
        ClientHttpRequestInterceptor correlationInterceptor = (request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                request.getHeaders().set("X-Correlation-Id", correlationId);
            }
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(correlationInterceptor));
        return restTemplate;
    }
}
