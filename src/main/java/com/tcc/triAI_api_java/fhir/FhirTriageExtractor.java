package com.tcc.triAI_api_java.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.tcc.triAI_api_java.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

@Service
public class FhirTriageExtractor {

    static final String ARRIVAL_MODE_EXTENSION_URL = "https://triai.local/fhir/StructureDefinition/arrival-mode";

    private static final int MAX_UNRECOGNIZED_SHOWN = 5;
    private static final int MIN_RELIABLE_SIGNS = 3;
    private static final List<String> AMBULANCE_MARKERS = List.of("ambulance", "ambulância", "ambulancia", "helicopter");

    private final FhirContext fhirContext;
    private final ComplaintConceptMap conceptMap;

    public FhirTriageExtractor(FhirContext fhirContext, ComplaintConceptMap conceptMap) {
        this.fhirContext = fhirContext;
        this.conceptMap = conceptMap;
    }

    public TriageInput extract(String json) {
        return extract(parseBundle(json));
    }

    private Bundle parseBundle(String json) {
        IBaseResource resource;
        try {
            resource = fhirContext.newJsonParser().parseResource(json);
        } catch (DataFormatException exception) {
            throw ValidationException.of("bundle", "JSON FHIR inválido: " + exception.getMessage());
        }

        if (resource instanceof Bundle bundle) {
            return bundle;
        }

        if (resource instanceof Parameters parameters) {
            for (Parameters.ParametersParameterComponent parameter : parameters.getParameter()) {
                if (parameter.getResource() instanceof Bundle bundle) {
                    return bundle;
                }
            }
        }

        throw ValidationException.of("bundle", "Envie um Bundle FHIR, diretamente ou dentro de Parameters");
    }

    private TriageInput extract(Bundle bundle) {
        Map<String, Resource> index = indexResources(bundle);
        Context context = new Context();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();

            if (resource instanceof Patient patient && context.patientReference == null) {
                fillPatient(patient, context);
                context.patientReference = referenceOf(entry, patient);
            } else if (resource instanceof Encounter encounter && context.encounterReference == null) {
                fillEncounter(encounter, index, context);
                context.encounterReference = referenceOf(entry, encounter);
            } else if (resource instanceof Observation observation) {
                collectObservation(observation, referenceOf(entry, observation), context);
            }
        }

        fillVitals(context);
        fillComplaint(context);

        if (context.chosen.isEmpty()) {
            throw ValidationException.of(
                    "bundle",
                    "Nenhum sinal vital reconhecido no Bundle. Envie Observations com códigos LOINC ou SNOMED CT de "
                            + "frequência cardíaca, frequência respiratória, saturação, temperatura, pressão arterial ou dor."
                            + unrecognizedSuffix(context));
        }

        addQualityWarnings(context);

        return new TriageInput(
                context.data,
                context.patientReference,
                context.encounterReference,
                List.copyOf(context.basis),
                List.copyOf(context.warnings));
    }

    private Map<String, Resource> indexResources(Bundle bundle) {
        Map<String, Resource> index = new HashMap<>();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            if (resource == null) {
                continue;
            }
            if (entry.hasFullUrl()) {
                index.put(entry.getFullUrl(), resource);
            }
            if (resource.hasIdElement() && resource.getIdElement().hasIdPart()) {
                index.put(resource.fhirType() + "/" + resource.getIdElement().getIdPart(), resource);
            }
        }

        return index;
    }

    private String referenceOf(Bundle.BundleEntryComponent entry, Resource resource) {
        if (entry.hasFullUrl()) {
            return entry.getFullUrl();
        }
        if (resource.hasIdElement() && resource.getIdElement().hasIdPart()) {
            return resource.fhirType() + "/" + resource.getIdElement().getIdPart();
        }
        return null;
    }

    private void fillPatient(Patient patient, Context context) {
        if (patient.hasBirthDate()) {
            LocalDate birthDate = patient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            context.data.setAge(Period.between(birthDate, LocalDate.now()).getYears());
        }

        Enumerations.AdministrativeGender gender = patient.getGender();
        if (gender == Enumerations.AdministrativeGender.MALE) {
            context.data.setMale(true);
        } else if (gender == Enumerations.AdministrativeGender.FEMALE) {
            context.data.setMale(false);
        }
    }

    private void fillEncounter(Encounter encounter, Map<String, Resource> index, Context context) {
        Extension arrival = encounter.getExtensionByUrl(ARRIVAL_MODE_EXTENSION_URL);
        if (arrival != null && arrival.getValue() instanceof CodeType mode && mode.hasValue()) {
            context.data.setAmbulance("ambulance".equals(mode.getValue()));
        } else if (mentionsAmbulance(encounter.getHospitalization().getAdmitSource())) {
            context.data.setAmbulance(true);
        }

        for (CodeableConcept reason : encounter.getReasonCode()) {
            addComplaint(reason, context);
        }

        for (Reference reference : encounter.getReasonReference()) {
            Resource target = reference.hasReference() ? index.get(reference.getReference()) : null;

            if (target instanceof Condition condition) {
                addComplaint(condition.getCode(), context);
            } else if (target instanceof Observation observation) {
                addText(complaintTextOf(observation), context);
            }
        }
    }

    private boolean mentionsAmbulance(CodeableConcept admitSource) {
        List<String> candidates = new ArrayList<>();
        if (admitSource.hasText()) {
            candidates.add(admitSource.getText());
        }
        for (Coding coding : admitSource.getCoding()) {
            candidates.add(coding.getCode());
            candidates.add(coding.getDisplay());
        }

        return candidates.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> AMBULANCE_MARKERS.stream().anyMatch(value::contains));
    }

    private void addComplaint(CodeableConcept concept, Context context) {
        for (Coding coding : concept.getCoding()) {
            if (!coding.hasCode()) {
                continue;
            }
            if (ChiefComplaintVocabulary.CODE_SYSTEM.equals(coding.getSystem())) {
                addSymptom(coding.getCode(), context);
            } else {
                conceptMap.targetsOf(coding.getSystem(), coding.getCode()).forEach(code -> addSymptom(code, context));
            }
        }

        String text = concept.hasText() ? concept.getText()
                : concept.getCoding().stream().filter(Coding::hasDisplay).map(Coding::getDisplay).findFirst().orElse(null);
        addText(text, context);
    }

    private void addSymptom(String code, Context context) {
        if (!context.data.getSymptomCodes().contains(code)) {
            context.data.getSymptomCodes().add(code);
        }
    }

    private void addText(String text, Context context) {
        if (text != null && !text.isBlank()) {
            context.complaintTexts.add(text.trim());
        }
    }

    private String complaintTextOf(Observation observation) {
        if (observation.hasValueStringType()) {
            return observation.getValueStringType().getValue();
        }
        if (observation.hasValueCodeableConcept()) {
            CodeableConcept concept = observation.getValueCodeableConcept();
            return concept.hasText() ? concept.getText() : concept.getCodingFirstRep().getDisplay();
        }
        return observation.getCode().getText();
    }

    private void collectObservation(Observation observation, String reference, Context context) {
        if (observation.getStatus() == Observation.ObservationStatus.ENTEREDINERROR
                || observation.getStatus() == Observation.ObservationStatus.CANCELLED) {
            return;
        }

        boolean recognized = false;
        Date effective = effectiveOf(observation);

        for (Coding coding : observation.getCode().getCoding()) {
            String code = coding.getCode();

            if (ObservationCodes.CHIEF_COMPLAINT.equals(code)) {
                addText(complaintTextOf(observation), context);
                return;
            }
            if (ObservationCodes.BLOOD_PRESSURE_PANELS.contains(code)) {
                recognized = true;
            }

            Optional<VitalSign> sign = ObservationCodes.signOf(code);
            if (sign.isPresent()) {
                recognized = true;
                addReading(sign.get(), measuredOf(observation), effective, reference, context);
            }
        }

        for (Observation.ObservationComponentComponent component : observation.getComponent()) {
            for (Coding coding : component.getCode().getCoding()) {
                Optional<VitalSign> sign = ObservationCodes.signOf(coding.getCode());
                if (sign.isPresent()) {
                    recognized = true;
                    addReading(sign.get(), measuredOf(component), effective, reference, context);
                }
            }
        }

        if (!recognized && observation.getCode().hasCoding()) {
            Coding first = observation.getCode().getCodingFirstRep();
            context.unrecognized.add(first.getSystem() + "|" + first.getCode());
        }
    }

    private void addReading(VitalSign sign, Optional<Measured> measured, Date effective, String reference, Context context) {
        if (measured.isEmpty()) {
            return;
        }

        Optional<BigDecimal> canonical = UnitConverter.toCanonical(sign, measured.get().value(), measured.get().unit());
        if (canonical.isEmpty()) {
            context.warnings.add("Unidade '" + measured.get().unit() + "' não suportada para " + sign.getLabel() + "; valor ignorado.");
            return;
        }

        context.readings.computeIfAbsent(sign, ignored -> new ArrayList<>())
                .add(new Reading(canonical.get(), effective, context.order++, reference));
    }

    private Optional<Measured> measuredOf(Observation observation) {
        if (observation.hasValueQuantity()) {
            return quantity(observation.getValueQuantity());
        }
        if (observation.hasValueIntegerType()) {
            return Optional.of(new Measured(BigDecimal.valueOf(observation.getValueIntegerType().getValue()), null));
        }
        if (observation.hasValueStringType()) {
            return numeric(observation.getValueStringType().getValue());
        }
        return Optional.empty();
    }

    private Optional<Measured> measuredOf(Observation.ObservationComponentComponent component) {
        if (component.hasValueQuantity()) {
            return quantity(component.getValueQuantity());
        }
        if (component.hasValueIntegerType()) {
            return Optional.of(new Measured(BigDecimal.valueOf(component.getValueIntegerType().getValue()), null));
        }
        return Optional.empty();
    }

    private Optional<Measured> quantity(org.hl7.fhir.r4.model.Quantity quantity) {
        if (!quantity.hasValue()) {
            return Optional.empty();
        }
        String unit = quantity.hasCode() ? quantity.getCode() : quantity.getUnit();
        return Optional.of(new Measured(quantity.getValue(), unit));
    }

    private Optional<Measured> numeric(String text) {
        try {
            return Optional.of(new Measured(new BigDecimal(text.trim()), null));
        } catch (NumberFormatException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private Date effectiveOf(Observation observation) {
        if (observation.hasEffectiveDateTimeType()) {
            return observation.getEffectiveDateTimeType().getValue();
        }
        if (observation.hasEffectiveInstantType()) {
            return observation.getEffectiveInstantType().getValue();
        }
        if (observation.hasEffectivePeriod()) {
            return observation.getEffectivePeriod().hasEnd()
                    ? observation.getEffectivePeriod().getEnd()
                    : observation.getEffectivePeriod().getStart();
        }
        return null;
    }

    private void fillVitals(Context context) {
        Comparator<Reading> latest = Comparator
                .comparing(Reading::effective, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(Reading::order);

        for (Map.Entry<VitalSign, List<Reading>> entry : context.readings.entrySet()) {
            Reading best = entry.getValue().stream().max(latest).orElseThrow();
            context.chosen.put(entry.getKey(), best);
            if (best.reference() != null) {
                context.basis.add(best.reference());
            }
        }

        VitalsWriter.write(context.data, context.chosen);
    }

    private void fillComplaint(Context context) {
        if (!context.complaintTexts.isEmpty()) {
            context.data.setChiefComplaint(String.join(", ", context.complaintTexts));
        }
    }

    private void addQualityWarnings(Context context) {
        List<String> missing = new ArrayList<>();
        for (VitalSign sign : VitalSign.values()) {
            if (!context.chosen.containsKey(sign)) {
                missing.add(sign.getLabel());
            }
        }

        if (!missing.isEmpty()) {
            context.warnings.add("Sinais não encontrados no Bundle: " + String.join(", ", missing) + ".");
        }
        if (context.chosen.size() < MIN_RELIABLE_SIGNS) {
            context.warnings.add("Poucos sinais vitais reconhecidos; a classificação pode ser menos confiável.");
        }
        if (!context.unrecognized.isEmpty()) {
            context.warnings.add("Observations com códigos não reconhecidos foram ignoradas:" + unrecognizedSuffix(context));
        }
    }

    private String unrecognizedSuffix(Context context) {
        if (context.unrecognized.isEmpty()) {
            return "";
        }
        Set<String> shown = context.unrecognized.stream().limit(MAX_UNRECOGNIZED_SHOWN).collect(Collectors.toCollection(LinkedHashSet::new));
        return " " + String.join(", ", shown) + (context.unrecognized.size() > MAX_UNRECOGNIZED_SHOWN ? " e outros" : "") + ".";
    }

    private record Measured(BigDecimal value, String unit) {
    }

    record Reading(BigDecimal value, Date effective, int order, String reference) {
    }

    private static final class Context {
        final VitalSignsData data = new VitalSignsData();
        final Map<VitalSign, List<Reading>> readings = new EnumMap<>(VitalSign.class);
        final Map<VitalSign, Reading> chosen = new EnumMap<>(VitalSign.class);
        final Set<String> basis = new LinkedHashSet<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> complaintTexts = new ArrayList<>();
        final Set<String> unrecognized = new LinkedHashSet<>();
        String patientReference;
        String encounterReference;
        int order;
    }

    static final class VitalsWriter {

        private VitalsWriter() {
        }

        static void write(VitalSignsData data, Map<VitalSign, Reading> chosen) {
            chosen.forEach((sign, reading) -> {
                BigDecimal value = reading.value();
                switch (sign) {
                    case HEART_RATE -> data.setHeartRate(toInt(value));
                    case RESPIRATORY_RATE -> data.setRespiratoryRate(toInt(value));
                    case OXYGEN_SATURATION -> data.setOxygenSaturation(toInt(value));
                    case TEMPERATURE -> data.setTemperature(value);
                    case SYSTOLIC_PRESSURE -> data.setSystolicPressure(toInt(value));
                    case DIASTOLIC_PRESSURE -> data.setDiastolicPressure(toInt(value));
                    case PAIN -> data.setPain(toInt(value));
                }
            });
        }

        private static int toInt(BigDecimal value) {
            return value.setScale(0, RoundingMode.HALF_UP).intValue();
        }
    }
}
