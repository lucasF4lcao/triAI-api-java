package com.tcc.triAI_api_java.service.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.triAI_api_java.fhir.ChiefComplaintVocabulary;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

class ChiefComplaintTerminologyServiceTest {

    private final SymptomVocabularyClient client = mock(SymptomVocabularyClient.class);
    private final ChiefComplaintTerminologyService service = new ChiefComplaintTerminologyService(client);

    private void stubVocabulary() {
        when(client.fetch()).thenReturn(List.of(
                new MlSymptom("chest-pain", "chest pain", "Dor torácica"),
                new MlSymptom("dyspnea", "dyspnea", "Dispneia (falta de ar)")));
    }

    @Test
    void expandsTheMlVocabularyIntoAValueSetWithPortugueseDesignations() {
        stubVocabulary();

        ValueSet valueSet = service.valueSet();
        ValueSet.ValueSetExpansionContainsComponent first = valueSet.getExpansion().getContains().get(0);

        assertThat(valueSet.getUrl()).isEqualTo(ChiefComplaintVocabulary.VALUE_SET_URL);
        assertThat(valueSet.getExpansion().getTotal()).isEqualTo(2);
        assertThat(first.getSystem()).isEqualTo(ChiefComplaintVocabulary.CODE_SYSTEM);
        assertThat(first.getCode()).isEqualTo("chest-pain");
        assertThat(first.getDisplay()).isEqualTo("chest pain");
        assertThat(first.getDesignationFirstRep().getLanguage()).isEqualTo("pt-BR");
        assertThat(first.getDesignationFirstRep().getValue()).isEqualTo("Dor torácica");
    }

    @Test
    void publishesTheSameConceptsAsACompleteCodeSystem() {
        stubVocabulary();

        CodeSystem codeSystem = service.codeSystem();

        assertThat(codeSystem.getUrl()).isEqualTo(ChiefComplaintVocabulary.CODE_SYSTEM);
        assertThat(codeSystem.getContent()).isEqualTo(CodeSystem.CodeSystemContentMode.COMPLETE);
        assertThat(codeSystem.getConcept()).extracting(CodeSystem.ConceptDefinitionComponent::getCode)
                .containsExactly("chest-pain", "dyspnea");
    }
}
