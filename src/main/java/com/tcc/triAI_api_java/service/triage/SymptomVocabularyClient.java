package com.tcc.triAI_api_java.service.triage;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SymptomVocabularyClient {

    private final RestClient triageModelRestClient;

    public SymptomVocabularyClient(RestClient triageModelRestClient) {
        this.triageModelRestClient = triageModelRestClient;
    }

    public List<MlSymptom> fetch() {
        List<MlSymptom> symptoms = triageModelRestClient.get()
                .uri("/sintomas")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (symptoms == null) {
            throw new IllegalStateException("Triage model returned an empty symptom vocabulary");
        }

        return symptoms;
    }
}
