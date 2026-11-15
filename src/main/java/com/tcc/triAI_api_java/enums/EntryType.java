package com.tcc.triAI_api_java.enums;

public enum EntryType {
    FHIR("fhir");

    private final String value;

    EntryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EntryType fromValue(String value) {
        for (EntryType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown entry type: " + value);
    }
}
