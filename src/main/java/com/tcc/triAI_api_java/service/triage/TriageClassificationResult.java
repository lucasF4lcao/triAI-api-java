package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.fhir.ModelOutput;

public record TriageClassificationResult(TriageColor color, double confidence, ModelOutput modelOutput) {
}
