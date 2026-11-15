package com.tcc.triAI_api_java.entity.converter;

import com.tcc.triAI_api_java.enums.ExecutionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExecutionStatusConverter implements AttributeConverter<ExecutionStatus, String> {

    @Override
    public String convertToDatabaseColumn(ExecutionStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ExecutionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExecutionStatus.fromValue(dbData);
    }
}
