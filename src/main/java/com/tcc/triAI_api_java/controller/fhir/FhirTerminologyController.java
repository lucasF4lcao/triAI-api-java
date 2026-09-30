package com.tcc.triAI_api_java.controller.fhir;

import com.tcc.triAI_api_java.exception.ResourceNotFoundException;
import com.tcc.triAI_api_java.fhir.ChiefComplaintVocabulary;
import com.tcc.triAI_api_java.fhir.ComplaintConceptMap;
import com.tcc.triAI_api_java.fhir.ManchesterCodes;
import com.tcc.triAI_api_java.fhir.ServerDefinitions;
import com.tcc.triAI_api_java.service.triage.ChiefComplaintTerminologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/fhir", produces = {FhirJson.MEDIA_TYPE, "application/json"})
public class FhirTerminologyController {

    private final ChiefComplaintTerminologyService terminology;
    private final ComplaintConceptMap conceptMap;
    private final ServerDefinitions definitions;
    private final FhirJson fhirJson;

    public FhirTerminologyController(
            ChiefComplaintTerminologyService terminology,
            ComplaintConceptMap conceptMap,
            ServerDefinitions definitions,
            FhirJson fhirJson
    ) {
        this.terminology = terminology;
        this.conceptMap = conceptMap;
        this.definitions = definitions;
        this.fhirJson = fhirJson;
    }

    @GetMapping("/metadata")
    public ResponseEntity<String> metadata() {
        return fhirJson.ok(definitions.capabilityStatement());
    }

    @GetMapping("/ValueSet/{id}/$expand")
    public ResponseEntity<String> expand(@PathVariable String id) {
        return fhirJson.ok(valueSetOf(id));
    }

    @GetMapping("/ValueSet/{id}")
    public ResponseEntity<String> readValueSet(@PathVariable String id) {
        return fhirJson.ok(valueSetOf(id));
    }

    @GetMapping("/CodeSystem/{id}")
    public ResponseEntity<String> readCodeSystem(@PathVariable String id) {
        return switch (id) {
            case ChiefComplaintVocabulary.VALUE_SET_ID -> fhirJson.ok(terminology.codeSystem());
            case ManchesterCodes.CODE_SYSTEM_ID -> fhirJson.ok(ManchesterCodes.codeSystem());
            default -> throw notFound("CodeSystem", id);
        };
    }

    @GetMapping("/ConceptMap/{id}")
    public ResponseEntity<String> readConceptMap(@PathVariable String id) {
        if (!conceptMap.resource().getIdElement().getIdPart().equals(id)) {
            throw notFound("ConceptMap", id);
        }
        return fhirJson.ok(conceptMap.resource());
    }

    @GetMapping("/OperationDefinition/{id}")
    public ResponseEntity<String> readOperationDefinition(@PathVariable String id) {
        return switch (id) {
            case ServerDefinitions.TRIAGE_OPERATION_ID -> fhirJson.ok(definitions.triageOperation());
            case ServerDefinitions.AMEND_OPERATION_ID -> fhirJson.ok(definitions.amendOperation());
            default -> throw notFound("OperationDefinition", id);
        };
    }

    private org.hl7.fhir.r4.model.ValueSet valueSetOf(String id) {
        if (!ChiefComplaintVocabulary.VALUE_SET_ID.equals(id)) {
            throw notFound("ValueSet", id);
        }
        return terminology.valueSet();
    }

    private ResourceNotFoundException notFound(String type, String id) {
        return new ResourceNotFoundException(type + "/" + id + " não encontrado");
    }
}
