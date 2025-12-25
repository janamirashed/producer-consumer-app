package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.SSE;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Event service for Server-Sent Events
 */
@Service
@Slf4j
public class EventService {
    
    private final Sinks.Many<SSE> eventSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<SSE> getEventStream() {
        return eventSink.asFlux();
    }

    public void publishEvent(SSE event) {
        Sinks.EmitResult result = eventSink.tryEmitNext(event);
        log.debug("SSE Event: {} - Result: {}", event.getType(), result);
        if (result.isFailure()) {
            log.error("Failed to publish SSE event: {}", event.getType());
        }
    }
}
