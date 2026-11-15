package com.tcc.triAI_api_java.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnitConverterTest {

    private static BigDecimal convert(VitalSign sign, String value, String unit) {
        return UnitConverter.toCanonical(sign, new BigDecimal(value), unit).orElse(null);
    }

    @Test
    void convertsKelvinAndFahrenheitToCelsius() {
        assertThat(convert(VitalSign.TEMPERATURE, "310.15", "K")).isEqualByComparingTo("37.00");
        assertThat(convert(VitalSign.TEMPERATURE, "212", "[degF]")).isEqualByComparingTo("100.00");
        assertThat(convert(VitalSign.TEMPERATURE, "37", "Cel")).isEqualByComparingTo("37");
        assertThat(convert(VitalSign.TEMPERATURE, "37", "°C")).isEqualByComparingTo("37");
    }

    @Test
    void keepsAPlausibleCelsiusValueWhenTheUnitIsMissing() {
        assertThat(convert(VitalSign.TEMPERATURE, "36.8", null)).isEqualByComparingTo("36.8");
    }

    @Test
    void convertsKilopascalsToMillimetersOfMercury() {
        assertThat(convert(VitalSign.SYSTOLIC_PRESSURE, "16", "kPa")).isEqualByComparingTo("120");
        assertThat(convert(VitalSign.DIASTOLIC_PRESSURE, "80", "mm[Hg]")).isEqualByComparingTo("80");
    }

    @Test
    void convertsSaturationFractionsToPercent() {
        assertThat(convert(VitalSign.OXYGEN_SATURATION, "0.95", "1")).isEqualByComparingTo("95");
        assertThat(convert(VitalSign.OXYGEN_SATURATION, "95", "%")).isEqualByComparingTo("95");
    }

    @Test
    void acceptsCommonRateUnitsAndRejectsUnknownOnes() {
        assertThat(convert(VitalSign.HEART_RATE, "80", "{beats}/min")).isEqualByComparingTo("80");
        assertThat(convert(VitalSign.RESPIRATORY_RATE, "16", "/min")).isEqualByComparingTo("16");
        assertThat(convert(VitalSign.HEART_RATE, "80", "furlong")).isNull();
    }

    @Test
    void acceptsAnyUnitForPain() {
        assertThat(convert(VitalSign.PAIN, "7", "{score}")).isEqualByComparingTo("7");
    }
}
