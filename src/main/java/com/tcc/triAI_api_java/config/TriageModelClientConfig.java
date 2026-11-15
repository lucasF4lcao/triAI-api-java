package com.tcc.triAI_api_java.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TriageModelClientConfig {

    @Bean
    public RestClient triageModelRestClient(
            @Value("${triage-model.base-url}") String baseUrl,
            @Value("${triage-model.timeout-seconds}") long timeoutSeconds
    ) {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.of(timeoutSeconds, ChronoUnit.SECONDS))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.of(timeoutSeconds, ChronoUnit.SECONDS));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
