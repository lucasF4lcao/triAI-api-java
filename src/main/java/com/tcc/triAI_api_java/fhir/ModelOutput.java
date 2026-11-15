package com.tcc.triAI_api_java.fhir;

import com.tcc.triAI_api_java.enums.TriageColor;
import java.util.Map;

public record ModelOutput(Map<TriageColor, Double> probabilities, boolean safetyRuleApplied) {

    public static ModelOutput none() {
        return new ModelOutput(Map.of(), false);
    }
}
