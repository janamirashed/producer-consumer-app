package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.*;
import com.producesconsumer.backend.model.Queue;
import com.producesconsumer.backend.observer.QueueEventObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private final MachineProcessingService machineProcessingService;
    private InputGenerator currentGenerator;
    private Future<?> generatorFuture;
    private Map<String, Queue> queueMap = new TreeMap<>((s1, s2) -> {
        int v1 = Integer.parseInt(s1.replace("Q", ""));
        int v2 = Integer.parseInt(s2.replace("Q", ""));
        return Integer.compare(v1, v2);
    });

    private final ExecutorService generatorExecutor = Executors.newSingleThreadExecutor();

    private int queueCounter = 0;
    private int machineCounter = 0;
    private int connectionCounter = 0;
    private SimulationSnapshot liveSessionBackup;
    private boolean inReplayMode = false;

    // ==================== State Access ====================

    public SimulationState getState() {
        return state;
    }

    // ==================== Queue Operations ====================

//    public Queue addQueue(double x, double y) {
//        Queue queue = new Queue();
//        queue.setId("Q" + (queueCounter++));
//        queue.setX(x);
//        queue.setY(y);
//        state.getQueues().add(queue);
//
//        // register observer for this queue
//        queueService.registerObserver(queueEventObserver);
//        log.info("Queue observer registered for queue: {}", queue.getId());
//
//        log.info("Added queue: {}", queue.getId());
//        broadcastState();
//        return queue;
//    }

    public Queue addQueue(double x, double y) {
        Queue queue = new Queue();

        int lastVal = -1;
        int queueId;

        for(Map.Entry<String, Queue> entry : queueMap.entrySet()) {
            int curr_val = Integer.parseInt(entry.getKey().replace("Q",""));
            if(curr_val - lastVal > 1) {
                break;
            }
            lastVal = curr_val;
        }
        queueId = lastVal + 1;

        queue.setId("Q" + queueId);
        queue.setX(x);
        queue.setY(y);
        state.getQueues().add(queue);

        // register observer for this queue
        queueService.registerObserver(queueEventObserver);
        queueMap.put(queue.getId(), queue);
        log.info("Queue observer registered for queue: {}", queue.getId());

        log.info("Added queue: {}", queue.getId());
        broadcastState();
        return queue;
    }

    public void deleteQueue(String id) {
        state.getQueues().removeIf(q -> q.getId().equals(id));
        state.getConnections().removeIf(c -> c.getSourceId().equals(id) || c.getTargetId().equals(id));

        queueService.unregisterObserver(queueEventObserver);
        queueMap.remove(id);
        log.info("Queue observer unregistered for queue: {}", id);

        log.info("Deleted queue: {}", id);
        broadcastState();
    }

    public void updateQueuePosition(String id, double x, double y) {
        state.getQueues().stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .ifPresent(q -> {
                    q.setX(x);
                    q.setY(y);
                });
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
        machine.setProcessingTime(3000 + (int) (Math.random() * 4000)); // 3-7 seconds
        state.getMachines().add(machine);
        log.info("Added machine: {}", machine.getId());
        broadcastState();
        return machine;
    }

    public void deleteMachine(String id) {
        state.getMachines().removeIf(m -> m.getId().equals(id));
        state.getConnections().removeIf(c -> c.getSourceId().equals(id) || c.getTargetId().equals(id));
        log.info("Deleted machine: {}", id);
        broadcastState();
    }

    public void updateMachinePosition(String id, double x, double y) {
        state.getMachines().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .ifPresent(m -> {
                    m.setX(x);
                    m.setY(y);
                });
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

        // Map setup for search
        queueMap = state.getQueues().stream().collect(Collectors.toMap(Queue::getId, q -> q));

        // Start machines(processing)
        for (Machine machine : state.getMachines()) {
            List<Queue> inputQueues = state.getConnections().stream()// stream of connections
                    .filter(c -> c.getTargetId().equals(machine.getId())) // only connections that end at this machine
                    .map(c -> queueMap.get(c.getSourceId())) // connection -> queue
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Queue> outputQueues = state.getConnections().stream()// stream of connections
                    .filter(c -> c.getSourceId().equals(machine.getId())) // connections that START at this machine
                    .map(c -> queueMap.get(c.getTargetId())) // get the target queue
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Starting machine {} with {} input queues and {} output queues",
                    machine.getId(), inputQueues.size(), outputQueues.size());
            machineProcessingService.startProcessing(machine, inputQueues, outputQueues);
        }

        // Input generation(source Q0)
        Queue q0 = queueMap.get("Q0");

        if (q0 != null) {
            currentGenerator = new InputGenerator(q0, queueService);
            generatorFuture = generatorExecutor.submit(currentGenerator);
        } else {
            log.warn("Q0 not found! No products will be generated.");
        }

        eventService.publishEvent(new SSE("SIMULATION_STARTED", null));
        broadcastState();
    }

    public void stopSimulation() {
        state.setRunning(false);

        // Stop Machines
        machineProcessingService.stopAll();

        // Stop generator
        if (currentGenerator != null) {
            currentGenerator.stop();
        }
        if (generatorFuture != null) {
            generatorFuture.cancel(true);
            generatorFuture = null;
        }

        log.info("Simulation stopped");
        eventService.publishEvent(new SSE("SIMULATION_STOPPED", null));
        broadcastState();
    }



    public SimulationState newSimulation() {
        stopSimulation();
        queueMap.clear();
        state.getQueues().clear();
        state.getMachines().clear();
        state.getConnections().clear();
        queueCounter = 0;
        machineCounter = 0;
        connectionCounter = 0;
        liveSessionBackup = null;
        log.info("New simulation created");
        broadcastState();
        return state;
    }

    public void backupLiveState() {
        // Save current state including running status
        if (this.inReplayMode)
            return;
        // do not update the liveSession since it's also a replay
        this.liveSessionBackup = state.saveToSnapshot("_internal_backup", true);
        this.inReplayMode = true;
        // next backup will consider state as a ReplayState
        log.info("Live session backed up");
    }



    public SimulationState restoreLiveState() {
        if (liveSessionBackup != null) {
            log.info("Restoring live session from backup");
            boolean wasRunning = liveSessionBackup.getState().isRunning();

            // Stop any current replay before restoring
            stopSimulation();

            // Load state preserving the 'wasRunning' flag
            state.loadFromSnapshot(liveSessionBackup, true);

            if (wasRunning) {
                log.info("Resuming simulation threads after restoration");
                startSimulation();
            } else {
                broadcastState();
            }
            return state;
        }
        log.warn("No live session backup found to restore");
        this.inReplayMode = false;
        //im not in Replay Mode anymore
        return state;
    }

    public void restartSimulation() {
        log.info("Restarting simulation: stopping and clearing counts");
        stopSimulation();

        // Clear product counts from queues and machines
        for (Queue queue : state.getQueues()) {
            queue.setProductCount(0);
            queue.getProducts().clear();
        }
        for (Machine machine : state.getMachines()) {
            machine.setProductCount(0);
            machine.setCurrentProductColor(null);
            machine.setState("idle");
        }

        // Start again
        startSimulation();
    }

    // ==================== Event Broadcasting ====================

    public void broadcastState() {
        eventService.publishEvent(new SSE("STATE_UPDATE", state));
    }

    public void broadcastMachineFlash(String machineId) {
        eventService.publishEvent(new SSE("MACHINE_FLASH", machineId));
    }

    public void broadcastMachineUpdate(Machine machine) {
        eventService.publishEvent(new SSE("MACHINE_UPDATE", machine));
    }
}
