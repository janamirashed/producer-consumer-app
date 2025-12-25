package com.producesconsumer.backend.observer;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;

public interface QueueObserver {
    void onProductAdded(Queue queue, Product product);

    void onProductRemoved(Queue queue, Product product);

    void onQueueEmpty(Queue queue);
}
