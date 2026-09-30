package com.tcc.triAI_api_java.entity.converter;

import com.tcc.triAI_api_java.enums.TriageColor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TriageColorConverter implements AttributeConverter<TriageColor, String> {

    @Override
    public String convertToDatabaseColumn(TriageColor attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TriageColor convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TriageColor.fromValue(dbData);
    }
}
