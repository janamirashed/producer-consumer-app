package com.producesconsumer.backend.observer;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;

/**
 * Observer interface for Queue updates (Observer pattern)
 */
public interface QueueObserver {
    /**
     * Called when a product is added to a queue
     */
    void onProductAdded(Queue queue, Product product);

    /**
     * Called when a product is removed from a queue
     */
    void onProductRemoved(Queue queue, Product product);

    /**
     * Called when a queue becomes empty
     */
    void onQueueEmpty(Queue queue);
}
