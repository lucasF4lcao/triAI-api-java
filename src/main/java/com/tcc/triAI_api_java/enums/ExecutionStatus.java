package com.tcc.triAI_api_java.enums;

public enum ExecutionStatus {
    SUCCESS("success"),
    ERROR("error");

    private final String value;

    ExecutionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ExecutionStatus fromValue(String value) {
        for (ExecutionStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + value);
    }
}
