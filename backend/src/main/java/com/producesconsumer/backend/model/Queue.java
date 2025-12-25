package com.producesconsumer.backend.model;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Queue element in the simulation - pure data model
 */
@Data
public class Queue {
    private String id;
    private double x;
    private double y;
    private int productCount;
    private List<Product> products;

    public Queue() {
        this.products = new CopyOnWriteArrayList<>();
        this.productCount = 0;
    }
}
