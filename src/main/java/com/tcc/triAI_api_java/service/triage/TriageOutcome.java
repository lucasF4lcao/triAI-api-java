package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.fhir.ModelOutput;
import com.tcc.triAI_api_java.fhir.TriageInput;

public record TriageOutcome(TriageAnalysis analysis, TriageInput input, ModelOutput modelOutput) {
}
