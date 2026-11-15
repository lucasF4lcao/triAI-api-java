package com.tcc.triAI_api_java.fhir;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VitalSignsData {

    private Integer age;
    private Integer heartRate;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer oxygenSaturation;
    private BigDecimal temperature;
    private Integer respiratoryRate;
    private Integer pain;
    private Boolean male;
    private Boolean ambulance;
    private String chiefComplaint;
    private List<String> symptomCodes = new ArrayList<>();

    public Map<String, Object> toClassificationPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        // Chaves no contrato da API de ML (triAI-ml, POST /classificar). `age` é aceito mas ignorado pelo modelo.
        payload.put("idade", age);
        payload.put("frequencia_cardiaca", heartRate);
        payload.put("pressao_sistolica", systolicPressure);
        payload.put("pressao_diastolica", diastolicPressure);
        payload.put("saturacao_o2", oxygenSaturation);
        payload.put("temperatura", temperature);
        payload.put("frequencia_respiratoria", respiratoryRate);
        payload.put("dor", pain);
        payload.put("sexo_masculino", male);
        payload.put("queixa_principal", chiefComplaint);
        payload.put("sintomas", symptomCodes);
        payload.put("ambulancia", ambulance);
        return payload;
    }
}
