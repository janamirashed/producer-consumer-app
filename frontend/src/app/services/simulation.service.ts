import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, catchError, of, delay, map, tap } from 'rxjs';
import {
    SimulationState,
    Queue,
    Machine,
    Connection,
    Snapshot,
    SSEEvent,
    ApiResponse,
} from '../models/simulation.model';

@Injectable({
    providedIn: 'root',
})
export class SimulationService {
    private readonly API_BASE = 'http://localhost:8080/api/simulation';

    // ========== MOCK MODE ==========
    // Set to false when backend is ready
    private readonly MOCK_MODE = false;
    private mockSimulationInterval: ReturnType<typeof setInterval> | null = null;
    private queueCounter = 0;
    private machineCounter = 0;
    private connectionCounter = 0;

    // Reactive state using signals
    private _state = signal<SimulationState>({
        queues: [],
        machines: [],
        connections: [],
        isRunning: false,
    });

    private _snapshots = signal<Snapshot[]>([]);
    private _isConnected = signal<boolean>(false);
    private _error = signal<string | null>(null);

    // Public readonly signals
    readonly state = this._state.asReadonly();
    readonly snapshots = this._snapshots.asReadonly();
    readonly isConnected = this._isConnected.asReadonly();
    readonly error = this._error.asReadonly();

    // Computed values
    readonly queues = computed(() => this._state().queues);
    readonly machines = computed(() => this._state().machines);
    readonly connections = computed(() => this._state().connections);
    readonly isRunning = computed(() => this._state().isRunning);

    // SSE event subject for components to subscribe
    private eventSource: EventSource | null = null;
    private _sseEvents = new Subject<SSEEvent>();
    readonly sseEvents$ = this._sseEvents.asObservable();

    // Machine flash events
    private _machineFlash = new Subject<string>();
    readonly machineFlash$ = this._machineFlash.asObservable();

    constructor(private http: HttpClient) { }

    // ==================== SSE Connection ====================

    /** Connect to Server-Sent Events stream */
    connectSSE(): void {
        if (this.MOCK_MODE) {
            console.log('[MOCK] SSE Connected (simulated)');
            this._isConnected.set(true);
            this._error.set(null);
            return;
        }

        if (this.eventSource) {
            this.disconnectSSE();
        }

        this.eventSource = new EventSource(`${this.API_BASE}/events`);

        this.eventSource.onopen = () => {
            this._isConnected.set(true);
            this._error.set(null);
            console.log('[SSE] Connected');
        };

        this.eventSource.onerror = (error) => {
            console.error('[SSE] Error:', error);
            this._isConnected.set(false);
            this._error.set('Connection lost. Attempting to reconnect...');
        };

        this.eventSource.onmessage = (event) => {
            try {
                const sseEvent: SSEEvent = JSON.parse(event.data);
                console.log('[SSE] Received event:', sseEvent.type, sseEvent.data);
                this.handleSSEEvent(sseEvent);
                this._sseEvents.next(sseEvent);
            } catch (e) {
                console.error('[SSE] Failed to parse event:', e);
            }
        };

        this.eventSource.addEventListener('STATE_UPDATE', (event: MessageEvent) => {
            const data = JSON.parse(event.data);
            this._state.set(data);
        });

        this.eventSource.addEventListener('MACHINE_FLASH', (event: MessageEvent) => {
            const data = JSON.parse(event.data);
            this._machineFlash.next(data.machineId);
        });
    }

    /** Disconnect from SSE stream */
    disconnectSSE(): void {
        if (this.MOCK_MODE) {
            this._isConnected.set(false);
            this.stopMockSimulation();
            return;
        }

        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            this._isConnected.set(false);
            console.log('[SSE] Disconnected');
        }
    }

    /** Handle incoming SSE events and update state */
    private handleSSEEvent(event: SSEEvent): void {
        switch (event.type) {
            case 'STATE_UPDATE':
                this._state.set(event.data as SimulationState);
                break;

            case 'QUEUE_UPDATE':
                this.updateQueue(event.data as Queue);
                break;

            case 'QUEUE_EVENT':
                // Handle queue events from QueueEventObserver (PRODUCT_ADDED, PRODUCT_REMOVED, QUEUE_EMPTY)
                const queueEvent = event.data as { eventType: string; queueId: string; productId: string | null; productColor: string | null; newQueueSize: number };
                console.log('[SSE] QUEUE_EVENT:', queueEvent.eventType, 'queue:', queueEvent.queueId, 'newSize:', queueEvent.newQueueSize);
                console.log('[SSE] Current queues:', this._state().queues.map(q => ({ id: q.id, count: q.productCount })));
                this._state.update((s) => ({
                    ...s,
                    queues: s.queues.map((q) =>
                        q.id === queueEvent.queueId
                            ? { ...q, productCount: queueEvent.newQueueSize }
                            : q
                    ),
                }));
                break;

            case 'MACHINE_UPDATE':
                this.updateMachine(event.data as Machine);
                break;

            case 'MACHINE_FLASH':
                const flashData = event.data as { machineId: string };
                this._machineFlash.next(flashData.machineId);
                break;

            case 'SIMULATION_STARTED':
                this._state.update((s) => ({ ...s, isRunning: true }));
                break;

            case 'SIMULATION_STOPPED':
                this._state.update((s) => ({ ...s, isRunning: false }));
                break;

            case 'SNAPSHOT_CREATED':
                this.loadSnapshots();
                break;
        }
    }

    private updateQueue(queue: Queue): void {
        this._state.update((s) => ({
            ...s,
            queues: s.queues.map((q) => (q.id === queue.id ? queue : q)),
        }));
    }

    private updateMachine(machine: Machine): void {
        this._state.update((s) => ({
            ...s,
            machines: s.machines.map((m) => (m.id === machine.id ? machine : m)),
        }));
    }

    // ==================== REST API Methods ====================

    /** Load current simulation state */
    loadState(): Observable<SimulationState> {
        if (this.MOCK_MODE) {
            return of(this._state());
        }
        return this.http.get<ApiResponse<SimulationState>>(`${this.API_BASE}/state`).pipe(
            catchError((error) => {
                console.error('Failed to load state:', error);
                this._error.set('Failed to load simulation state');
                return of({ success: false, data: this._state() });
            })
        ).pipe(
            map((response) => {
                if (response.success && response.data) {
                    this._state.set(response.data);
                    return response.data;
                }
                return this._state();
            })
        );
    }

    /** Add a new queue at position */
    addQueue(x: number, y: number): Observable<ApiResponse<Queue>> {
        const newQueue: Queue = {
            id: `Q${this.queueCounter++}`,
            x,
            y,
            productCount: 0,
            products: [],
        };

        if (this.MOCK_MODE) {
            this.addQueueLocally(newQueue);
            return of({ success: true, data: newQueue });
        }
        return this.http.post<ApiResponse<Queue>>(`${this.API_BASE}/queues`, { x, y });
    }

    /** Add a new machine at position */
    addMachine(x: number, y: number): Observable<ApiResponse<Machine>> {
        const newMachine: Machine = {
            id: `M${++this.machineCounter}`,
            x,
            y,
            state: 'idle',
            productCount: 0,
            processingTime: Math.floor(Math.random() * 3000) + 1000, // Random 1-4 seconds
        };

        if (this.MOCK_MODE) {
            this.addMachineLocally(newMachine);
            return of({ success: true, data: newMachine });
        }
        return this.http.post<ApiResponse<Machine>>(`${this.API_BASE}/machines`, { x, y });
    }

    /** Create connection between elements */
    connect(
        sourceId: string,
        sourceType: 'queue' | 'machine',
        targetId: string,
        targetType: 'queue' | 'machine'
    ): Observable<ApiResponse<Connection>> {
        const newConnection: Connection = {
            id: `C${++this.connectionCounter}`,
            sourceId,
            sourceType,
            targetId,
            targetType,
        };

        if (this.MOCK_MODE) {
            this.addConnectionLocally(newConnection);
            return of({ success: true, data: newConnection });
        }
        return this.http.post<ApiResponse<Connection>>(`${this.API_BASE}/connections`, {
            sourceId,
            sourceType,
            targetId,
            targetType,
        });
    }

    /** Remove an element */
    removeElement(type: 'queue' | 'machine', id: string): Observable<ApiResponse<void>> {
        if (this.MOCK_MODE) {
            if (type === 'queue') {
                this.removeQueueLocally(id);
            } else {
                this.removeMachineLocally(id);
            }
            return of({ success: true });
        }
        return this.http.delete<ApiResponse<void>>(`${this.API_BASE}/${type}s/${id}`);
    }

    /** Remove a connection */
    removeConnection(id: string): Observable<ApiResponse<void>> {
        if (this.MOCK_MODE) {
            this._state.update((s) => ({
                ...s,
                connections: s.connections.filter((c) => c.id !== id),
            }));
            return of({ success: true });
        }
        return this.http.delete<ApiResponse<void>>(`${this.API_BASE}/connections/${id}`);
    }

    /** Update element position */
    updatePosition(
        type: 'queue' | 'machine',
        id: string,
        x: number,
        y: number
    ): Observable<ApiResponse<void>> {
        if (this.MOCK_MODE) {
            if (type === 'queue') {
                this._state.update((s) => ({
                    ...s,
                    queues: s.queues.map((q) => (q.id === id ? { ...q, x, y } : q)),
                }));
            } else {
                this._state.update((s) => ({
                    ...s,
                    machines: s.machines.map((m) => (m.id === id ? { ...m, x, y } : m)),
                }));
            }
            return of({ success: true });
        }
        return this.http.patch<ApiResponse<void>>(`${this.API_BASE}/${type}s/${id}/position`, {
            x,
            y,
        });
    }

    /** Start the simulation */
    startSimulation(): Observable<ApiResponse<void>> {
        if (this.MOCK_MODE) {
            this._state.update((s) => ({ ...s, isRunning: true }));
            this.startMockSimulation();
            return of({ success: true });
        }
        return this.http.post<ApiResponse<void>>(`${this.API_BASE}/start`, {});
    }

    /** Stop the simulation */
    stopSimulation(): Observable<ApiResponse<void>> {
        if (this.MOCK_MODE) {
            this._state.update((s) => ({ ...s, isRunning: false }));
            this.stopMockSimulation();
            return of({ success: true });
        }
        return this.http.post<ApiResponse<void>>(`${this.API_BASE}/stop`, {});
    }

    /** Reset and start new simulation */
    newSimulation(): Observable<ApiResponse<SimulationState>> {
        const newState: SimulationState = {
            queues: [],
            machines: [],
            connections: [],
            isRunning: false,
        };

        if (this.MOCK_MODE) {
            this.stopMockSimulation();
            this._state.set(newState);
            this._snapshots.set([]);
            this.queueCounter = 0;
            this.machineCounter = 0;
            this.connectionCounter = 0;
            return of({ success: true, data: newState });
        }
        return this.http.post<ApiResponse<SimulationState>>(`${this.API_BASE}/new`, {});
    }

    // ==================== Snapshot Methods ====================

    /** Load available snapshots */
    loadSnapshots(): void {
        if (this.MOCK_MODE) {
            // Already have local snapshots
            return;
        }
        this.http.get<Snapshot[]>(`${this.API_BASE}/snapshots`).subscribe({
            next: (snapshots) => this._snapshots.set(snapshots),
            error: (err) => console.error('Failed to load snapshots:', err),
        });
    }

    /** Replay from a specific snapshot */
    replaySnapshot(snapshotId: string): Observable<ApiResponse<SimulationState>> {
        if (this.MOCK_MODE) {
            const snapshot = this._snapshots().find((s) => s.id === snapshotId);
            if (snapshot) {
                this.stopMockSimulation();
                this._state.set({ ...snapshot.state });
            }
            return of({ success: true, data: this._state() });
        }
        return this.http.post<ApiResponse<SimulationState>>(
            `${this.API_BASE}/snapshots/${snapshotId}/replay`,
            {}
        );
    }

    /** Create a manual snapshot */
    createSnapshot(label?: string): Observable<ApiResponse<Snapshot>> {
        if (this.MOCK_MODE) {
            const snapshot: Snapshot = {
                id: `SNAP${this._snapshots().length + 1}`,
                timestamp: new Date(),
                label: label || `Snapshot ${this._snapshots().length + 1}`,
                state: JSON.parse(JSON.stringify(this._state())), // Deep clone
            };
            this._snapshots.update((s) => [...s, snapshot]);
            return of({ success: true, data: snapshot });
        }
        return this.http.post<ApiResponse<Snapshot>>(`${this.API_BASE}/snapshots`, { label });
    }

    // ==================== Mock Simulation ====================

    private startMockSimulation(): void {
        if (this.mockSimulationInterval) return;

        console.log('[MOCK] Simulation started');

        // Simulate product flow every 1-2 seconds
        this.mockSimulationInterval = setInterval(() => {
            const state = this._state();
            if (!state.isRunning) return;

            // Add products to queues randomly
            if (state.queues.length > 0 && Math.random() > 0.5) {
                const randomQueue = state.queues[Math.floor(Math.random() * state.queues.length)];
                const newProductCount = randomQueue.productCount + 1;
                this._state.update((s) => ({
                    ...s,
                    queues: s.queues.map((q) =>
                        q.id === randomQueue.id ? { ...q, productCount: newProductCount } : q
                    ),
                }));
            }

            // Process products in machines
            state.machines.forEach((machine) => {
                if (machine.state === 'idle') {
                    // Find connected input queue with products
                    const inputConnection = state.connections.find(
                        (c) => c.targetId === machine.id && c.sourceType === 'queue'
                    );
                    if (inputConnection) {
                        const inputQueue = state.queues.find((q) => q.id === inputConnection.sourceId);
                        if (inputQueue && inputQueue.productCount > 0) {
                            // Start processing
                            const productColor = this.generateRandomColor();
                            this._state.update((s) => ({
                                ...s,
                                queues: s.queues.map((q) =>
                                    q.id === inputQueue.id ? { ...q, productCount: q.productCount - 1 } : q
                                ),
                                machines: s.machines.map((m) =>
                                    m.id === machine.id
                                        ? {
                                            ...m,
                                            state: 'processing' as const,
                                            currentProductColor: productColor,
                                            productCount: m.productCount + 1,
                                        }
                                        : m
                                ),
                            }));

                            // Complete processing after random time
                            setTimeout(() => {
                                this._machineFlash.next(machine.id);
                                this._state.update((s) => ({
                                    ...s,
                                    machines: s.machines.map((m) =>
                                        m.id === machine.id
                                            ? { ...m, state: 'idle' as const, currentProductColor: undefined }
                                            : m
                                    ),
                                }));

                                // Add to output queue if connected
                                const outputConnection = state.connections.find(
                                    (c) => c.sourceId === machine.id && c.targetType === 'queue'
                                );
                                if (outputConnection) {
                                    this._state.update((s) => ({
                                        ...s,
                                        queues: s.queues.map((q) =>
                                            q.id === outputConnection.targetId ? { ...q, productCount: q.productCount + 1 } : q
                                        ),
                                    }));
                                }
                            }, machine.processingTime);
                        }
                    }
                }
            });
        }, 1500);
    }

    private stopMockSimulation(): void {
        if (this.mockSimulationInterval) {
            clearInterval(this.mockSimulationInterval);
            this.mockSimulationInterval = null;
            console.log('[MOCK] Simulation stopped');
        }
    }

    private generateRandomColor(): string {
        const colors = [
            '#ef4444', // red
            '#f97316', // orange
            '#eab308', // yellow
            '#22c55e', // green
            '#06b6d4', // cyan
            '#3b82f6', // blue
            '#8b5cf6', // violet
            '#ec4899', // pink
        ];
        return colors[Math.floor(Math.random() * colors.length)];
    }

    // ==================== Local State Updates ====================

    /** Locally add a queue (optimistic update) */
    addQueueLocally(queue: Queue): void {
        this._state.update((s) => ({
            ...s,
            queues: [...s.queues, queue],
        }));
    }

    /** Locally add a machine (optimistic update) */
    addMachineLocally(machine: Machine): void {
        this._state.update((s) => ({
            ...s,
            machines: [...s.machines, machine],
        }));
    }

    /** Locally add a connection (optimistic update) */
    addConnectionLocally(connection: Connection): void {
        this._state.update((s) => ({
            ...s,
            connections: [...s.connections, connection],
        }));
    }

    /** Locally remove a queue */
    removeQueueLocally(id: string): void {
        this._state.update((s) => ({
            ...s,
            queues: s.queues.filter((q) => q.id !== id),
            connections: s.connections.filter((c) => c.sourceId !== id && c.targetId !== id),
        }));
    }

    /** Locally remove a machine */
    removeMachineLocally(id: string): void {
        this._state.update((s) => ({
            ...s,
            machines: s.machines.filter((m) => m.id !== id),
            connections: s.connections.filter((c) => c.sourceId !== id && c.targetId !== id),
        }));
    }
}
