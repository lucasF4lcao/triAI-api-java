package com.tcc.triAI_api_java.fhir;

public final class TriageExtensions {

    private static final String BASE = "https://triai.local/fhir/StructureDefinition/";

    public static final String COLOR_PROBABILITY = BASE + "color-probability";
    public static final String SAFETY_RULE = BASE + "safety-rule";

    private TriageExtensions() {
    }
}
