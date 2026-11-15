package com.tcc.triAI_api_java.enums;

import java.util.Locale;

public enum TriageColor {
    BLUE("blue"),
    GREEN("green"),
    YELLOW("yellow"),
    ORANGE("orange"),
    RED("red");

    private final String value;

    TriageColor(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TriageColor fromValue(String value) {
        for (TriageColor color : values()) {
            if (color.value.equalsIgnoreCase(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown triage color: " + value);
    }

    public static TriageColor fromMlLabel(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "azul" -> BLUE;
            case "verde" -> GREEN;
            case "amarelo" -> YELLOW;
            case "laranja" -> ORANGE;
            case "vermelho" -> RED;
            default -> throw new IllegalArgumentException("Unknown triage color label from ML service: " + label);
        };
    }
}
