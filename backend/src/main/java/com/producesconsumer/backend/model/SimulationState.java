package com.producesconsumer.backend.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
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
        this.queues = new ArrayList<>();
        this.machines = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.isRunning = false;
    }

    public void loadFromSnapshot(SimulationSnapshot snapshot) {
        SimulationState oldState = snapshot.getState();
        if (oldState == null){
            System.err.println("Unable to load old state for snapshot " + snapshot.getId());
            return;
        }
        this.queues = List.copyOf(oldState.getQueues());
        this.machines = List.copyOf(oldState.getMachines());
        this.connections = List.copyOf(oldState.getConnections());
        // so that if we want to cache the snapshot to access it faster we do not modify the snapshot data and keep it intact
        this.isRunning = false;
        this.simulationId = snapshot.getId();
    }

    public SimulationSnapshot saveToSnapshot(String label) {
        SimulationSnapshot snapshot = new SimulationSnapshot();
        snapshot.setLabel(label != null ? label : this.simulationId);

        SimulationState newState = new SimulationState();
        newState.setMachines(List.copyOf(this.machines));
        newState.setQueues(List.copyOf(this.queues));
        newState.setConnections(List.copyOf(this.connections));
        newState.setSimulationId(this.simulationId);
        // to make sure we get a copy of the data that is irrespective of the modifications that the state undergoes
        // after the snapshot was saved which is the essence of snapshots.

        snapshot.setState(newState);
        snapshot.setId(this.simulationId);
        return snapshot;
    }
}
