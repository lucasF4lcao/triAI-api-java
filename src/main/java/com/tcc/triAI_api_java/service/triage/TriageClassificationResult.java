package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.enums.TriageColor;

public record TriageClassificationResult(TriageColor color, double confidence) {
}
