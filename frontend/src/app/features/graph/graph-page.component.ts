import { Component, OnInit, inject } from '@angular/core';
import { GraphStore } from '@core/stores/graph.store';
import { TelemetryStore } from '@core/stores/telemetry.store';
import { GraphCanvasComponent } from './components/graph-canvas.component';
import { NodeConfigPanelComponent } from './components/node-config-panel.component';
import { TelemetryPanelComponent } from './components/telemetry-panel.component';

@Component({
  selector: 'app-graph-page',
  standalone: true,
  imports: [GraphCanvasComponent, NodeConfigPanelComponent, TelemetryPanelComponent],
  template: `
    <div class="graph-layout">
      <div class="canvas-area">
        <app-graph-canvas />
      </div>
      <div class="side-panel">
        <app-node-config-panel />
        <app-telemetry-panel />
      </div>
    </div>
  `,
  styles: [`
    .graph-layout {
      display: flex;
      height: calc(100vh - 57px);
    }
    .canvas-area {
      flex: 1;
      position: relative;
      overflow: hidden;
    }
    .side-panel {
      width: 380px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 8px;
      overflow-y: auto;
      border-left: 1px solid var(--border-color);
    }
  `],
})
export class GraphPageComponent implements OnInit {
  private readonly graphStore = inject(GraphStore);
  private readonly telemetryStore = inject(TelemetryStore);

  ngOnInit(): void {
    this.graphStore.loadGraph();
    this.telemetryStore.connect();
  }
}
