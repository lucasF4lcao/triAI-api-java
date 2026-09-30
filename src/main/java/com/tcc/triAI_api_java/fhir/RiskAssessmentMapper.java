package com.tcc.triAI_api_java.fhir;

import com.tcc.triAI_api_java.entity.Change;
import com.tcc.triAI_api_java.entity.TriageAnalysis;
import com.tcc.triAI_api_java.enums.ExecutionStatus;
import com.tcc.triAI_api_java.enums.TriageColor;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentMapper {

    private static final String METHOD = "triAI: classificador de ESI convertido para o Protocolo de Manchester";
    private static final String FALLBACK_NOTE =
            "O modelo de classificação não respondeu; o triAI aplicou vermelho por segurança. Revise manualmente.";
    private static final String SAFETY_RULE_NOTE =
            "A cor Vermelho foi definida pela regra de sinais vitais críticos; as probabilidades são as do modelo.";
    private static final String UNKNOWN_PATIENT = "Paciente não identificado";

    public RiskAssessment map(TriageAnalysis analysis, List<Change> changesOldestFirst, TriageInput input) {
        return map(analysis, changesOldestFirst, input, ModelOutput.none());
    }

    public RiskAssessment map(
            TriageAnalysis analysis,
            List<Change> changesOldestFirst,
            TriageInput input,
            ModelOutput modelOutput
    ) {
        boolean fallback = analysis.getExecutionStatus() == ExecutionStatus.ERROR;

        RiskAssessment assessment = new RiskAssessment();
        assessment.setId(String.valueOf(analysis.getId()));
        assessment.setStatus(statusOf(fallback, changesOldestFirst));
        assessment.setSubject(input.patientReference() != null
                ? new Reference(input.patientReference())
                : new Reference().setDisplay(UNKNOWN_PATIENT));
        assessment.setOccurrence(new DateTimeType(occurredAt(analysis)));
        assessment.setMethod(new CodeableConcept().setText(METHOD));

        if (input.encounterReference() != null) {
            assessment.setEncounter(new Reference(input.encounterReference()));
        }
        input.basisReferences().forEach(reference -> assessment.addBasis(new Reference(reference)));

        RiskAssessment.RiskAssessmentPredictionComponent modelPrediction = assessment.addPrediction()
                .setOutcome(ManchesterCodes.concept(analysis.getPredictedColor()))
                .setProbability(new DecimalType(analysis.getConfidence()))
                .setRationale(fallback ? "Cor aplicada por segurança" : "Predição do modelo");

        modelOutput.probabilities().forEach((color, probability) ->
                modelPrediction.addExtension(colorProbability(color, probability)));

        if (modelOutput.safetyRuleApplied()) {
            modelPrediction.addExtension(new Extension(TriageExtensions.SAFETY_RULE, new BooleanType(true)));
            assessment.addNote(new Annotation().setText(SAFETY_RULE_NOTE));
        }

        for (Change change : changesOldestFirst) {
            assessment.addPrediction()
                    .setOutcome(ManchesterCodes.concept(change.getFinalColor()))
                    .setRationale(change.getReason());
        }

        if (fallback) {
            assessment.addNote(new Annotation().setText(FALLBACK_NOTE));
        }
        input.warnings().forEach(warning -> assessment.addNote(new Annotation().setText(warning)));

        return assessment;
    }

    private Extension colorProbability(TriageColor color, double probability) {
        Extension extension = new Extension(TriageExtensions.COLOR_PROBABILITY);
        extension.addExtension(new Extension("color", ManchesterCodes.coding(color)));
        extension.addExtension(new Extension("probability", new DecimalType(probability)));
        return extension;
    }

    private RiskAssessment.RiskAssessmentStatus statusOf(boolean fallback, List<Change> changes) {
        if (!changes.isEmpty()) {
            return RiskAssessment.RiskAssessmentStatus.AMENDED;
        }
        return fallback ? RiskAssessment.RiskAssessmentStatus.PRELIMINARY : RiskAssessment.RiskAssessmentStatus.FINAL;
    }

    private Date occurredAt(TriageAnalysis analysis) {
        LocalDateTime createdAt = analysis.getCreatedAt() != null ? analysis.getCreatedAt() : LocalDateTime.now();
        return Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
