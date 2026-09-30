package com.tcc.triAI_api_java.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class FhirBundles {

    private final List<String> entries = new ArrayList<>();

    static FhirBundles bundle() {
        return new FhirBundles();
    }

    FhirBundles patient(String id, String gender, String birthDate) {
        entries.add("{\"fullUrl\": \"urn:uuid:" + id + "\", \"resource\": {\"resourceType\": \"Patient\", \"id\": \"" + id
                + "\", \"gender\": \"" + gender + "\", \"birthDate\": \"" + birthDate + "\"}}");
        return this;
    }

    FhirBundles encounter(String body) {
        entries.add("{\"resource\": {\"resourceType\": \"Encounter\", \"id\": \"enc-1\", \"status\": \"triaged\","
                + "\"class\": {\"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\", \"code\": \"EMER\"}" + body + "}}");
        return this;
    }

    FhirBundles observation(String system, String code, String value, String unit) {
        return observation("obs-" + code, "final", system, code, value, unit, null);
    }

    FhirBundles observation(String id, String status, String system, String code, String value, String unit, String effective) {
        String unitJson = unit == null ? "" : ", \"unit\": \"" + unit + "\", \"system\": \"http://unitsofmeasure.org\", \"code\": \"" + unit + "\"";
        String effectiveJson = effective == null ? "" : ", \"effectiveDateTime\": \"" + effective + "\"";
        entries.add("{\"resource\": {\"resourceType\": \"Observation\", \"id\": \"" + id + "\", \"status\": \"" + status + "\","
                + "\"code\": {\"coding\": [{\"system\": \"" + system + "\", \"code\": \"" + code + "\"}]}"
                + effectiveJson + ", \"valueQuantity\": {\"value\": " + value + unitJson + "}}}");
        return this;
    }

    FhirBundles raw(String entry) {
        entries.add(entry);
        return this;
    }

    String json() {
        return "{\"resourceType\": \"Bundle\", \"type\": \"collection\", \"entry\": ["
                + entries.stream().collect(Collectors.joining(",")) + "]}";
    }
}
