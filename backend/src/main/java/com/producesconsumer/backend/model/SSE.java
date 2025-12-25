package com.producesconsumer.backend.model;

import lombok.Data;

import java.time.Instant;

/**
 * Server-Sent Event model - uses simple String for event type
 */
@Data
public class SSE {
    private String type;
    private Instant timestamp;
    private Object data;

    public SSE() {
        this.timestamp = Instant.now();
    }

    public SSE(String type, Object data) {
        this();
        this.type = type;
        this.data = data;
    }
}
