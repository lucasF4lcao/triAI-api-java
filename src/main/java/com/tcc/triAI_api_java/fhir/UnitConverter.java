package com.tcc.triAI_api_java.fhir;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class UnitConverter {

    private static final Set<String> CELSIUS = Set.of("cel", "°c", "c", "degc", "celsius");
    private static final Set<String> FAHRENHEIT = Set.of("[degf]", "°f", "f", "degf", "fahrenheit");
    private static final Set<String> PER_MINUTE = Set.of(
            "/min", "1/min", "min-1", "{beats}/min", "{breaths}/min", "beats/min", "breaths/min", "bpm", "rpm", "irpm");
    private static final Set<String> PERCENT = Set.of("%", "{percent}", "percent");
    private static final Set<String> MM_HG = Set.of("mm[hg]", "mmhg");

    private static final BigDecimal FAHRENHEIT_OFFSET = BigDecimal.valueOf(32);
    private static final BigDecimal FAHRENHEIT_RATIO = BigDecimal.valueOf(5).divide(BigDecimal.valueOf(9), 10, RoundingMode.HALF_UP);
    private static final BigDecimal KELVIN_OFFSET = new BigDecimal("273.15");
    private static final BigDecimal KPA_TO_MM_HG = new BigDecimal("7.50062");
    private static final BigDecimal FAHRENHEIT_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private UnitConverter() {
    }

    static Optional<BigDecimal> toCanonical(VitalSign sign, BigDecimal value, String unit) {
        String normalized = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT).replace(" ", "");

        return switch (sign) {
            case TEMPERATURE -> temperature(value, normalized);
            case HEART_RATE, RESPIRATORY_RATE -> normalized.isEmpty() || PER_MINUTE.contains(normalized)
                    ? Optional.of(value) : Optional.empty();
            case OXYGEN_SATURATION -> oxygenSaturation(value, normalized);
            case SYSTOLIC_PRESSURE, DIASTOLIC_PRESSURE -> pressure(value, normalized);
            case PAIN -> Optional.of(value);
        };
    }

    private static Optional<BigDecimal> temperature(BigDecimal value, String unit) {
        if (unit.isEmpty()) {
            return Optional.of(value.compareTo(FAHRENHEIT_THRESHOLD) > 0 ? fahrenheitToCelsius(value) : value);
        }
        if (CELSIUS.contains(unit)) {
            return Optional.of(value);
        }
        if (FAHRENHEIT.contains(unit)) {
            return Optional.of(fahrenheitToCelsius(value));
        }
        if (unit.equals("k")) {
            return Optional.of(value.subtract(KELVIN_OFFSET).setScale(2, RoundingMode.HALF_UP));
        }
        return Optional.empty();
    }

    private static BigDecimal fahrenheitToCelsius(BigDecimal fahrenheit) {
        return fahrenheit.subtract(FAHRENHEIT_OFFSET).multiply(FAHRENHEIT_RATIO).setScale(2, RoundingMode.HALF_UP);
    }

    private static Optional<BigDecimal> oxygenSaturation(BigDecimal value, String unit) {
        if (unit.isEmpty() || PERCENT.contains(unit)) {
            return Optional.of(value);
        }
        if (unit.equals("1")) {
            return Optional.of(value.compareTo(ONE) <= 0 ? value.multiply(HUNDRED) : value);
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> pressure(BigDecimal value, String unit) {
        if (unit.isEmpty() || MM_HG.contains(unit)) {
            return Optional.of(value);
        }
        if (unit.equals("kpa")) {
            return Optional.of(value.multiply(KPA_TO_MM_HG).setScale(0, RoundingMode.HALF_UP));
        }
        return Optional.empty();
    }
}
