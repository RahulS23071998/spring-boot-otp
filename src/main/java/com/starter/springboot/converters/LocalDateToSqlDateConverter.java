package com.starter.springboot.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.sql.Date;
import java.util.Objects;


@Converter
public class LocalDateToSqlDateConverter implements AttributeConverter<LocalDate, Date> {

    @Override
    public Date convertToDatabaseColumn(LocalDate localDate) {
        return ( Objects.isNull(localDate) ? null : Date.valueOf(localDate) );
    }

    @Override
    public LocalDate convertToEntityAttribute(Date date) {
        return ( Objects.isNull(date) ? null : date.toLocalDate() );
    }
}
