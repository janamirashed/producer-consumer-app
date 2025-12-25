package com.producesconsumer.backend.dto;

import lombok.Data;

/**
 * Request DTO for creating a queue or machine at a position
 */
@Data
public class PositionRequest {
    private double x;
    private double y;
}
