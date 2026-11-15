package com.tcc.triAI_api_java.service.triage;

import java.util.Map;

public record MlClassificationResponse(
        String corPrevista,
        Double confianca,
        Map<String, Double> probabilidades,
        Boolean regraDeSeguranca
) {
}
