package com.tcc.triAI_api_java.fhir;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class ObservationCodes {

    static final String CHIEF_COMPLAINT = "10154-3";
    static final Set<String> BLOOD_PRESSURE_PANELS = Set.of("85354-9", "55284-4", "75367002");

    private static final Map<String, VitalSign> SIGNS = Map.ofEntries(
            entry("8867-4", VitalSign.HEART_RATE),
            entry("8889-8", VitalSign.HEART_RATE),
            entry("364075005", VitalSign.HEART_RATE),
            entry("78564009", VitalSign.HEART_RATE),
            entry("9279-1", VitalSign.RESPIRATORY_RATE),
            entry("86290005", VitalSign.RESPIRATORY_RATE),
            entry("59408-5", VitalSign.OXYGEN_SATURATION),
            entry("2708-6", VitalSign.OXYGEN_SATURATION),
            entry("20564-1", VitalSign.OXYGEN_SATURATION),
            entry("431314004", VitalSign.OXYGEN_SATURATION),
            entry("8310-5", VitalSign.TEMPERATURE),
            entry("8331-1", VitalSign.TEMPERATURE),
            entry("8328-7", VitalSign.TEMPERATURE),
            entry("386725007", VitalSign.TEMPERATURE),
            entry("8480-6", VitalSign.SYSTOLIC_PRESSURE),
            entry("271649006", VitalSign.SYSTOLIC_PRESSURE),
            entry("8462-4", VitalSign.DIASTOLIC_PRESSURE),
            entry("271650006", VitalSign.DIASTOLIC_PRESSURE),
            entry("72514-3", VitalSign.PAIN),
            entry("38208-5", VitalSign.PAIN)
    );

    private ObservationCodes() {
    }

    static Optional<VitalSign> signOf(String code) {
        return Optional.ofNullable(SIGNS.get(code));
    }
}
