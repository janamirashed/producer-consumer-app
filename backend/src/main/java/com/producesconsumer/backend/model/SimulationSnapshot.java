package com.producesconsumer.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.producesconsumer.backend.dto.SnapshotInfo;
import lombok.Data;

import java.time.Instant;

/**
 * SimulationSnapshot for replay functionality - pure data model (Memento)
 */
@Data
public class SimulationSnapshot {
    private String id;
    private String timestamp;
    private String label;
    private SimulationState state;

    public SimulationSnapshot() {
        this.timestamp = Instant.now().toString();
    }

    @JsonIgnore
    public SnapshotInfo getInfo(){
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setId(id);
        snapshotInfo.setTimestamp(timestamp);
        snapshotInfo.setLabel(label);
        return snapshotInfo;
    }
}
