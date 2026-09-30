package com.tcc.triAI_api_java.fhir;

import com.tcc.triAI_api_java.enums.TriageColor;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;

public final class ManchesterCodes {

    public static final String SYSTEM = "https://triai.local/fhir/CodeSystem/manchester-triage";
    public static final String CODE_SYSTEM_ID = "manchester-triage";

    private static final Map<TriageColor, String> DISPLAYS = Map.of(
            TriageColor.RED, "Vermelho - emergência",
            TriageColor.ORANGE, "Laranja - muito urgente",
            TriageColor.YELLOW, "Amarelo - urgente",
            TriageColor.GREEN, "Verde - pouco urgente",
            TriageColor.BLUE, "Azul - não urgente");

    private ManchesterCodes() {
    }

    public static CodeableConcept concept(TriageColor color) {
        Coding coding = new Coding().setSystem(SYSTEM).setCode(color.getValue()).setDisplay(DISPLAYS.get(color));
        return new CodeableConcept().addCoding(coding).setText(DISPLAYS.get(color));
    }

    public static TriageColor colorOf(String code) {
        try {
            return TriageColor.fromValue(code);
        } catch (IllegalArgumentException exception) {
            return TriageColor.fromMlLabel(code);
        }
    }

    public static CodeSystem codeSystem() {
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId(CODE_SYSTEM_ID);
        codeSystem.setUrl(SYSTEM);
        codeSystem.setName("ManchesterTriageColors");
        codeSystem.setTitle("Cores do Protocolo de Manchester");
        codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
        codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
        codeSystem.setCaseSensitive(true);

        for (TriageColor color : TriageColor.values()) {
            codeSystem.addConcept().setCode(color.getValue()).setDisplay(DISPLAYS.get(color));
        }

        return codeSystem;
    }
}
