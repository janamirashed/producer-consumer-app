import { Component, OnInit, OnDestroy, signal, ViewChild, computed } from '@angular/core';
import { Subscription } from 'rxjs';

import { SimulationCanvasComponent } from './components/simulation-canvas/simulation-canvas.component';
import { SnapshotListComponent } from './components/snapshot-list/snapshot-list.component';

import { SimulationService } from './services/simulation.service';
import {
  PlacementMode,
  SelectedElement,
} from './models/simulation.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    SimulationCanvasComponent,
    SnapshotListComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit, OnDestroy {
  @ViewChild(SimulationCanvasComponent) canvasComponent!: SimulationCanvasComponent;

  // UI State
  placementMode = signal<PlacementMode>('none');
  selectedElement = signal<SelectedElement | null>(null);
  connectionSource = signal<SelectedElement | null>(null);
  showSnapshotList = signal<boolean>(false);

  // Service data exposed as signals for template binding
  get state() {
    return this.simulationService.state;
  }
  get isRunning() {
    return this.simulationService.isRunning;
  }
  get isConnected() {
    return this.simulationService.isConnected;
  }
  get isReplaying() {
    return this.simulationService.isReplaying;
  }
  get error() {
    return this.simulationService.error;
  }
  get snapshots() {
    return this.simulationService.snapshots;
  }

  // Computed total products
  totalProducts = computed(() => {
    const state = this.simulationService.state();
    const queueProducts = state.queues.reduce((sum, q) => sum + q.productCount, 0);
    const machineProducts = state.machines.reduce((sum, m) => sum + m.productCount, 0);
    return queueProducts + machineProducts;
  });

  private subscriptions: Subscription[] = [];

  constructor(private simulationService: SimulationService) { }

  ngOnInit(): void {
    // Connect to SSE
    this.simulationService.connectSSE();

    // Load snapshots
    this.simulationService.loadSnapshots();

    // Subscribe to machine flash events
    const flashSub = this.simulationService.machineFlash$.subscribe((machineId) => {
      if (this.canvasComponent) {
        this.canvasComponent.triggerFlash(machineId);
      }
    });
    this.subscriptions.push(flashSub);
  }

  ngOnDestroy(): void {
    this.simulationService.disconnectSSE();
    this.subscriptions.forEach((sub) => sub.unsubscribe());
  }

  // ==================== Toolbar Handlers ====================

  onStart(): void {
    this.simulationService.startSimulation().subscribe();
  }

  onStop(): void {
    this.simulationService.stopSimulation().subscribe();
  }

  onRestart(): void {
    this.simulationService.restartSimulation().subscribe();
  }

  onNewSimulation(): void {
    this.simulationService.newSimulation().subscribe({
      next: () => {
        this.placementMode.set('none');
        this.selectedElement.set(null);
        this.connectionSource.set(null);
      },
    });
  }

  onReplay(snapshotLabel: string): void {
    this.simulationService.replaySnapshot(snapshotLabel).subscribe();
  }

  // ==================== Control Panel Handlers ====================

  onModeChange(mode: PlacementMode): void {
    // Toggle off if clicking same mode
    if (this.placementMode() === mode) {
      this.placementMode.set('none');
    } else {
      this.placementMode.set(mode);
    }
    this.selectedElement.set(null);
    this.connectionSource.set(null);
  }

  onDeleteElement(element: SelectedElement): void {
    this.simulationService.removeElement(element.type, element.id).subscribe({
      next: () => {
        this.selectedElement.set(null);
      },
    });
  }

  onDeleteSelected(): void {
    const element = this.selectedElement();
    if (element) {
      this.onDeleteElement(element);
    }
  }

  getSelectedElementLabel(): string {
    const element = this.selectedElement();
    if (!element) return '';
    return element.type === 'queue' ? 'Queue' : 'Machine';
  }

  // ==================== Canvas Handlers ====================

  onCanvasClick(position: { x: number; y: number }): void {
    const mode = this.placementMode();

    if (mode === 'queue') {
      this.addQueue(position.x, position.y);
    } else if (mode === 'machine') {
      this.addMachine(position.x, position.y);
    }
  }

  onElementSelect(element: SelectedElement | null): void {
    if (this.placementMode() === 'connect') {
      this.handleConnectionSelection(element);
    } else {
      this.selectedElement.set(element);
      // Selecting an element should deselect any placement mode
      if (element) {
        this.placementMode.set('none');
      }
    }
  }

  onPositionUpdate(update: { type: 'queue' | 'machine'; id: string; x: number; y: number }): void {
    this.simulationService.updatePosition(update.type, update.id, update.x, update.y).subscribe();
  }

  // ==================== Snapshot Handlers ====================

  toggleSnapshotList(): void {
    const nextValue = !this.showSnapshotList();
    if (nextValue) {
      this.simulationService.loadSnapshots();
    }
    this.showSnapshotList.set(nextValue);
  }

  onSnapshotSelect(snapshotLabel: string): void {
    this.simulationService.replaySnapshot(snapshotLabel).subscribe();
    this.showSnapshotList.set(false);
  }

  onReturnToLive(): void {
    this.simulationService.restoreLiveState();
  }

  onSnapshotCreate(): void {
    this.simulationService.createSnapshot().subscribe({
      next: () => {
        this.simulationService.loadSnapshots();
      },
    });
  }

  // ==================== Private Methods ====================

  private addQueue(x: number, y: number): void {
    this.simulationService.addQueue(x, y).subscribe();
  }

  private addMachine(x: number, y: number): void {
    this.simulationService.addMachine(x, y).subscribe();
  }

  private handleConnectionSelection(element: SelectedElement | null): void {
    if (!element) return;

    const source = this.connectionSource();

    if (!source) {
      // Set source
      this.connectionSource.set(element);
    } else {
      // Create connection
      if (source.id !== element.id) {
        this.simulationService
          .connect(source.id, source.type, element.id, element.type)
          .subscribe();
      }
      // Reset
      this.connectionSource.set(null);
    }
  }
}
