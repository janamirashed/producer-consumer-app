package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Machine;
import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import com.producesconsumer.backend.observer.QueueObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

@Slf4j
public class MachineRunner implements Runnable, QueueObserver {
    private final Machine machine;
    private final List<Queue> inputQueues;
    private final List<Queue> outputQueues;
    private final QueueService queueService;
    private final SimulationService simulationService;

    private volatile boolean running = true;
    private final Random random = new Random();

    // Guarded Suspension
    public final Object lock = new Object();

    public MachineRunner(Machine machine,
            List<Queue> inputQueues,
            List<Queue> outputQueues,
            QueueService queueService,
            SimulationService simulationService) {
        this.machine = machine;
        this.inputQueues = inputQueues;
        this.outputQueues = outputQueues;
        this.queueService = queueService;
        this.simulationService = simulationService;
    }

    public void stop() {
        this.running = false;
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void run() {
        log.info("Machine {} started", machine.getId());
        // Listen to Q service for any product adds
        queueService.registerObserver(this);

        while (running) {
            try {
                // MACHINE CAN'T HAVE ZERO Qs as INPUT
                if (inputQueues.isEmpty()) {
                    log.warn("Machine {} has no input queues connected. Stopping.", machine.getId());
                    running = false;
                    break;
                }

                // Consumer Logic(fetch products)
                Product productToProcess = fetchProductFromInput();

                if (productToProcess == null) {
                    machine.setState("idle");

                    synchronized (lock) {
                        productToProcess = fetchProductFromInput();
                        if (productToProcess == null && running) {
                            lock.wait(); // Suspend
                            continue; // woke up
                        }
                    }
                }

                if (!running)
                    break;

                // Processing
                machine.setState("processing");
                machine.setCurrentProductColor(productToProcess.getColor());
                simulationService.broadcastMachineUpdate(machine); // SSE event for processing start
                simulationService.broadcastMachineFlash(machine.getId());

                Thread.sleep(machine.getProcessingTime());

                // Producer Logic(Send to next Q)
                if (!outputQueues.isEmpty()) {
                    Queue target = outputQueues.get(random.nextInt(outputQueues.size()));
                    queueService.addProductToQueue(target, productToProcess);
                } else {
                    log.info("TBD{}{}", productToProcess.getId(), machine.getId());
                }
                machine.setState("idle");
                machine.setCurrentProductColor(null);
                simulationService.broadcastMachineUpdate(machine); // SSE event for processing complete

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                log.warn("Machine {} error: {}", machine.getId(), e.getMessage());
            }
        }
        queueService.unregisterObserver(this);
        log.info("Machine {} stopped", machine.getId());
    }

    private Product fetchProductFromInput() {
        for (Queue queue : inputQueues) {
            Product p = queueService.removeProductFromQueue(queue);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void onProductAdded(Queue queue, Product product) {
        // Wake if product arrives in one of the input Qs
        if (inputQueues.stream().anyMatch(q -> q.getId().equals(queue.getId()))) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    // IGNORE
    @Override
    public void onProductRemoved(Queue queue, Product product) {
    }

    @Override
    public void onQueueEmpty(Queue queue) {
    }
}
