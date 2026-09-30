package com.tcc.triAI_api_java.controller.fhir;

final class SwaggerExamples {

    static final String TRIAGE_BUNDLE = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "entry": [
                {"fullUrl": "urn:uuid:pac-1", "resource": {"resourceType": "Patient", "id": "pac-1", "gender": "male", "birthDate": "1970-01-01"}},
                {"resource": {"resourceType": "Observation", "id": "o-hr", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "8867-4"}]},
                  "valueQuantity": {"value": 110, "unit": "/min", "system": "http://unitsofmeasure.org", "code": "/min"}}},
                {"resource": {"resourceType": "Observation", "id": "o-spo2", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "59408-5"}]},
                  "valueQuantity": {"value": 93, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%"}}},
                {"resource": {"resourceType": "Observation", "id": "o-temp", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "8310-5"}]},
                  "valueQuantity": {"value": 38.4, "unit": "Cel", "system": "http://unitsofmeasure.org", "code": "Cel"}}},
                {"resource": {"resourceType": "Observation", "id": "o-bp", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9"}]},
                  "component": [
                    {"code": {"coding": [{"system": "http://loinc.org", "code": "8480-6"}]}, "valueQuantity": {"value": 95, "unit": "mm[Hg]"}},
                    {"code": {"coding": [{"system": "http://loinc.org", "code": "8462-4"}]}, "valueQuantity": {"value": 60, "unit": "mm[Hg]"}}]}},
                {"resource": {"resourceType": "Observation", "id": "o-pain", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "72514-3"}]}, "valueInteger": 7}},
                {"resource": {"resourceType": "Encounter", "id": "enc-1", "status": "triaged",
                  "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "EMER"},
                  "extension": [{"url": "https://triai.local/fhir/StructureDefinition/arrival-mode", "valueCode": "ambulance"}],
                  "reasonCode": [{"coding": [{"system": "https://triai.local/fhir/CodeSystem/chief-complaints", "code": "chest-pain"}]}]}}
              ]
            }
            """;

    static final String AMEND_PARAMETERS = """
            {
              "resourceType": "Parameters",
              "parameter": [
                {"name": "finalColor", "valueCoding": {"system": "https://triai.local/fhir/CodeSystem/manchester-triage", "code": "red"}},
                {"name": "reason", "valueString": "Piora clínica no exame físico"}
              ]
            }
            """;

    private SwaggerExamples() {
    }
}
