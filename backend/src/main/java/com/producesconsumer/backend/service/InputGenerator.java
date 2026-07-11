package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import com.producesconsumer.backend.model.SimulationState;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class InputGenerator implements Runnable {
    private final Queue targetQueue;
    private final QueueService queueService;
    private final SimulationState state; // Reference to update total count
    private volatile boolean running = true;
    private final Random random = new Random();

    public InputGenerator(Queue targetQueue, QueueService queueService, SimulationState state) {
        this.targetQueue = targetQueue;
        this.queueService = queueService;
        this.state = state;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Generate products every 2-2.5 second (faster than machines can process)
                int delay = 2000 + random.nextInt(500);
                Thread.sleep(delay);

                if (!running)
                    break;

                // Product random creation
                Product product = new Product();
                product.setId("PROD-" + System.nanoTime());
                product.setColor(getNextColor());

                // Increment total products generated counter BEFORE adding to queue
                // so that any SSE events have the updated count
                state.setTotalProductsGenerated(state.getTotalProductsGenerated() + 1);

                // Put product in target queue (this publishes QUEUE_EVENT)
                queueService.addProductToQueue(targetQueue, product);

                log.info("Generated Product {} into Queue {} (Total: {})",
                        product.getId(), targetQueue.getId(), state.getTotalProductsGenerated());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("InputGenerator Stopped");
    }

    // 10 predefined visible colors
    private static final String[] COLORS = {
            "#e74c3c", // Red
            "#3498db", // Blue
            "#130ac5ff", // Not HCI
            "#f39c12", // Orange
            "#9b59b6", // Purple
            "#1abc9c", // Teal
            "#e91e63", // Pink
            "#00bcd4", // Cyan
            "#ff9800", // Amber
            "#ed0f9cff" // برجندي
    };
    private int colorIndex = 0;

    private String getNextColor() {
        String color = COLORS[colorIndex];
        colorIndex = (colorIndex + 1) % COLORS.length;
        return color;
    }
}
