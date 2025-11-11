package com.starter.springboot.service;

import com.starter.springboot.utils.IdWorker;
import org.springframework.stereotype.Service;

/**
 * Service for generating unique Snowflake IDs using the IdWorker algorithm.
 * Provides centralized ID generation for entities that need distributed unique identifiers.
 */
@Service
public class IdGeneratorService {

    private final IdWorker idWorker;

    public IdGeneratorService() {
        this.idWorker = new IdWorker();
    }

    /**
     * Generates a unique Snowflake ID.
     *
     * @return A unique 64-bit ID containing timestamp, datacenter, worker, and sequence information
     */
    public Long generateId() {
        return idWorker.nextId();
    }
}