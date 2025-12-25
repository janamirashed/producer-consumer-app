package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Machine;
import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

/**
 * Machine processing service - single responsibility: Machine thread execution and processing logic
 */
@Service
public class MachineProcessingService {

    private final ExecutorService executorService;

    public MachineProcessingService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Start processing on a machine (runs in separate thread)
     */
    public void startProcessing(Machine machine, Queue inputQueue, Queue outputQueue) {
        // TODO: Implement
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
        // TODO: Implement
    }
}
