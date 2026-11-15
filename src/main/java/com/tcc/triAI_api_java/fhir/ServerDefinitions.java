package com.tcc.triAI_api_java.fhir;

import java.util.Date;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.springframework.stereotype.Component;

@Component
public class ServerDefinitions {

    public static final String OPERATION_BASE = "https://triai.local/fhir/OperationDefinition/";
    public static final String TRIAGE_OPERATION_ID = "triage";
    public static final String AMEND_OPERATION_ID = "amend";

    public CapabilityStatement capabilityStatement() {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setId("triai");
        statement.setName("TriAiServer");
        statement.setTitle("API FHIR do triAI");
        statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
        statement.setDate(new Date());
        statement.setPublisher("triAI");
        statement.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);
        statement.setFhirVersion(Enumerations.FHIRVersion._4_0_1);
        statement.addFormat(ca.uhn.fhir.rest.api.Constants.CT_FHIR_JSON_NEW);
        statement.getSoftware().setName("triAI API");

        CapabilityStatement.CapabilityStatementRestComponent rest = statement.addRest();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
        rest.getSecurity()
                .setCors(true)
                .setDescription("Token Bearer no cabeçalho Authorization. SMART on FHIR ainda não é suportado.");
        rest.addOperation().setName(TRIAGE_OPERATION_ID).setDefinition(OPERATION_BASE + TRIAGE_OPERATION_ID);

        CapabilityStatement.CapabilityStatementRestResourceComponent risk = rest.addResource().setType("RiskAssessment");
        risk.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        risk.addOperation().setName(AMEND_OPERATION_ID).setDefinition(OPERATION_BASE + AMEND_OPERATION_ID);

        CapabilityStatement.CapabilityStatementRestResourceComponent valueSet = rest.addResource().setType("ValueSet");
        valueSet.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        valueSet.addOperation().setName("expand").setDefinition("http://hl7.org/fhir/OperationDefinition/ValueSet-expand");

        for (String type : new String[]{"CodeSystem", "ConceptMap", "OperationDefinition"}) {
            rest.addResource().setType(type).addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
        }

        return statement;
    }

    public OperationDefinition triageOperation() {
        OperationDefinition operation = baseOperation(TRIAGE_OPERATION_ID, "Triage", "Classificação de risco (Protocolo de Manchester)");
        operation.setSystem(true).setType(false).setInstance(false);
        operation.setComment("Recebe um Bundle FHIR com Patient, Observations de sinais vitais e Encounter, e devolve um RiskAssessment.");
        operation.addParameter().setName("bundle").setUse(OperationDefinition.OperationParameterUse.IN).setMin(1).setMax("1").setType("Bundle");
        operation.addParameter().setName("return").setUse(OperationDefinition.OperationParameterUse.OUT).setMin(1).setMax("1").setType("RiskAssessment");
        return operation;
    }

    public OperationDefinition amendOperation() {
        OperationDefinition operation = baseOperation(AMEND_OPERATION_ID, "Amend", "Alteração da cor pelo profissional");
        operation.setSystem(false).setType(false).setInstance(true);
        operation.addResource("RiskAssessment");
        operation.setComment("Registra a cor final decidida pelo profissional, com o motivo, e devolve o RiskAssessment atualizado.");
        operation.addParameter().setName("finalColor").setUse(OperationDefinition.OperationParameterUse.IN).setMin(1).setMax("1").setType("Coding");
        operation.addParameter().setName("reason").setUse(OperationDefinition.OperationParameterUse.IN).setMin(1).setMax("1").setType("string");
        operation.addParameter().setName("return").setUse(OperationDefinition.OperationParameterUse.OUT).setMin(1).setMax("1").setType("RiskAssessment");
        return operation;
    }

    private OperationDefinition baseOperation(String code, String name, String title) {
        OperationDefinition operation = new OperationDefinition();
        operation.setId(code);
        operation.setUrl(OPERATION_BASE + code);
        operation.setName(name);
        operation.setTitle(title);
        operation.setStatus(Enumerations.PublicationStatus.ACTIVE);
        operation.setKind(OperationDefinition.OperationKind.OPERATION);
        operation.setCode(code);
        return operation;
    }
}
