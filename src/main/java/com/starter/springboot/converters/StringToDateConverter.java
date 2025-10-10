package com.starter.springboot.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class StringToDateConverter implements AttributeConverter<String, Date> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringToDateConverter.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Override
    public Date convertToDatabaseColumn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(value);
        } catch (ParseException exception) {
            LOGGER.warn("Unable to parse date string '{}' using pattern {}", value, DATE_PATTERN, exception);
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}
