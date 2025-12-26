/**
 * Simulation Models
 * TypeScript interfaces for the Producer/Consumer simulation
 */

/** Product with unique color */
export interface Product {
    id: string;
    color: string;
}

/** Queue element in the simulation */
export interface Queue {
    id: string;
    x: number;
    y: number;
    productCount: number;
    products: Product[];
}

/** Machine processing state */
export type MachineState = 'idle' | 'processing';

/** Machine element in the simulation */
export interface Machine {
    id: string;
    x: number;
    y: number;
    state: MachineState;
    productCount: number;
    processingTime: number;
    currentProductColor?: string;
    inputQueueId?: string;
    outputQueueId?: string;
}

/** Connection between elements */
export interface Connection {
    id: string;
    sourceId: string;
    sourceType: 'queue' | 'machine';
    targetId: string;
    targetType: 'queue' | 'machine';
}

/** Complete simulation state */
export interface SimulationState {
    queues: Queue[];
    machines: Machine[];
    connections: Connection[];
    isRunning: boolean;
    simulationId?: string;
}

/** Snapshot for replay functionality */
export interface Snapshot {
    id: string;
    timestamp: Date;
    label?: string;
    state: SimulationState;
}

/** SSE Event types from backend */
export type SSEEventType =
    | 'STATE_UPDATE'
    | 'QUEUE_UPDATE'
    | 'QUEUE_EVENT'
    | 'MACHINE_UPDATE'
    | 'MACHINE_FLASH'
    | 'PRODUCT_ADDED'
    | 'PRODUCT_PROCESSED'
    | 'SIMULATION_STARTED'
    | 'SIMULATION_STOPPED'
    | 'SNAPSHOT_CREATED';

/** SSE Event message */
export interface SSEEvent<T = unknown> {
    type: SSEEventType;
    timestamp: Date;
    data: T;
}

/** Element placement mode for UI */
export type PlacementMode = 'none' | 'queue' | 'machine' | 'connect';

/** Selected element for connections */
export interface SelectedElement {
    id: string;
    type: 'queue' | 'machine';
}

/** API response wrapper */
export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
}
