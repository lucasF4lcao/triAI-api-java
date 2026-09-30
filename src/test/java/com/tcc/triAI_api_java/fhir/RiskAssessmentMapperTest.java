package com.tcc.triAI_api_java.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.triAI_api_java.entity.Change;
import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.enums.ExecutionStatus;
import com.tcc.triAI_api_java.enums.TriageColor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.junit.jupiter.api.Test;

class RiskAssessmentMapperTest {

    private final RiskAssessmentMapper mapper = new RiskAssessmentMapper();

    private static TriageAnalysis analysis(TriageColor color, String confidence, ExecutionStatus status) {
        TriageAnalysis analysis = new TriageAnalysis();
        analysis.setId(42L);
        analysis.setPredictedColor(color);
        analysis.setConfidence(new BigDecimal(confidence));
        analysis.setExecutionStatus(status);
        analysis.setCreatedAt(LocalDateTime.of(2026, 9, 30, 14, 0));
        return analysis;
    }

    private static Change change(TriageColor color, String reason) {
        Change change = new Change();
        change.setFinalColor(color);
        change.setReason(reason);
        return change;
    }

    private static TriageInput input(List<String> warnings) {
        return new TriageInput(new VitalSignsData(), "urn:uuid:p-1", "Encounter/enc-1", List.of("Observation/o1"), warnings);
    }

    @Test
    void mapsASuccessfulAnalysisToAFinalRiskAssessment() {
        RiskAssessment assessment = mapper.map(analysis(TriageColor.ORANGE, "0.7100", ExecutionStatus.SUCCESS), List.of(), input(List.of()));

        assertThat(assessment.getIdElement().getIdPart()).isEqualTo("42");
        assertThat(assessment.getStatus()).isEqualTo(RiskAssessment.RiskAssessmentStatus.FINAL);
        assertThat(assessment.getSubject().getReference()).isEqualTo("urn:uuid:p-1");
        assertThat(assessment.getEncounter().getReference()).isEqualTo("Encounter/enc-1");
        assertThat(assessment.getBasis()).extracting(reference -> reference.getReference()).containsExactly("Observation/o1");

        RiskAssessment.RiskAssessmentPredictionComponent prediction = assessment.getPredictionFirstRep();
        assertThat(prediction.getOutcome().getCodingFirstRep().getSystem()).isEqualTo(ManchesterCodes.SYSTEM);
        assertThat(prediction.getOutcome().getCodingFirstRep().getCode()).isEqualTo("orange");
        assertThat(prediction.getProbabilityDecimalType().getValue()).isEqualByComparingTo("0.71");
    }

    @Test
    void marksAFallbackAnalysisAsPreliminaryWithAnExplanation() {
        RiskAssessment assessment = mapper.map(analysis(TriageColor.RED, "0.0", ExecutionStatus.ERROR), List.of(), input(List.of()));

        assertThat(assessment.getStatus()).isEqualTo(RiskAssessment.RiskAssessmentStatus.PRELIMINARY);
        assertThat(assessment.getNoteFirstRep().getText()).contains("não respondeu");
    }

    @Test
    void appendsEachAmendmentAsAnotherPredictionAndMarksTheAssessmentAmended() {
        List<Change> changes = List.of(change(TriageColor.RED, "Piora clínica"), change(TriageColor.YELLOW, "Reavaliado"));

        RiskAssessment assessment = mapper.map(analysis(TriageColor.ORANGE, "0.7", ExecutionStatus.SUCCESS), changes, TriageInput.empty());

        assertThat(assessment.getStatus()).isEqualTo(RiskAssessment.RiskAssessmentStatus.AMENDED);
        assertThat(assessment.getPrediction()).hasSize(3);
        assertThat(assessment.getPrediction().get(2).getOutcome().getCodingFirstRep().getCode()).isEqualTo("yellow");
        assertThat(assessment.getPrediction().get(2).getRationale()).isEqualTo("Reavaliado");
    }

    @Test
    void usesADisplayOnlySubjectWhenThePatientIsUnknownAndCarriesWarningsAsNotes() {
        RiskAssessment assessment = mapper.map(
                analysis(TriageColor.GREEN, "0.5", ExecutionStatus.SUCCESS),
                List.of(),
                new TriageInput(new VitalSignsData(), null, null, List.of(), List.of("Sinais não encontrados: dor.")));

        assertThat(assessment.getSubject().hasReference()).isFalse();
        assertThat(assessment.getSubject().getDisplay()).isEqualTo("Paciente não identificado");
        assertThat(assessment.getNote()).extracting(note -> note.getText()).containsExactly("Sinais não encontrados: dor.");
    }

    @Test
    void resolvesColorsByFhirCodeOrPortugueseLabel() {
        assertThat(ManchesterCodes.colorOf("red")).isEqualTo(TriageColor.RED);
        assertThat(ManchesterCodes.colorOf("Laranja")).isEqualTo(TriageColor.ORANGE);
    }
}
