package com.producesconsumer.backend.model;

import lombok.Data;

/**
 * Product with unique color - pure data model
 */
@Data
public class Product {
    private String id;
    private String color;
}
