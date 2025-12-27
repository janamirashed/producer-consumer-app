package com.producesconsumer.backend.dto;

import lombok.Data;

@Data
public class SnapshotInfo {
    String id;
    String label;
    String timestamp;
    //and any other data useful for the frontend snapshot picker
}
