package com.producesconsumer.backend.controller;

import com.producesconsumer.backend.dto.*;
import com.producesconsumer.backend.model.*;
import com.producesconsumer.backend.service.EventService;
import com.producesconsumer.backend.service.SimulationService;
import com.producesconsumer.backend.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * REST Controller for the Producer/Consumer simulation
 */
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final SnapshotService snapshotService;
    private final EventService eventService;

    // ==================== SSE Endpoint ====================

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SSE>> streamEvents() {
        return eventService.getEventStream()
                .map(event -> ServerSentEvent.<SSE>builder().data(event).build());
    }

    // ==================== State ====================

    @GetMapping("/state")
    public ApiResponse<SimulationState> getState() {
        return ApiResponse.success(simulationService.getState());
    }

    // ==================== Queues ====================

    @PostMapping("/queues")
    public ApiResponse<Queue> addQueue(@RequestBody PositionRequest request) {
        return ApiResponse.success(simulationService.addQueue(request.getX(), request.getY()));
    }

    @DeleteMapping("/queues/{id}")
    public ApiResponse<Void> deleteQueue(@PathVariable String id) {
        simulationService.deleteQueue(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/queues/{id}/position")
    public ApiResponse<Void> updateQueuePosition(@PathVariable String id, @RequestBody PositionRequest request) {
        simulationService.updateQueuePosition(id, request.getX(), request.getY());
        return ApiResponse.success(null);
    }

    // ==================== Machines ====================

    @PostMapping("/machines")
    public ApiResponse<Machine> addMachine(@RequestBody PositionRequest request) {
        return ApiResponse.success(simulationService.addMachine(request.getX(), request.getY()));
    }

    @DeleteMapping("/machines/{id}")
    public ApiResponse<Void> deleteMachine(@PathVariable String id) {
        simulationService.deleteMachine(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/machines/{id}/position")
    public ApiResponse<Void> updateMachinePosition(@PathVariable String id, @RequestBody PositionRequest request) {
        simulationService.updateMachinePosition(id, request.getX(), request.getY());
        return ApiResponse.success(null);
    }

    // ==================== Connections ====================

    @PostMapping("/connections")
    public ApiResponse<Connection> addConnection(@RequestBody ConnectionRequest request) {
        return ApiResponse.success(simulationService.addConnection(
                request.getSourceId(), request.getSourceType(), 
                request.getTargetId(), request.getTargetType()));
    }

    @DeleteMapping("/connections/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable String id) {
        simulationService.deleteConnection(id);
        return ApiResponse.success(null);
    }

    // ==================== Simulation Control ====================

    @PostMapping("/start")
    public ApiResponse<Void> startSimulation() {
        simulationService.startSimulation();
        return ApiResponse.success(null);
    }

    @PostMapping("/stop")
    public ApiResponse<Void> stopSimulation() {
        simulationService.stopSimulation();
        return ApiResponse.success(null);
    }

    @PostMapping("/new")
    public ApiResponse<SimulationState> newSimulation() {
        return ApiResponse.success(simulationService.newSimulation());
    }

    // ==================== Snapshots ====================

    @GetMapping("/snapshots")
    public ApiResponse<List<Snapshot>> getSnapshots() {
        return ApiResponse.success(snapshotService.getSnapshots());
    }

    @PostMapping("/snapshots")
    public ApiResponse<Snapshot> createSnapshot(@RequestBody(required = false) SnapshotRequest request) {
        String label = request != null ? request.getLabel() : null;
        return ApiResponse.success(snapshotService.createSnapshot(simulationService.getState(), label));
    }

    @PostMapping("/snapshots/{id}/replay")
    public ApiResponse<SimulationState> replaySnapshot(@PathVariable String id) {
        return snapshotService.getSnapshot(id)
                .map(snapshot -> ApiResponse.success(snapshot.getState()))
                .orElse(ApiResponse.error("Snapshot not found"));
    }
}
