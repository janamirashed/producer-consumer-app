import { Component, input, output } from '@angular/core';
import { Snapshot } from '../../models/simulation.model';

@Component({
    selector: 'app-snapshot-list',
    standalone: true,
    imports: [],
    templateUrl: './snapshot-list.component.html',
})
export class SnapshotListComponent {
    // Inputs
    snapshots = input<Snapshot[]>([]);
    isOpen = input<boolean>(false);

    // Outputs
    onSelect = output<string>();
    onClose = output<void>();
    onCreate = output<void>();

    selectSnapshot(label: string): void {
        this.onSelect.emit(label);
    }

    close(): void {
        this.onClose.emit();
    }

    createSnapshot(): void {
        this.onCreate.emit();
    }

    formatDate(date: Date | string): string {
        const d = new Date(date);
        return d.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    }
}
