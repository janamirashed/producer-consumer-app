package com.producesconsumer.backend.model;

import lombok.Data;

/**
 * Connection between elements - uses simple String for types
 */
@Data
public class Connection {
    private String id;
    private String sourceId;
    private String sourceType; // "queue" or "machine"
    private String targetId;
    private String targetType; // "queue" or "machine"
}
