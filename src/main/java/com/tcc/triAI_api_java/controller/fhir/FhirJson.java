package com.tcc.triAI_api_java.controller.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class FhirJson {

    public static final String MEDIA_TYPE = "application/fhir+json";

    private final FhirContext fhirContext;

    public FhirJson(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public ResponseEntity<String> ok(IBaseResource resource) {
        return respond(HttpStatus.OK, resource);
    }

    public ResponseEntity<String> respond(HttpStatus status, IBaseResource resource) {
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType(MEDIA_TYPE + ";charset=UTF-8"))
                .body(fhirContext.newJsonParser().encodeResourceToString(resource));
    }

    public <T extends IBaseResource> T parse(String body, Class<T> type) {
        return fhirContext.newJsonParser().parseResource(type, body);
    }
}
