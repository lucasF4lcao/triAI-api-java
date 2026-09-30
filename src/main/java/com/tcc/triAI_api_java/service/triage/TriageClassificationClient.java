package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.fhir.ModelOutput;
import com.tcc.triAI_api_java.fhir.VitalSignsData;
import java.util.LinkedHashMap;
import java.util.Map;
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
                response.confianca(),
                modelOutputOf(response)
        );
    }

    private ModelOutput modelOutputOf(MlClassificationResponse response) {
        Map<TriageColor, Double> probabilities = new LinkedHashMap<>();

        if (response.probabilidades() != null) {
            response.probabilidades().forEach((label, probability) ->
                    probabilities.put(TriageColor.fromMlLabel(label), probability));
        }

        return new ModelOutput(probabilities, Boolean.TRUE.equals(response.regraDeSeguranca()));
    }
}
