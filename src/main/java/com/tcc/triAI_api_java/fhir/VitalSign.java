package com.tcc.triAI_api_java.fhir;

public enum VitalSign {
    HEART_RATE("frequência cardíaca"),
    RESPIRATORY_RATE("frequência respiratória"),
    OXYGEN_SATURATION("saturação de O2"),
    TEMPERATURE("temperatura"),
    SYSTOLIC_PRESSURE("pressão sistólica"),
    DIASTOLIC_PRESSURE("pressão diastólica"),
    PAIN("dor");

    private final String label;

    VitalSign(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
