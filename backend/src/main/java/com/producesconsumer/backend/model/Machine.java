package com.producesconsumer.backend.model;

import lombok.Data;

/**
 * Machine element in the simulation
 */
@Data
public class Machine {
    private String id;
    private double x;
    private double y;
    private String state; // "idle" or "processing"
    private int productCount;
    private int processingTime; // in milliseconds
    private String currentProductColor;
    private String inputQueueId;
    private String outputQueueId;
}
