package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Machine;
import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Machine processing service - single responsibility: Machine thread execution
 * and processing logic
 */
@Service
public class MachineProcessingService {

    private final ExecutorService executorService;
    private final Map<String, MachineRunner> activeRunners = new ConcurrentHashMap<>(); // handles multiple threads
                                                                                        // accessing it at the same time

    private final QueueService queueService;
    private final SimulationService simulationService;

    public MachineProcessingService(ExecutorService executorService,
            QueueService queueService,
            @Lazy SimulationService simulationService) {
        this.executorService = executorService;
        this.queueService = queueService;
        this.simulationService = simulationService;
    }

    /**
     * Start processing on a machine (runs in separate thread)
     */
    public void startProcessing(Machine machine, List<Queue> inputs, List<Queue> outputs) {
        if (activeRunners.containsKey(machine.getId())) {
            return;
        }
        MachineRunner runner = new MachineRunner(
                machine, inputs, outputs, queueService, simulationService);
        activeRunners.put(machine.getId(), runner);
        executorService.submit(runner);
    }

    /**
     * Stop processing on a machine
     */
    public void stopProcessing(Machine machine) {
        // TODO: Implement
    }

    /**
     * Stop all machine processing
     */
    public void stopAll() {
        activeRunners.values().forEach(MachineRunner::stop);
        activeRunners.clear();
    }
}
