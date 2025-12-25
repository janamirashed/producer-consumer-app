package com.producesconsumer.backend.observer;

import com.producesconsumer.backend.model.Product;
import com.producesconsumer.backend.model.Queue;
import com.producesconsumer.backend.model.SSE;
import com.producesconsumer.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class QueueEventObserver implements QueueObserver {

    private final EventService eventService;

    /// called when a product is added to a queue
    /// publishes SSE event so frontend updates in real-time
    @Override
    public void onProductAdded(Queue queue, Product product) {
        log.debug("Product added to queue {}: {}", queue.getId(), product.getId());

        try {
            QueueEventPayload payload = new QueueEventPayload(
                    "PRODUCT_ADDED",
                    queue.getId(),
                    product.getId(),
                    product.getColor(),
                    queue.getProductCount()
            );

            eventService.publishEvent(new SSE("QUEUE_EVENT", payload));

        } catch (Exception e) {
            log.error("Error publishing product added event: {}", e.getMessage(), e);
        }
    }

    /// called when a product is removed from a queue
    /// publishes SSE event so frontend updates in real-time
    @Override
    public void onProductRemoved(Queue queue, Product product) {
        log.debug("Product removed from queue {}: {}", queue.getId(), product.getId());

        try {
            QueueEventPayload payload = new QueueEventPayload(
                    "PRODUCT_REMOVED",
                    queue.getId(),
                    product.getId(),
                    product.getColor(),
                    queue.getProductCount()
            );

            eventService.publishEvent(new SSE("QUEUE_EVENT", payload));

        } catch (Exception e) {
            log.error("Error publishing product removed event: {}", e.getMessage(), e);
        }
    }

    /// called when a queue becomes empty
    /// publishes SSE event - useful for machines waiting for products
    @Override
    public void onQueueEmpty(Queue queue) {
        log.debug("Queue is now empty: {}", queue.getId());

        try {
            QueueEventPayload payload = new QueueEventPayload(
                    "QUEUE_EMPTY",
                    queue.getId(),
                    null,
                    null,
                    0
            );

            eventService.publishEvent(new SSE("QUEUE_EVENT", payload));

        } catch (Exception e) {
            log.error("Error publishing queue empty event: {}", e.getMessage(), e);
        }
    }

    /// Payload sent via SSE for queue events
    public static class QueueEventPayload {
        public String eventType;      // product_added, product_removed, queue_empty
        public String queueId;        // which queue?
        public String productId;      // which product? (null for empty queue)
        public String productColor;   // product color (null for empty queue)
        public int newQueueSize;      // queue size after change

        public QueueEventPayload(String eventType, String queueId, String productId,
                                 String productColor, int newQueueSize) {
            this.eventType = eventType;
            this.queueId = queueId;
            this.productId = productId;
            this.productColor = productColor;
            this.newQueueSize = newQueueSize;
        }
    }
}
