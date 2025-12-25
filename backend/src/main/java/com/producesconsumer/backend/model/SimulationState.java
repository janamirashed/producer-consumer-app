package com.producesconsumer.backend.model;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Complete simulation state - pure data model
 */
@Data
public class SimulationState {
    private List<Queue> queues;
    private List<Machine> machines;
    private List<Connection> connections;
    private boolean isRunning;
    private String simulationId;

    public SimulationState() {
        this.queues = new CopyOnWriteArrayList<>();
        this.machines = new CopyOnWriteArrayList<>();
        this.connections = new CopyOnWriteArrayList<>();
        this.isRunning = false;
    }
}
