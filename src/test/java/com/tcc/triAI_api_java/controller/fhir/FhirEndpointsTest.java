package com.tcc.triAI_api_java.controller.fhir;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.enums.ExecutionStatus;
import com.tcc.triAI_api_java.enums.TriageColor;
import com.tcc.triAI_api_java.exception.ResourceNotFoundException;
import com.tcc.triAI_api_java.exception.ValidationException;
import com.tcc.triAI_api_java.fhir.ComplaintConceptMap;
import com.tcc.triAI_api_java.fhir.RiskAssessmentMapper;
import com.tcc.triAI_api_java.fhir.ServerDefinitions;
import com.tcc.triAI_api_java.fhir.TriageInput;
import com.tcc.triAI_api_java.service.triage.ChiefComplaintTerminologyService;
import com.tcc.triAI_api_java.service.triage.CreateTriageAnalysisService;
import com.tcc.triAI_api_java.service.triage.MlSymptom;
import com.tcc.triAI_api_java.service.triage.SymptomVocabularyClient;
import com.tcc.triAI_api_java.service.triage.TriageOutcome;
import com.tcc.triAI_api_java.service.triage.TriageReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FhirEndpointsTest {

    private static final String FHIR_JSON = "application/fhir+json";

    private final CreateTriageAnalysisService createService = mock(CreateTriageAnalysisService.class);
    private final TriageReviewService reviewService = mock(TriageReviewService.class);
    private final SymptomVocabularyClient vocabularyClient = mock(SymptomVocabularyClient.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FhirContext fhirContext = FhirContext.forR4();
        FhirJson fhirJson = new FhirJson(fhirContext);

        when(vocabularyClient.fetch()).thenReturn(List.of(new MlSymptom("chest-pain", "chest pain", "Dor torácica", true)));

        FhirTriageController triage = new FhirTriageController(createService, reviewService, new RiskAssessmentMapper(), fhirJson);
        FhirTerminologyController terminology = new FhirTerminologyController(
                new ChiefComplaintTerminologyService(vocabularyClient),
                new ComplaintConceptMap(fhirContext),
                new ServerDefinitions(),
                fhirJson);

        mockMvc = MockMvcBuilders.standaloneSetup(triage, terminology)
                .setControllerAdvice(new FhirExceptionHandler(fhirJson))
                .build();
    }

    private static TriageAnalysis analysis(TriageColor color) {
        TriageAnalysis analysis = new TriageAnalysis();
        analysis.setId(42L);
        analysis.setPredictedColor(color);
        analysis.setConfidence(new BigDecimal("0.7100"));
        analysis.setExecutionStatus(ExecutionStatus.SUCCESS);
        analysis.setCreatedAt(LocalDateTime.of(2026, 9, 30, 14, 0));
        return analysis;
    }

    @Test
    void triageReturnsARiskAssessmentAsFhirJson() throws Exception {
        when(createService.run(anyString())).thenReturn(new TriageOutcome(analysis(TriageColor.ORANGE), TriageInput.empty()));

        mockMvc.perform(post("/api/fhir/$triage").contentType(FHIR_JSON).content("{\"resourceType\":\"Bundle\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(FHIR_JSON))
                .andExpect(jsonPath("$.resourceType").value("RiskAssessment"))
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.status").value("final"))
                .andExpect(jsonPath("$.prediction[0].outcome.coding[0].code").value("orange"))
                .andExpect(jsonPath("$.prediction[0].probabilityDecimal").value(0.71));
    }

    @Test
    void triageAlsoAcceptsPlainJsonAndAnAcceptHeaderOfApplicationJson() throws Exception {
        when(createService.run(anyString())).thenReturn(new TriageOutcome(analysis(TriageColor.GREEN), TriageInput.empty()));

        mockMvc.perform(post("/api/fhir/$triage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"Bundle\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(FHIR_JSON))
                .andExpect(jsonPath("$.resourceType").value("RiskAssessment"));
    }

    @Test
    void invalidInputBecomesAnOperationOutcome() throws Exception {
        when(createService.run(anyString())).thenThrow(ValidationException.of("bundle", "Nenhum sinal vital reconhecido"));

        mockMvc.perform(post("/api/fhir/$triage").contentType(FHIR_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(FHIR_JSON))
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("error"))
                .andExpect(jsonPath("$.issue[0].code").value("invalid"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value("Nenhum sinal vital reconhecido"));
    }

    @Test
    void unexpectedFailuresBecomeAFatalOperationOutcome() throws Exception {
        when(createService.run(anyString())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/fhir/$triage").contentType(FHIR_JSON).content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("fatal"));
    }

    @Test
    void anUnavailableMlServiceBecomesATransient503() throws Exception {
        when(vocabularyClient.fetch()).thenThrow(new org.springframework.web.client.ResourceAccessException("connection refused"));

        mockMvc.perform(get("/api/fhir/ValueSet/chief-complaints/$expand"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("transient"));
    }

    @Test
    void readingAnUnknownRiskAssessmentIsANotFoundOperationOutcome() throws Exception {
        when(reviewService.find(7L)).thenThrow(new ResourceNotFoundException("RiskAssessment/7 não encontrado"));

        mockMvc.perform(get("/api/fhir/RiskAssessment/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("not-found"));
    }

    @Test
    void amendRegistersTheClinicianColorAndReturnsTheAmendedAssessment() throws Exception {
        TriageAnalysis analysis = analysis(TriageColor.ORANGE);
        com.tcc.triAI_api_java.entity.Change change = new com.tcc.triAI_api_java.entity.Change();
        change.setFinalColor(TriageColor.RED);
        change.setReason("Piora clínica");
        when(reviewService.amend(42L, TriageColor.RED, "Piora clínica"))
                .thenReturn(new TriageReviewService.Review(analysis, List.of(change)));

        String parameters = """
                {"resourceType": "Parameters", "parameter": [
                  {"name": "finalColor", "valueCoding": {"system": "https://triai.local/fhir/CodeSystem/manchester-triage", "code": "red"}},
                  {"name": "reason", "valueString": "Piora clínica"}]}
                """;

        mockMvc.perform(post("/api/fhir/RiskAssessment/42/$amend").contentType(FHIR_JSON).content(parameters))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("amended"))
                .andExpect(jsonPath("$.prediction[1].outcome.coding[0].code").value("red"))
                .andExpect(jsonPath("$.prediction[1].rationale").value("Piora clínica"));
    }

    @Test
    void amendRejectsAnInvalidColorAndAMissingReason() throws Exception {
        String invalidColor = """
                {"resourceType": "Parameters", "parameter": [
                  {"name": "finalColor", "valueCoding": {"code": "purple"}}, {"name": "reason", "valueString": "x"}]}
                """;
        String missingReason = """
                {"resourceType": "Parameters", "parameter": [{"name": "finalColor", "valueCoding": {"code": "red"}}]}
                """;

        mockMvc.perform(post("/api/fhir/RiskAssessment/42/$amend").contentType(FHIR_JSON).content(invalidColor))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issue[0].diagnostics").value("Cor final inválida: purple"));
        mockMvc.perform(post("/api/fhir/RiskAssessment/42/$amend").contentType(FHIR_JSON).content(missingReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issue[0].diagnostics").value("Informe o parâmetro reason com o motivo da alteração"));
    }

    @Test
    void amendRejectsMalformedJsonAsAStructureError() throws Exception {
        mockMvc.perform(post("/api/fhir/RiskAssessment/42/$amend").contentType(FHIR_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issue[0].code").value("structure"));
    }

    @Test
    void metadataDescribesTheServer() throws Exception {
        mockMvc.perform(get("/api/fhir/metadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(FHIR_JSON))
                .andExpect(jsonPath("$.resourceType").value("CapabilityStatement"))
                .andExpect(jsonPath("$.fhirVersion").value("4.0.1"))
                .andExpect(jsonPath("$.rest[0].mode").value("server"))
                .andExpect(jsonPath("$.rest[0].operation[0].name").value("triage"))
                .andExpect(jsonPath("$.rest[0].resource[?(@.type=='RiskAssessment')].operation[0].name").value("amend"));
    }

    @Test
    void expandsTheChiefComplaintValueSet() throws Exception {
        mockMvc.perform(get("/api/fhir/ValueSet/chief-complaints/$expand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("ValueSet"))
                .andExpect(jsonPath("$.expansion.total").value(1))
                .andExpect(jsonPath("$.expansion.contains[0].code").value("chest-pain"))
                .andExpect(jsonPath("$.expansion.contains[0].designation[0].value").value("Dor torácica"));
    }

    @Test
    void unknownTerminologyResourcesAreNotFound() throws Exception {
        mockMvc.perform(get("/api/fhir/ValueSet/other/$expand")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/fhir/CodeSystem/other")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/fhir/ConceptMap/other")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/fhir/OperationDefinition/other")).andExpect(status().isNotFound());
    }

    @Test
    void servesTheCodeSystemsConceptMapAndOperationDefinitions() throws Exception {
        mockMvc.perform(get("/api/fhir/CodeSystem/manchester-triage"))
                .andExpect(jsonPath("$.resourceType").value("CodeSystem"))
                .andExpect(jsonPath("$.concept.length()").value(5));
        mockMvc.perform(get("/api/fhir/CodeSystem/chief-complaints"))
                .andExpect(jsonPath("$.concept[0].code").value("chest-pain"));
        mockMvc.perform(get("/api/fhir/ConceptMap/chief-complaints"))
                .andExpect(jsonPath("$.resourceType").value("ConceptMap"))
                .andExpect(jsonPath("$.group[0].source").value("http://snomed.info/sct"));
        mockMvc.perform(get("/api/fhir/OperationDefinition/triage"))
                .andExpect(jsonPath("$.code").value("triage"))
                .andExpect(jsonPath("$.system").value(true));
        mockMvc.perform(get("/api/fhir/OperationDefinition/amend"))
                .andExpect(jsonPath("$.instance").value(true));
    }
}
