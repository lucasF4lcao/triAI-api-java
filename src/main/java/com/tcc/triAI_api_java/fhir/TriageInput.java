package com.tcc.triAI_api_java.fhir;

import java.util.List;

public record TriageInput(
        VitalSignsData data,
        String patientReference,
        String encounterReference,
        List<String> basisReferences,
        List<String> warnings
) {

    public static TriageInput empty() {
        return new TriageInput(new VitalSignsData(), null, null, List.of(), List.of());
    }
}
