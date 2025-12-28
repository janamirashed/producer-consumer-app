package com.producesconsumer.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.producesconsumer.backend.dto.SnapshotInfo;
import com.producesconsumer.backend.model.SimulationSnapshot;
import com.producesconsumer.backend.model.SimulationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;


/**
 * SimulationSnapshot service - manages snapshots (Memento Caretaker)
 */
@Service
@Slf4j
public class SnapshotService {

    private final Map<String, SimulationSnapshot> snapshots = new ConcurrentHashMap<>();
    // caching a certain number of snapshots (recents) to reduce the Disk I/O and mapping overhead

    private int snapshotCounter = 0;

    @Autowired
    private SimulationService simulationService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final String snapshotsDir = "snapshots";

    public SnapshotService(SimulationService simulationService) {}

    public List<SimulationSnapshot> getSnapshots() {
        List<SimulationSnapshot> snapshotList = new ArrayList<>();
        for (Map.Entry<String, SimulationSnapshot> entry : snapshots.entrySet()) {
            snapshotList.add(entry.getValue());
        }
        return snapshotList;
    }

    public List<SnapshotInfo> getSnapshotsInfo() {
        List<SnapshotInfo> snapshotList = new ArrayList<>();
        for (Map.Entry<String, SimulationSnapshot> entry : snapshots.entrySet()) {
            SnapshotInfo snapshotInfo = entry.getValue().getInfo();
            snapshotList.add(snapshotInfo);
        }
        Path path = Paths.get(snapshotsDir);
        try (Stream<Path> stream = Files.list(path)) {
            stream.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fullFileName = filePath.getFileName().toString();
                        int lastDotIndex = fullFileName.lastIndexOf('.');
                        String baseName = (lastDotIndex == -1) ? fullFileName : fullFileName.substring(0, lastDotIndex);

                        if (!snapshots.containsKey(baseName)) {
                            SnapshotInfo snapshotInfo = new SnapshotInfo();
                            snapshotInfo.setTimestamp("Older");
                            snapshotInfo.setId(baseName);
                            snapshotInfo.setLabel(baseName);
                            snapshotList.add(snapshotInfo);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return snapshotList;
    }


//    public SimulationSnapshot createSnapshot(SimulationState state, String label) {
//        SimulationSnapshot snapshot = new SimulationSnapshot();
//        snapshot.setId("SNAP" + (++snapshotCounter));
//        snapshot.setLabel(label != null ? label : "SimulationSnapshot " + snapshotCounter);
//        snapshot.setState(deepCopyState(state));
//        snapshots.add(snapshot);
//        log.info("Created snapshot: {}", snapshot.getId());
//        return snapshot;
//    }

    public SimulationSnapshot saveSnapshot(String label)  {
        SimulationSnapshot snapshot = simulationService.getState().saveToSnapshot(label);
        snapshots.put(snapshot.getLabel(), snapshot); // for faster access
        snapshotCounter++;
        try {
            String snapshotString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
            Files.createDirectories(Path.of(snapshotsDir));
            Files.writeString(Path.of(snapshotsDir + "/" + snapshot.getLabel() + ".json"), snapshotString);

        } catch (JsonProcessingException e){
            System.err.println("An error occurred while trying to write Snapshot as a JsonString" + e.getMessage());
        } catch (IOException e){
            System.err.println("An error occurred while trying to write Snapshot in a File" + e.getMessage());
        }
        return snapshot;
    }

    public SimulationSnapshot loadSnapshot(String label) {
        if (snapshots.containsKey(label)) {
            SimulationSnapshot snapshot = snapshots.get(label);
            simulationService.getState().loadFromSnapshot(snapshot);
            return snapshot;
        }
        try {
            String snapshotStr =  Files.readString(Path.of(snapshotsDir, label + ".json"));
            SimulationSnapshot snapshot = objectMapper.readValue(snapshotStr, SimulationSnapshot.class);
            snapshots.put(snapshot.getLabel(), snapshot); // for faster access
            snapshotCounter++;
            simulationService.getState().loadFromSnapshot(snapshot);
            return snapshot;
        } catch (IOException e) {
            System.err.println("An error occurred while trying to read Snapshot in a File" +  e.getMessage());
            return null;
        }
    }

//    public Optional<SimulationSnapshot> getSnapshot(String id) {
//        return snapshots.stream()
//                .filter(s -> s.getId().equals(id))
//                .findFirst();
//    }

    public void clearSnapshots() {
        snapshots.clear();
        snapshotCounter = 0;
        log.info("Cleared all snapshots");
    }

}
