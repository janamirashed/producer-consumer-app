import {
    Component,
    ElementRef,
    ViewChild,
    AfterViewInit,
    OnDestroy,
    input,
    output,
    effect,
    inject,
} from '@angular/core';
import * as fabric from 'fabric';
import {
    Queue,
    Machine,
    Connection,
    PlacementMode,
    SelectedElement,
    SimulationState,
} from '../../models/simulation.model';

interface CustomFabricObject extends fabric.FabricObject {
    data?: { type: 'queue' | 'machine'; id: string };
}

@Component({
    selector: 'app-simulation-canvas',
    standalone: true,
    imports: [],
    templateUrl: './simulation-canvas.component.html',
    styleUrl: './simulation-canvas.component.css',
})
export class SimulationCanvasComponent implements AfterViewInit, OnDestroy {
    @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

    private hostElement = inject(ElementRef);

    // Inputs
    state = input<SimulationState>({
        queues: [],
        machines: [],
        connections: [],
        isRunning: false,
    });
    placementMode = input<PlacementMode>('none');
    connectionSourceId = input<string | null>(null);

    // Outputs
    onCanvasClick = output<{ x: number; y: number }>();
    onElementSelect = output<SelectedElement | null>();
    onPositionUpdate = output<{ type: 'queue' | 'machine'; id: string; x: number; y: number }>();

    private canvas!: fabric.Canvas;
    private fabricObjects = new Map<string, CustomFabricObject>();
    private connectionLines = new Map<string, fabric.Group>();
    private flashTimeouts = new Map<string, ReturnType<typeof setTimeout>>();
    private resizeObserver!: ResizeObserver;

    private readonly QUEUE_COLOR = '#22c55e';
    private readonly MACHINE_COLOR = '#3b82f6';
    private readonly CONNECTION_COLOR = '#374151';

    constructor() {
        effect(() => {
            const currentState = this.state();
            if (this.canvas) {
                this.renderState(currentState);
            }
        });
    }

    ngAfterViewInit(): void {
        // Use setTimeout to ensure DOM is fully rendered
        setTimeout(() => this.initializeCanvas(), 0);
    }

    ngOnDestroy(): void {
        this.flashTimeouts.forEach((timeout) => clearTimeout(timeout));
        this.flashTimeouts.clear();
        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
        }
        if (this.canvas) {
            this.canvas.dispose();
        }
    }

    private initializeCanvas(): void {
        const hostEl = this.hostElement.nativeElement as HTMLElement;
        const rect = hostEl.getBoundingClientRect();

        // Set initial canvas size from host element
        this.canvasRef.nativeElement.width = rect.width || 800;
        this.canvasRef.nativeElement.height = rect.height || 600;

        this.canvas = new fabric.Canvas(this.canvasRef.nativeElement, {
            backgroundColor: '#f9fafb',
            selection: false,
            preserveObjectStacking: true,
        });

        // Set up ResizeObserver on the host element
        this.resizeObserver = new ResizeObserver((entries) => {
            for (const entry of entries) {
                const { width, height } = entry.contentRect;
                if (width > 0 && height > 0) {
                    this.canvas.setDimensions({ width, height });
                    this.canvas.renderAll();
                }
            }
        });
        this.resizeObserver.observe(hostEl);

        // Handle canvas click for placement
        this.canvas.on('mouse:down', (options) => {
            if (!options.target) {
                const pointer = this.canvas.getViewportPoint(options.e);
                this.onCanvasClick.emit({ x: pointer.x, y: pointer.y });
            }
        });

        // Handle object selection
        this.canvas.on('selection:created', ({ selected }) => {
            if (selected && selected.length > 0) {
                this.handleSelection(selected[0] as CustomFabricObject);
            }
        });

        this.canvas.on('selection:updated', ({ selected }) => {
            if (selected && selected.length > 0) {
                this.handleSelection(selected[0] as CustomFabricObject);
            }
        });

        this.canvas.on('selection:cleared', () => {
            this.onElementSelect.emit(null);
        });

        // Handle object movement
        this.canvas.on('object:modified', ({ target }) => {
            const obj = target as CustomFabricObject;
            if (obj && obj.data) {
                this.onPositionUpdate.emit({
                    type: obj.data.type,
                    id: obj.data.id,
                    x: obj.left || 0,
                    y: obj.top || 0,
                });
                this.updateConnections();
            }
        });

        this.renderState(this.state());
    }

    private handleSelection(obj: CustomFabricObject | null): void {
        if (obj && obj.data) {
            this.onElementSelect.emit({
                id: obj.data.id,
                type: obj.data.type,
            });
        }
    }

    // ==================== Rendering ====================

    private renderState(state: SimulationState): void {
        const existingQueueIds = new Set(state.queues.map((q) => q.id));
        const existingMachineIds = new Set(state.machines.map((m) => m.id));
        const existingConnectionIds = new Set(state.connections.map((c) => c.id));

        this.fabricObjects.forEach((obj, id) => {
            const isQueue = id.startsWith('queue-');
            const isMachine = id.startsWith('machine-');
            const cleanId = id.replace('queue-', '').replace('machine-', '');

            if ((isQueue && !existingQueueIds.has(cleanId)) || (isMachine && !existingMachineIds.has(cleanId))) {
                this.canvas.remove(obj);
                this.fabricObjects.delete(id);
            }
        });

        this.connectionLines.forEach((line, id) => {
            if (!existingConnectionIds.has(id)) {
                this.canvas.remove(line);
                this.connectionLines.delete(id);
            }
        });

        state.connections.forEach((conn) => this.renderConnection(conn, state));
        state.queues.forEach((queue) => this.renderQueue(queue));
        state.machines.forEach((machine) => this.renderMachine(machine));

        this.canvas.renderAll();
    }

    private renderQueue(queue: Queue): void {
        const objectId = `queue-${queue.id}`;
        let group = this.fabricObjects.get(objectId) as fabric.Group | undefined;

        if (group) {
            group.set({ left: queue.x, top: queue.y });
            this.updateQueueLabel(group, queue);
        } else {
            group = this.createQueueObject(queue);
            this.fabricObjects.set(objectId, group as unknown as CustomFabricObject);
            this.canvas.add(group);
        }
    }

    private createQueueObject(queue: Queue): fabric.Group {
        const rect = new fabric.Rect({
            width: 100,
            height: 60,
            fill: this.QUEUE_COLOR,
            rx: 8,
            ry: 8,
            originX: 'center',
            originY: 'center',
        });

        const label = new fabric.FabricText(queue.id, {
            fontSize: 14,
            fontFamily: 'system-ui, sans-serif',
            fontWeight: '600',
            fill: '#ffffff',
            originX: 'center',
            originY: 'center',
            top: -5,
        });

        const countText = new fabric.FabricText(`Products: ${queue.productCount}`, {
            fontSize: 11,
            fontFamily: 'system-ui, sans-serif',
            fill: 'rgba(255, 255, 255, 0.9)',
            originX: 'center',
            originY: 'center',
            top: 12,
        });

        const group = new fabric.Group([rect, label, countText], {
            left: queue.x,
            top: queue.y,
            selectable: true,
            hasControls: false,
            hasBorders: true,
            borderColor: '#2563eb',
        });

        (group as CustomFabricObject).data = { type: 'queue', id: queue.id };
        return group;
    }

    private updateQueueLabel(group: fabric.Group, queue: Queue): void {
        const objects = group.getObjects();
        const countText = objects[2] as fabric.FabricText;
        if (countText) {
            countText.set('text', `Products: ${queue.productCount}`);
        }
    }

    private renderMachine(machine: Machine): void {
        const objectId = `machine-${machine.id}`;
        let group = this.fabricObjects.get(objectId) as fabric.Group | undefined;

        if (group) {
            group.set({ left: machine.x, top: machine.y });
            this.updateMachineAppearance(group, machine);
        } else {
            group = this.createMachineObject(machine);
            this.fabricObjects.set(objectId, group as unknown as CustomFabricObject);
            this.canvas.add(group);
        }
    }

    private createMachineObject(machine: Machine): fabric.Group {
        const bgColor = machine.currentProductColor || this.MACHINE_COLOR;

        const rect = new fabric.Rect({
            width: 100,
            height: 60,
            fill: bgColor,
            rx: 8,
            ry: 8,
            originX: 'center',
            originY: 'center',
        });

        const label = new fabric.FabricText('Machine', {
            fontSize: 14,
            fontFamily: 'system-ui, sans-serif',
            fontWeight: '600',
            fill: '#ffffff',
            originX: 'center',
            originY: 'center',
            top: -5,
        });

        // Show processing time in seconds
        const timeText = new fabric.FabricText(`${(machine.processingTime / 1000).toFixed(1)}s`, {
            fontSize: 11,
            fontFamily: 'system-ui, sans-serif',
            fill: 'rgba(255, 255, 255, 0.9)',
            originX: 'center',
            originY: 'center',
            top: 12,
        });

        const group = new fabric.Group([rect, label, timeText], {
            left: machine.x,
            top: machine.y,
            selectable: true,
            hasControls: false,
            hasBorders: true,
            borderColor: '#2563eb',
        });

        (group as CustomFabricObject).data = { type: 'machine', id: machine.id };
        return group;
    }

    private updateMachineAppearance(group: fabric.Group, machine: Machine): void {
        const objects = group.getObjects();
        const rect = objects[0] as fabric.Rect;
        const bgColor = machine.currentProductColor || this.MACHINE_COLOR;
        rect.set({ fill: bgColor });
    }

    private renderConnection(connection: Connection, state: SimulationState): void {
        const sourcePos = this.findElementPosition(connection.sourceId, connection.sourceType, state);
        const targetPos = this.findElementPosition(connection.targetId, connection.targetType, state);

        if (!sourcePos || !targetPos) return;

        let arrowGroup = this.connectionLines.get(connection.id);
        if (arrowGroup) {
            this.canvas.remove(arrowGroup);
        }

        // Calculate start and end points at element edges
        const x1 = sourcePos.x + 50; // Right edge of source
        const y1 = sourcePos.y;
        const x2 = targetPos.x - 50; // Left edge of target
        const y2 = targetPos.y;

        // Calculate control points for smooth bezier curve
        const dx = x2 - x1;
        const dy = y2 - y1;
        const curvature = Math.min(Math.abs(dx) * 0.5, 80); // Max curve of 80px

        // Control points for cubic bezier
        const cp1x = x1 + curvature;
        const cp1y = y1;
        const cp2x = x2 - curvature;
        const cp2y = y2;

        // Create bezier path
        const pathData = `M ${x1} ${y1} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${x2} ${y2}`;

        const path = new fabric.Path(pathData, {
            fill: '',
            stroke: this.CONNECTION_COLOR,
            strokeWidth: 2,
            selectable: false,
            evented: false,
        });

        // Arrow head at the end
        const angle = Math.atan2(y2 - cp2y, x2 - cp2x);
        const arrowHead = new fabric.Triangle({
            width: 12,
            height: 12,
            fill: this.CONNECTION_COLOR,
            left: x2,
            top: y2,
            angle: (angle * 180) / Math.PI + 90,
            originX: 'center',
            originY: 'center',
            selectable: false,
            evented: false,
        });

        arrowGroup = new fabric.Group([path, arrowHead], {
            selectable: false,
            evented: false,
        });

        this.connectionLines.set(connection.id, arrowGroup);
        this.canvas.add(arrowGroup);
        this.canvas.sendObjectToBack(arrowGroup);
    }

    private findElementPosition(
        id: string,
        type: 'queue' | 'machine',
        state: SimulationState
    ): { x: number; y: number } | null {
        if (type === 'queue') {
            const queue = state.queues.find((q) => q.id === id);
            return queue ? { x: queue.x, y: queue.y } : null;
        } else {
            const machine = state.machines.find((m) => m.id === id);
            return machine ? { x: machine.x, y: machine.y } : null;
        }
    }

    private updateConnections(): void {
        this.renderState(this.state());
    }

    triggerFlash(machineId: string): void {
        const objectId = `machine-${machineId}`;
        const group = this.fabricObjects.get(objectId) as fabric.Group | undefined;

        if (!group) return;

        const objects = group.getObjects();
        const rect = objects[0] as fabric.Rect;
        const originalFill = rect.fill;

        rect.set({ fill: '#22c55e' });
        this.canvas.renderAll();

        const timeout = setTimeout(() => {
            rect.set({ fill: originalFill });
            this.canvas.renderAll();
            this.flashTimeouts.delete(machineId);
        }, 500);

        this.flashTimeouts.set(machineId, timeout);
    }

    forceRender(): void {
        this.renderState(this.state());
    }
}
