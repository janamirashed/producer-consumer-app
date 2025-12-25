package com.producesconsumer.backend.service;

import com.producesconsumer.backend.model.SimulationState;
import com.producesconsumer.backend.model.Snapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Snapshot service - manages snapshots (Memento Caretaker)
 */
@Service
@Slf4j
public class SnapshotService {

    private final List<Snapshot> snapshots = new CopyOnWriteArrayList<>();
    private int snapshotCounter = 0;

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public Snapshot createSnapshot(SimulationState state, String label) {
        Snapshot snapshot = new Snapshot();
        snapshot.setId("SNAP" + (++snapshotCounter));
        snapshot.setLabel(label != null ? label : "Snapshot " + snapshotCounter);
        snapshot.setState(deepCopyState(state));
        snapshots.add(snapshot);
        log.info("Created snapshot: {}", snapshot.getId());
        return snapshot;
    }

    public Optional<Snapshot> getSnapshot(String id) {
        return snapshots.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    public void clearSnapshots() {
        snapshots.clear();
        snapshotCounter = 0;
        log.info("Cleared all snapshots");
    }

    private SimulationState deepCopyState(SimulationState state) {
        // TODO: Implement proper deep copy
        return state;
    }
}
