package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.*;
import com.producesconsumer.backend.observer.QueueEventObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Main simulation service - handles all state management and simulation control
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final EventService eventService;
    private final SimulationState state = new SimulationState();

    private final QueueService queueService;
    private final QueueEventObserver queueEventObserver;

    private int queueCounter = 0;
    private int machineCounter = 0;
    private int connectionCounter = 0;

    // ==================== State Access ====================

    public SimulationState getState() {
        return state;
    }

    // ==================== Queue Operations ====================

    public Queue addQueue(double x, double y) {
        Queue queue = new Queue();
        queue.setId("Q" + (++queueCounter));
        queue.setX(x);
        queue.setY(y);
        state.getQueues().add(queue);

        // register observer for this queue
        queueService.registerObserver(queueEventObserver);
        log.info("Queue observer registered for queue: {}", queue.getId());

        log.info("Added queue: {}", queue.getId());
        broadcastState();
        return queue;
    }

    public void deleteQueue(String id) {
        state.getQueues().removeIf(q -> q.getId().equals(id));
        state.getConnections().removeIf(c -> 
            c.getSourceId().equals(id) || c.getTargetId().equals(id));

        queueService.unregisterObserver(queueEventObserver);
        log.info("Queue observer unregistered for queue: {}", id);

        log.info("Deleted queue: {}", id);
        broadcastState();
    }

    public void updateQueuePosition(String id, double x, double y) {
        state.getQueues().stream()
            .filter(q -> q.getId().equals(id))
            .findFirst()
            .ifPresent(q -> { q.setX(x); q.setY(y); });
        broadcastState();
    }

    public void addProductToQueue(String queueId, Product product) {
        Queue queue = state.getQueues().stream()
                .filter(q -> q.getId().equals(queueId))
                .findFirst()
                .orElse(null);

        if (queue != null) {
            queueService.addProductToQueue(queue, product);
            log.info("Added product {} to queue {}", product.getId(), queueId);
        } else {
            log.warn("Queue not found: {}", queueId);
        }
    }

    public Product removeProductFromQueue(String queueId) {
        Queue queue = state.getQueues().stream()
                .filter(q -> q.getId().equals(queueId))
                .findFirst()
                .orElse(null);

        if (queue != null) {
            Product product = queueService.removeProductFromQueue(queue);
            if (product != null) {
                log.info("Removed product {} from queue {}", product.getId(), queueId);
            }
            return product;
        } else {
            log.warn("Queue not found: {}", queueId);
            return null;
        }
    }

    public Queue getQueueById(String queueId) {
        return state.getQueues().stream()
                .filter(q -> q.getId().equals(queueId))
                .findFirst()
                .orElse(null);
    }

    // ==================== Machine Operations ====================

    public Machine addMachine(double x, double y) {
        Machine machine = new Machine();
        machine.setId("M" + (++machineCounter));
        machine.setX(x);
        machine.setY(y);
        machine.setState("idle");
        machine.setProcessingTime(1000 + (int)(Math.random() * 4000));
        state.getMachines().add(machine);
        log.info("Added machine: {}", machine.getId());
        broadcastState();
        return machine;
    }

    public void deleteMachine(String id) {
        state.getMachines().removeIf(m -> m.getId().equals(id));
        state.getConnections().removeIf(c -> 
            c.getSourceId().equals(id) || c.getTargetId().equals(id));
        log.info("Deleted machine: {}", id);
        broadcastState();
    }

    public void updateMachinePosition(String id, double x, double y) {
        state.getMachines().stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .ifPresent(m -> { m.setX(x); m.setY(y); });
        broadcastState();
    }

    // ==================== Connection Operations ====================

    public Connection addConnection(String sourceId, String sourceType,
                                    String targetId, String targetType) {
        Connection connection = new Connection();
        connection.setId("C" + (++connectionCounter));
        connection.setSourceId(sourceId);
        connection.setSourceType(sourceType);
        connection.setTargetId(targetId);
        connection.setTargetType(targetType);
        state.getConnections().add(connection);
        log.info("Added connection: {} -> {}", sourceId, targetId);
        broadcastState();
        return connection;
    }

    public void deleteConnection(String id) {
        state.getConnections().removeIf(c -> c.getId().equals(id));
        log.info("Deleted connection: {}", id);
        broadcastState();
    }

    // ==================== Simulation Control ====================

    public void startSimulation() {
        state.setRunning(true);
        log.info("Simulation started");
        eventService.publishEvent(new SSE("SIMULATION_STARTED", null));
        broadcastState();
    }

    public void stopSimulation() {
        state.setRunning(false);
        log.info("Simulation stopped");
        eventService.publishEvent(new SSE("SIMULATION_STOPPED", null));
        broadcastState();
    }

    public SimulationState newSimulation() {
        stopSimulation();
        state.getQueues().clear();
        state.getMachines().clear();
        state.getConnections().clear();
        queueCounter = 0;
        machineCounter = 0;
        connectionCounter = 0;
        log.info("New simulation created");
        broadcastState();
        return state;
    }

    // ==================== Event Broadcasting ====================

    private void broadcastState() {
        eventService.publishEvent(new SSE("STATE_UPDATE", state));
    }

    public void broadcastMachineFlash(String machineId) {
        eventService.publishEvent(new SSE("MACHINE_FLASH", machineId));
    }
}
