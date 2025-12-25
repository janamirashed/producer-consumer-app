package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import com.producesconsumer.backend.observer.QueueObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/// manages all queue operations and observer notifications
/// handles product addition/removal and notifies all registered observers

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {

    private final EventService eventService;
    private final List<QueueObserver> observers = new CopyOnWriteArrayList<>();

    /// register an observer to be notified of queue changes
    public void registerObserver(QueueObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.debug("Observer registered. Total observers: {}", observers.size());
        }
    }

    /// unregister an observer
    public void unregisterObserver(QueueObserver observer) {
        observers.remove(observer);
        log.debug("Observer unregistered. Total observers: {}", observers.size());
    }

    /// add a product to a queue (thread-safe)
    /// notifies all observers of the change
    public synchronized void addProductToQueue(Queue queue, Product product) {
        if (queue == null || product == null) {
            log.warn("Cannot add product: queue or product is null");
            return;
        }

        try {
            // add product to queue (thread-safe list)
            queue.getProducts().add(product);

            // update product count
            queue.setProductCount(queue.getProducts().size());

            log.info("Product {} added to queue {}. New size: {}",
                    product.getId(), queue.getId(), queue.getProductCount());

            // notify all observers
            notifyObserversProductAdded(queue, product);

            // publish SSE event for frontend
            publishQueueUpdateEvent(queue, "PRODUCT_ADDED");

        } catch (Exception e) {
            log.error("Error adding product to queue: {}", e.getMessage(), e);
        }
    }

    /// remove a product from queue
    /// notifies all observers of the change
    /// returns the removed product, or null if queue is empty
    public synchronized Product removeProductFromQueue(Queue queue) {
        if (queue == null || queue.getProducts().isEmpty()) {
            if (queue != null) {
                log.warn("Cannot remove product from queue {}: queue is empty", queue.getId());
                notifyObserversQueueEmpty(queue);
            }
            return null;
        }

        try {
            // remove first product (FIFO)
            Product product = queue.getProducts().remove(0);

            // update product count
            queue.setProductCount(queue.getProducts().size());

            log.info("Product {} removed from queue {}. New size: {}",
                    product.getId(), queue.getId(), queue.getProductCount());

            // notify all observers
            notifyObserversProductRemoved(queue, product);

            // publish SSE event for frontend
            publishQueueUpdateEvent(queue, "PRODUCT_REMOVED");

            // check if queue is empty
            if (queue.getProducts().isEmpty()) {
                notifyObserversQueueEmpty(queue);
            }

            return product;

        } catch (Exception e) {
            log.error("Error removing product from queue: {}", e.getMessage(), e);
            return null;
        }
    }

    /// get next product without removing it (peek operation)
    public synchronized Product peekQueue(Queue queue) {
        if (queue == null || queue.getProducts().isEmpty()) {
            return null;
        }
        return queue.getProducts().get(0);
    }

    /// check if queue is empty
    public synchronized boolean isQueueEmpty(Queue queue) {
        return queue == null || queue.getProducts().isEmpty();
    }

    /// get current queue size
    public synchronized int getQueueSize(Queue queue) {
        return queue == null ? 0 : queue.getProducts().size();
    }

    /// clear all products from a queue
    public synchronized void clearQueue(Queue queue) {
        if (queue == null) {
            return;
        }
        queue.getProducts().clear();
        queue.setProductCount(0);
        log.info("Queue {} cleared", queue.getId());
        notifyObserversQueueEmpty(queue);
        publishQueueUpdateEvent(queue, "QUEUE_CLEARED");
    }

    /// private notification methods

    /// notify all observers that a product was added
    private void notifyObserversProductAdded(Queue queue, Product product) {
        for (QueueObserver observer : observers) {
            try {
                observer.onProductAdded(queue, product);
            } catch (Exception e) {
                log.error("Error notifying observer of product addition: {}", e.getMessage(), e);
            }
        }
    }

    /// notify all observers that a product was removed
    private void notifyObserversProductRemoved(Queue queue, Product product) {
        for (QueueObserver observer : observers) {
            try {
                observer.onProductRemoved(queue, product);
            } catch (Exception e) {
                log.error("Error notifying observer of product removal: {}", e.getMessage(), e);
            }
        }
    }

    /// notify all observers that queue is empty
    private void notifyObserversQueueEmpty(Queue queue) {
        for (QueueObserver observer : observers) {
            try {
                observer.onQueueEmpty(queue);
            } catch (Exception e) {
                log.error("Error notifying observer of empty queue: {}", e.getMessage(), e);
            }
        }
    }

    /// publish queue update event via SSE
    private void publishQueueUpdateEvent(Queue queue, String action) {
        try {
            QueueUpdatePayload payload = new QueueUpdatePayload(
                    queue.getId(),
                    queue.getX(),
                    queue.getY(),
                    queue.getProductCount(),
                    queue.getProducts(),
                    action
            );
            eventService.publishEvent(
                    new com.producesconsumer.backend.model.SSE("QUEUE_UPDATE", payload)
            );
        } catch (Exception e) {
            log.error("Error publishing queue update event: {}", e.getMessage(), e);
        }
    }

    /// inner class for queue update payload sent via SSE
    public static class QueueUpdatePayload {
        public String id;
        public double x;
        public double y;
        public int productCount;
        public List<Product> products;
        public String action;

        public QueueUpdatePayload(String id, double x, double y, int productCount,
                                  List<Product> products, String action) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.productCount = productCount;
            this.products = products;
            this.action = action;
        }
    }
}
