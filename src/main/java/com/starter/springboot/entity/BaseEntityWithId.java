package com.starter.springboot.entity;

/**
 * Interface for entities that require automatic ID generation using Snowflake IDs.
 * Entities implementing this interface will have their IDs automatically generated
 * by the IdGeneratorEntityListener before being persisted.
 */
public interface BaseEntityWithId {

    Long getId();

    void setId(Long id);
}