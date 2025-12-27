package com.producesconsumer.backend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Standard load from snapshot (always pauses simulation for safety)
     */
    public void loadFromSnapshot(SimulationSnapshot snapshot) {
        loadFromSnapshot(snapshot, false);
    }

    /**
     * Load from snapshot with option to preserve running state (for internal
     * backups)
     */
    public void loadFromSnapshot(SimulationSnapshot snapshot, boolean preserveRunningState) {
        SimulationState snapshotState = snapshot.getState();
        if (snapshotState == null) {
            System.err.println("Unable to load old state for snapshot " + snapshot.getId());
            return;
        }

        // Deep copy queues
        this.queues = snapshotState.getQueues().stream()
                .map(this::deepCopyQueue)
                .collect(Collectors.toCollection(ArrayList::new));

        // Deep copy machines
        this.machines = snapshotState.getMachines().stream()
                .map(this::deepCopyMachine)
                .collect(Collectors.toCollection(ArrayList::new));

        // Deep copy connections
        this.connections = snapshotState.getConnections().stream()
                .map(this::deepCopyConnection)
                .collect(Collectors.toCollection(ArrayList::new));

        if (preserveRunningState) {
            this.isRunning = snapshotState.isRunning();
        } else {
            this.isRunning = false; // standard snapshots are always paused
        }

        this.simulationId = snapshot.getId();
    }

    /**
     * Standard save to snapshot (always marks as paused)
     */
    public SimulationSnapshot saveToSnapshot(String label) {
        return saveToSnapshot(label, false);
    }

    /**
     * Save to snapshot with option to preserve running state
     */
    public SimulationSnapshot saveToSnapshot(String label, boolean preserveRunningState) {
        SimulationSnapshot snapshot = new SimulationSnapshot();
        snapshot.setLabel(label != null ? label : this.simulationId);
        snapshot.setId(this.simulationId);

        SimulationState stateCopy = new SimulationState();
        stateCopy.setSimulationId(this.simulationId);

        if (preserveRunningState) {
            stateCopy.setRunning(this.isRunning);
        } else {
            stateCopy.setRunning(false);
        }

        // Deep copy current data
        stateCopy.setQueues(this.queues.stream()
                .map(this::deepCopyQueue)
                .collect(Collectors.toCollection(ArrayList::new)));

        stateCopy.setMachines(this.machines.stream()
                .map(this::deepCopyMachine)
                .collect(Collectors.toCollection(ArrayList::new)));

        stateCopy.setConnections(this.connections.stream()
                .map(this::deepCopyConnection)
                .collect(Collectors.toCollection(ArrayList::new)));

        snapshot.setState(stateCopy);
        return snapshot;
    }

    private Queue deepCopyQueue(Queue original) {
        Queue copy = new Queue();
        copy.setId(original.getId());
        copy.setX(original.getX());
        copy.setY(original.getY());
        copy.setProductCount(original.getProductCount());

        // Deep copy products within queue
        if (original.getProducts() != null) {
            List<Product> productsCopy = original.getProducts().stream()
                    .map(this::deepCopyProduct)
                    .collect(Collectors.toList());
            copy.getProducts().addAll(productsCopy);
        }
        return copy;
    }

    private Machine deepCopyMachine(Machine original) {
        Machine copy = new Machine();
        copy.setId(original.getId());
        copy.setX(original.getX());
        copy.setY(original.getY());
        copy.setState(original.getState());
        copy.setProductCount(original.getProductCount());
        copy.setProcessingTime(original.getProcessingTime());
        copy.setCurrentProductColor(original.getCurrentProductColor());
        copy.setInputQueueId(original.getInputQueueId());
        copy.setOutputQueueId(original.getOutputQueueId());
        return copy;
    }

    private Connection deepCopyConnection(Connection original) {
        Connection copy = new Connection();
        copy.setId(original.getId());
        copy.setSourceId(original.getSourceId());
        copy.setSourceType(original.getSourceType());
        copy.setTargetId(original.getTargetId());
        copy.setTargetType(original.getTargetType());
        return copy;
    }

    private Product deepCopyProduct(Product original) {
        Product copy = new Product();
        copy.setId(original.getId());
        copy.setColor(original.getColor());
        return copy;
    }
}
