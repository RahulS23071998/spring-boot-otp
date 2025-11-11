package com.starter.springboot.entity;

import com.starter.springboot.service.IdGeneratorService;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Entity listener that automatically generates Snowflake IDs for entities before they are persisted.
 * This listener is applied to entities that need distributed unique identifiers.
 */
@Component
public class IdGeneratorEntityListener {

    private static IdGeneratorService idGeneratorService;

    @Autowired
    public void setIdGeneratorService(IdGeneratorService idGeneratorService) {
        IdGeneratorEntityListener.idGeneratorService = idGeneratorService;
    }

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseEntityWithId baseEntity && (baseEntity.getId() == null || baseEntity.getId() == 0)) {
            baseEntity.setId(idGeneratorService.generateId());
        }
    }
}