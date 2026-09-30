package com.tcc.triAI_api_java.entity.converter;

import com.tcc.triAI_api_java.enums.EntryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntryTypeConverter implements AttributeConverter<EntryType, String> {

    @Override
    public String convertToDatabaseColumn(EntryType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public EntryType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EntryType.fromValue(dbData);
    }
}
