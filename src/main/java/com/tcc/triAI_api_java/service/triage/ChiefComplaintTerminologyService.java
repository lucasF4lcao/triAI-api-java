package com.tcc.triAI_api_java.service.triage;

import com.tcc.triAI_api_java.fhir.ChiefComplaintVocabulary;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Service;

@Service
public class ChiefComplaintTerminologyService {

    private static final String PORTUGUESE = "pt-BR";

    private final SymptomVocabularyClient vocabularyClient;

    public ChiefComplaintTerminologyService(SymptomVocabularyClient vocabularyClient) {
        this.vocabularyClient = vocabularyClient;
    }

    public ValueSet valueSet() {
        List<MlSymptom> symptoms = vocabularyClient.fetch();

        ValueSet valueSet = new ValueSet();
        valueSet.setId(ChiefComplaintVocabulary.VALUE_SET_ID);
        valueSet.setUrl(ChiefComplaintVocabulary.VALUE_SET_URL);
        valueSet.setName("TriAiChiefComplaints");
        valueSet.setTitle("Queixas principais do triAI");
        valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
        valueSet.getCompose().addInclude().setSystem(ChiefComplaintVocabulary.CODE_SYSTEM);

        ValueSet.ValueSetExpansionComponent expansion = valueSet.getExpansion();
        expansion.setTimestamp(new Date());
        expansion.setTotal(symptoms.size());

        for (MlSymptom symptom : symptoms) {
            ValueSet.ValueSetExpansionContainsComponent concept = expansion.addContains();
            concept.setSystem(ChiefComplaintVocabulary.CODE_SYSTEM).setCode(symptom.codigo()).setDisplay(symptom.termo());
            concept.setAbstract(!symptom.isSelectable());
            concept.addDesignation().setLanguage(PORTUGUESE).setValue(symptom.rotulo());
        }

        return valueSet;
    }

    public CodeSystem codeSystem() {
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId(ChiefComplaintVocabulary.VALUE_SET_ID);
        codeSystem.setUrl(ChiefComplaintVocabulary.CODE_SYSTEM);
        codeSystem.setName("TriAiChiefComplaintsCodes");
        codeSystem.setTitle("Palavras-chave de queixa principal do triAI");
        codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
        codeSystem.setCaseSensitive(true);

        for (MlSymptom symptom : vocabularyClient.fetch()) {
            CodeSystem.ConceptDefinitionComponent concept = codeSystem.addConcept()
                    .setCode(symptom.codigo())
                    .setDisplay(symptom.termo());
            concept.addDesignation().setLanguage(PORTUGUESE).setValue(symptom.rotulo());
        }

        return codeSystem;
    }
}
