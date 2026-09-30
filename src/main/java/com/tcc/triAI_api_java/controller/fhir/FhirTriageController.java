package com.tcc.triAI_api_java.controller.fhir;

import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.exception.ValidationException;
import com.tcc.triAI_api_java.fhir.RiskAssessmentMapper;
import com.tcc.triAI_api_java.fhir.TriageInput;
import com.tcc.triAI_api_java.fhir.ManchesterCodes;
import com.tcc.triAI_api_java.service.triage.CreateTriageAnalysisService;
import com.tcc.triAI_api_java.service.triage.TriageOutcome;
import com.tcc.triAI_api_java.service.triage.TriageReviewService;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir")
public class FhirTriageController {

    private static final String[] MEDIA_TYPES = {FhirJson.MEDIA_TYPE, "application/json"};
    private static final int MAX_REASON_LENGTH = 500;

    private final CreateTriageAnalysisService createService;
    private final TriageReviewService reviewService;
    private final RiskAssessmentMapper mapper;
    private final FhirJson fhirJson;

    public FhirTriageController(
            CreateTriageAnalysisService createService,
            TriageReviewService reviewService,
            RiskAssessmentMapper mapper,
            FhirJson fhirJson
    ) {
        this.createService = createService;
        this.reviewService = reviewService;
        this.mapper = mapper;
        this.fhirJson = fhirJson;
    }

    @PostMapping(value = "/$triage", consumes = {FhirJson.MEDIA_TYPE, "application/json"}, produces = {FhirJson.MEDIA_TYPE, "application/json"})
    public ResponseEntity<String> triage(@RequestBody String body) {
        TriageOutcome outcome = createService.run(body);
        return fhirJson.ok(mapper.map(outcome.analysis(), List.of(), outcome.input()));
    }

    @GetMapping(value = "/RiskAssessment/{id}", produces = {FhirJson.MEDIA_TYPE, "application/json"})
    public ResponseEntity<String> read(@PathVariable Long id) {
        TriageReviewService.Review review = reviewService.find(id);
        return fhirJson.ok(mapper.map(review.analysis(), review.changesOldestFirst(), TriageInput.empty()));
    }

    @PostMapping(value = "/RiskAssessment/{id}/$amend", consumes = {FhirJson.MEDIA_TYPE, "application/json"}, produces = {FhirJson.MEDIA_TYPE, "application/json"})
    public ResponseEntity<String> amend(@PathVariable Long id, @RequestBody String body) {
        Parameters parameters = fhirJson.parse(body, Parameters.class);

        TriageColor finalColor = finalColorOf(parameters);
        String reason = reasonOf(parameters);

        TriageReviewService.Review review = reviewService.amend(id, finalColor, reason);
        return fhirJson.ok(mapper.map(review.analysis(), review.changesOldestFirst(), TriageInput.empty()));
    }

    private TriageColor finalColorOf(Parameters parameters) {
        var parameter = parameters.getParameter("finalColor");
        String code = null;

        if (parameter != null && parameter.getValue() instanceof Coding coding) {
            code = coding.getCode();
        } else if (parameter != null && parameter.hasValue()) {
            code = parameter.getValue().primitiveValue();
        }

        if (code == null || code.isBlank()) {
            throw ValidationException.of("finalColor", "Informe o parâmetro finalColor (Coding do Protocolo de Manchester)");
        }

        try {
            return ManchesterCodes.colorOf(code);
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("finalColor", "Cor final inválida: " + code);
        }
    }

    private String reasonOf(Parameters parameters) {
        var parameter = parameters.getParameter("reason");
        String reason = parameter != null && parameter.hasValue() ? parameter.getValue().primitiveValue() : null;

        if (reason == null || reason.isBlank()) {
            throw ValidationException.of("reason", "Informe o parâmetro reason com o motivo da alteração");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw ValidationException.of("reason", "O motivo deve ter no máximo " + MAX_REASON_LENGTH + " caracteres");
        }
        return reason.trim();
    }
}
