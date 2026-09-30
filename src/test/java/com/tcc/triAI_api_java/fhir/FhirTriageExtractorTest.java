package com.tcc.triAI_api_java.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.context.FhirContext;
import com.tcc.triAI_api_java.exception.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FhirTriageExtractorTest {

    private static final String LOINC = "http://loinc.org";
    private static final String SNOMED = "http://snomed.info/sct";
    private static final String TRIAI = ChiefComplaintVocabulary.CODE_SYSTEM;

    private final FhirContext fhirContext = FhirContext.forR4();
    private final FhirTriageExtractor extractor =
            new FhirTriageExtractor(fhirContext, new ComplaintConceptMap(fhirContext));

    private static FhirBundles completeVitals() {
        return FhirBundles.bundle()
                .observation(LOINC, "8867-4", "110", "/min")
                .observation(LOINC, "9279-1", "22", "/min")
                .observation(LOINC, "59408-5", "93", "%")
                .observation(LOINC, "8310-5", "38.4", "Cel")
                .observation(LOINC, "8480-6", "95", "mm[Hg]")
                .observation(LOINC, "8462-4", "60", "mm[Hg]")
                .observation(LOINC, "72514-3", "7", "{score}");
    }

    @Test
    void extractsEveryVitalSignPatientAndEncounterFromAStandardBundle() {
        String json = completeVitals()
                .patient("p-1", "male", "1970-01-01")
                .encounter(", \"extension\": [{\"url\": \"" + FhirTriageExtractor.ARRIVAL_MODE_EXTENSION_URL + "\", \"valueCode\": \"ambulance\"}],"
                        + "\"reasonCode\": [{\"coding\": [{\"system\": \"" + TRIAI + "\", \"code\": \"chest-pain\"}], \"text\": \"Dor no peito\"},"
                        + "{\"text\": \"piora ao esforço\"}]")
                .json();

        TriageInput input = extractor.extract(json);
        VitalSignsData data = input.data();

        assertThat(data.getHeartRate()).isEqualTo(110);
        assertThat(data.getRespiratoryRate()).isEqualTo(22);
        assertThat(data.getOxygenSaturation()).isEqualTo(93);
        assertThat(data.getTemperature()).isEqualByComparingTo("38.4");
        assertThat(data.getSystolicPressure()).isEqualTo(95);
        assertThat(data.getDiastolicPressure()).isEqualTo(60);
        assertThat(data.getPain()).isEqualTo(7);
        assertThat(data.getMale()).isTrue();
        assertThat(data.getAmbulance()).isTrue();
        assertThat(data.getChiefComplaint()).isEqualTo("Dor no peito, piora ao esforço");
        assertThat(data.getSymptomCodes()).containsExactly("chest-pain");
        assertThat(input.patientReference()).isEqualTo("urn:uuid:p-1");
        assertThat(input.encounterReference()).isEqualTo("Encounter/enc-1");
        assertThat(input.basisReferences()).hasSize(7).contains("Observation/obs-8867-4");
        assertThat(input.warnings()).isEmpty();
    }

    @Test
    void acceptsAlternativeLoincAndSnomedCodesForTheSameSigns() {
        TriageInput input = FhirBundles.bundle()
                .observation(LOINC, "8889-8", "88", "/min")
                .observation(LOINC, "2708-6", "97", "%")
                .observation(SNOMED, "386725007", "37.2", "Cel")
                .observation(SNOMED, "271649006", "120", "mm[Hg]")
                .observation(SNOMED, "271650006", "80", "mm[Hg]")
                .observation(SNOMED, "86290005", "16", "/min")
                .json().transform(extractor::extract);

        VitalSignsData data = input.data();

        assertThat(data.getHeartRate()).isEqualTo(88);
        assertThat(data.getOxygenSaturation()).isEqualTo(97);
        assertThat(data.getTemperature()).isEqualByComparingTo("37.2");
        assertThat(data.getSystolicPressure()).isEqualTo(120);
        assertThat(data.getDiastolicPressure()).isEqualTo(80);
        assertThat(data.getRespiratoryRate()).isEqualTo(16);
    }

    @Test
    void readsBloodPressureFromAPanelWithComponents() {
        String panel = """
                {"resource": {"resourceType": "Observation", "id": "bp", "status": "final",
                  "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9"}]},
                  "component": [
                    {"code": {"coding": [{"system": "http://loinc.org", "code": "8480-6"}]}, "valueQuantity": {"value": 130, "unit": "mmHg"}},
                    {"code": {"coding": [{"system": "http://loinc.org", "code": "8462-4"}]}, "valueQuantity": {"value": 85, "unit": "mmHg"}}]}}
                """;

        VitalSignsData data = extractor.extract(FhirBundles.bundle().observation(LOINC, "8867-4", "70", "/min").raw(panel).json()).data();

        assertThat(data.getSystolicPressure()).isEqualTo(130);
        assertThat(data.getDiastolicPressure()).isEqualTo(85);
    }

    @Test
    void convertsUnitsToTheCanonicalOnes() {
        VitalSignsData data = FhirBundles.bundle()
                .observation(LOINC, "8310-5", "98.6", "[degF]")
                .observation(LOINC, "8480-6", "16", "kPa")
                .observation(LOINC, "59408-5", "0.97", "1")
                .json().transform(extractor::extract).data();

        assertThat(data.getTemperature()).isEqualByComparingTo("37.00");
        assertThat(data.getSystolicPressure()).isEqualTo(120);
        assertThat(data.getOxygenSaturation()).isEqualTo(97);
    }

    @Test
    void treatsATemperatureWithoutUnitAboveFiftyAsFahrenheit() {
        VitalSignsData data = FhirBundles.bundle()
                .observation(LOINC, "8310-5", "101.3", null)
                .json().transform(extractor::extract).data();

        assertThat(data.getTemperature()).isEqualByComparingTo("38.50");
    }

    @Test
    void ignoresAValueWithAnUnsupportedUnitAndWarns() {
        TriageInput input = FhirBundles.bundle()
                .observation(LOINC, "8867-4", "80", "/min")
                .observation(LOINC, "8310-5", "310", "furlong")
                .json().transform(extractor::extract);

        assertThat(input.data().getTemperature()).isNull();
        assertThat(input.warnings()).anyMatch(warning -> warning.contains("furlong") && warning.contains("temperatura"));
    }

    @Test
    void keepsTheMostRecentObservationOfEachSign() {
        VitalSignsData data = FhirBundles.bundle()
                .observation("hr-new", "final", LOINC, "8867-4", "120", "/min", "2026-09-30T10:00:00Z")
                .observation("hr-old", "final", LOINC, "8867-4", "70", "/min", "2026-09-30T08:00:00Z")
                .json().transform(extractor::extract).data();

        assertThat(data.getHeartRate()).isEqualTo(120);
    }

    @Test
    void ignoresCancelledAndEnteredInErrorObservations() {
        VitalSignsData data = FhirBundles.bundle()
                .observation("ok", "final", LOINC, "8867-4", "75", "/min", null)
                .observation("bad", "entered-in-error", LOINC, "8867-4", "200", "/min", "2030-01-01T00:00:00Z")
                .observation("cancelled", "cancelled", LOINC, "9279-1", "40", "/min", null)
                .json().transform(extractor::extract).data();

        assertThat(data.getHeartRate()).isEqualTo(75);
        assertThat(data.getRespiratoryRate()).isNull();
    }

    @Test
    void mapsSnomedAndIcd10ComplaintCodesToTheTriAiVocabulary() {
        String json = FhirBundles.bundle()
                .observation(LOINC, "8867-4", "90", "/min")
                .encounter(", \"reasonCode\": [{\"coding\": [{\"system\": \"http://snomed.info/sct\", \"code\": \"267036007\", \"display\": \"Dyspnea\"}]},"
                        + "{\"coding\": [{\"system\": \"http://hl7.org/fhir/sid/icd-10\", \"code\": \"R51\"}]},"
                        + "{\"coding\": [{\"system\": \"http://snomed.info/sct\", \"code\": \"000000\", \"display\": \"Unmapped\"}]}]")
                .json();

        VitalSignsData data = extractor.extract(json).data();

        assertThat(data.getSymptomCodes()).containsExactly("dyspnea", "headache");
        assertThat(data.getChiefComplaint()).isEqualTo("Dyspnea, Unmapped");
    }

    @Test
    void resolvesComplaintsReferencedFromTheEncounterAndStandaloneChiefComplaintObservations() {
        String condition = "{\"fullUrl\": \"urn:uuid:cond\", \"resource\": {\"resourceType\": \"Condition\", \"id\": \"cond\","
                + "\"code\": {\"text\": \"Tontura intensa\"}, \"subject\": {\"reference\": \"Patient/p\"}}}";
        String chiefObservation = "{\"resource\": {\"resourceType\": \"Observation\", \"id\": \"cc\", \"status\": \"final\","
                + "\"code\": {\"coding\": [{\"system\": \"http://loinc.org\", \"code\": \"10154-3\"}]}, \"valueString\": \"Fraqueza\"}}";

        String json = FhirBundles.bundle()
                .observation(LOINC, "8867-4", "90", "/min")
                .raw(condition)
                .raw(chiefObservation)
                .encounter(", \"reasonReference\": [{\"reference\": \"urn:uuid:cond\"}]")
                .json();

        TriageInput input = extractor.extract(json);

        assertThat(input.data().getChiefComplaint()).contains("Tontura intensa").contains("Fraqueza");
        assertThat(input.basisReferences()).doesNotContain("Observation/cc");
    }

    @Test
    void detectsAmbulanceArrivalFromTheAdmitSource() {
        String json = FhirBundles.bundle()
                .observation(LOINC, "8867-4", "90", "/min")
                .encounter(", \"hospitalization\": {\"admitSource\": {\"text\": \"Chegou de ambulância do SAMU\"}}")
                .json();

        assertThat(extractor.extract(json).data().getAmbulance()).isTrue();
    }

    @Test
    void acceptsTheBundleInsideAParametersResource() {
        String bundle = completeVitals().json();
        String parameters = "{\"resourceType\": \"Parameters\", \"parameter\": [{\"name\": \"bundle\", \"resource\": " + bundle + "}]}";

        assertThat(extractor.extract(parameters).data().getHeartRate()).isEqualTo(110);
    }

    @Test
    void rejectsAResourceThatIsNotABundle() {
        assertThatThrownBy(() -> extractor.extract("{\"resourceType\": \"Patient\"}"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> extractor.extract("not json"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsABundleWithoutAnyRecognizedVitalSignAndListsTheUnknownCodes() {
        String json = FhirBundles.bundle()
                .patient("p-1", "female", "1990-05-12")
                .observation(LOINC, "99999-9", "5", "mg")
                .json();

        assertThatThrownBy(() -> extractor.extract(json))
                .isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).getErrors().get("bundle").get(0))
                        .contains("Nenhum sinal vital reconhecido")
                        .contains("http://loinc.org|99999-9"));
    }

    @Test
    void warnsAboutMissingSignsLowCoverageAndUnknownCodes() {
        TriageInput input = FhirBundles.bundle()
                .observation(LOINC, "8867-4", "80", "/min")
                .observation(LOINC, "12345-6", "1", "x")
                .json().transform(extractor::extract);

        assertThat(input.warnings()).anyMatch(warning -> warning.startsWith("Sinais não encontrados") && warning.contains("pressão sistólica"));
        assertThat(input.warnings()).anyMatch(warning -> warning.contains("Poucos sinais vitais"));
        assertThat(input.warnings()).anyMatch(warning -> warning.contains("http://loinc.org|12345-6"));
    }

    @Test
    void buildsThePayloadInTheMlContract() {
        String json = completeVitals()
                .patient("p-1", "male", "1970-01-01")
                .encounter(", \"reasonCode\": [{\"coding\": [{\"system\": \"" + TRIAI + "\", \"code\": \"fever\"}]}]")
                .json();

        Map<String, Object> payload = extractor.extract(json).data().toClassificationPayload();

        assertThat(payload).containsEntry("frequencia_cardiaca", 110)
                .containsEntry("dor", 7)
                .containsEntry("sexo_masculino", true)
                .containsEntry("sintomas", List.of("fever"));
        assertThat((BigDecimal) payload.get("temperatura")).isEqualByComparingTo("38.4");
    }
}
