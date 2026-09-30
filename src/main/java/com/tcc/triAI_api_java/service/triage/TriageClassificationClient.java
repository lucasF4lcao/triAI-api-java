package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.fhir.VitalSignsData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TriageClassificationClient {

    private final RestClient triageModelRestClient;

    public TriageClassificationClient(RestClient triageModelRestClient) {
        this.triageModelRestClient = triageModelRestClient;
    }

    public TriageClassificationResult classify(VitalSignsData vitalSigns) {
        MlClassificationResponse response = triageModelRestClient.post()
                .uri("/classificar")
                .body(vitalSigns.toClassificationPayload())
                .retrieve()
                .body(MlClassificationResponse.class);

        if (response == null || response.corPrevista() == null || response.confianca() == null) {
            throw new IllegalStateException("Triage model returned an unexpected response");
        }

        return new TriageClassificationResult(
                TriageColor.fromMlLabel(response.corPrevista()),
                response.confianca()
        );
    }
}
