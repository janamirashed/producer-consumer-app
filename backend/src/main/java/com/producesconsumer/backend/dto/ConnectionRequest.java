package com.producesconsumer.backend.dto;

import lombok.Data;

/**
 * Request DTO for creating a connection between elements
 */
@Data
public class ConnectionRequest {
    private String sourceId;
    private String sourceType;
    private String targetId;
    private String targetType;
}
