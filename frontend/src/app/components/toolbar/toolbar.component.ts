import { Component, input, output, computed } from '@angular/core';
import { Snapshot } from '../../models/simulation.model';

@Component({
    selector: 'app-toolbar',
    standalone: true,
    imports: [],
    templateUrl: './toolbar.component.html',
})
export class ToolbarComponent {
    // Inputs
    isRunning = input<boolean>(false);
    isConnected = input<boolean>(false);
    snapshots = input<Snapshot[]>([]);
    showReplayDropdown = false;

    // Outputs
    onStart = output<void>();
    onStop = output<void>();
    onNew = output<void>();
    onReplay = output<string>();

    // Computed
    statusText = computed(() => {
        if (!this.isConnected()) return 'Disconnected';
        return this.isRunning() ? 'Running' : 'Stopped';
    });

    statusColor = computed(() => {
        if (!this.isConnected()) return 'bg-red-500';
        return this.isRunning() ? 'bg-green-500' : 'bg-yellow-500';
    });

    toggleSimulation(): void {
        if (this.isRunning()) {
            this.onStop.emit();
        } else {
            this.onStart.emit();
        }
    }

    newSimulation(): void {
        this.onNew.emit();
    }

    toggleReplayDropdown(): void {
        this.showReplayDropdown = !this.showReplayDropdown;
    }

    selectSnapshot(snapshotId: string): void {
        this.onReplay.emit(snapshotId);
        this.showReplayDropdown = false;
    }
}
