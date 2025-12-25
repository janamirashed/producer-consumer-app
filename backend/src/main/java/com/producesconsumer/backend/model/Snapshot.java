package com.producesconsumer.backend.model;

import lombok.Data;

import java.time.Instant;

/**
 * Snapshot for replay functionality - pure data model (Memento)
 */
@Data
public class Snapshot {
    private String id;
    private Instant timestamp;
    private String label;
    private SimulationState state;

    public Snapshot() {
        this.timestamp = Instant.now();
    }
}
