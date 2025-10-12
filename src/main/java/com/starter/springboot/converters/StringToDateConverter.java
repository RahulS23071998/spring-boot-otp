package com.starter.springboot.converters;

import com.starter.springboot.constants.ValidationConstants;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class StringToDateConverter implements AttributeConverter<String, Date> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringToDateConverter.class);

    @Override
    public Date convertToDatabaseColumn(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }
        try {
            return new SimpleDateFormat(ValidationConstants.DATE_PATTERN).parse(value);
        } catch (ParseException exception) {
            LOGGER.warn("Unable to parse date string '{}' using pattern {}", value, ValidationConstants.DATE_PATTERN, exception);
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return new SimpleDateFormat(ValidationConstants.DATE_PATTERN).format(date);
    }
}
