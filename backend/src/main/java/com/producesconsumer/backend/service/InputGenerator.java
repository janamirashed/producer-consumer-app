package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class InputGenerator implements Runnable {
    private final Queue targetQueue;
    private final QueueService queueService;
    private volatile boolean running = true;
    private final Random random = new Random();

    public InputGenerator(Queue targetQueue, QueueService queueService) {
        this.targetQueue = targetQueue;
        this.queueService = queueService;
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
                product.setColor(generateRandomHexColor());

                // Put product in target queue
                queueService.addProductToQueue(targetQueue, product);
                log.info("Generated Product {} into Queue {}", product.getId(), targetQueue.getId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("InputGenerator Stopped");
    }

    private String generateRandomHexColor() {
        int nextInt = random.nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }
}
