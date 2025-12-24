import { Component, input, output, signal } from '@angular/core';
import { PlacementMode, SelectedElement } from '../../models/simulation.model';

@Component({
    selector: 'app-control-panel',
    standalone: true,
    imports: [],
    templateUrl: './control-panel.component.html',
})
export class ControlPanelComponent {
    // Inputs
    placementMode = input<PlacementMode>('none');
    selectedElement = input<SelectedElement | null>(null);

    // Outputs
    onModeChange = output<PlacementMode>();
    onDeleteElement = output<SelectedElement>();

    setMode(mode: PlacementMode): void {
        // Toggle off if clicking same mode
        if (this.placementMode() === mode) {
            this.onModeChange.emit('none');
        } else {
            this.onModeChange.emit(mode);
        }
    }

    deleteSelected(): void {
        const selected = this.selectedElement();
        if (selected) {
            this.onDeleteElement.emit(selected);
        }
    }
}
