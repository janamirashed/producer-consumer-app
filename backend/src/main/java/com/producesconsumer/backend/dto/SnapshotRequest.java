package com.producesconsumer.backend.dto;

import lombok.Data;

/**
 * Request DTO for creating a snapshot
 */
@Data
public class SnapshotRequest {
    private String label;
}
