import { Component, ElementRef, ViewChild, inject, AfterViewInit } from '@angular/core';
import { GraphStore } from '@core/stores/graph.store';
import { TelemetryStore } from '@core/stores/telemetry.store';
import { ServiceNode, ServiceEdge } from '@shared/models/graph.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-graph-canvas',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="toolbar">
      <button class="btn btn-primary" (click)="addNewNode()">+ Add Node</button>
      <span class="connection-status">
        @if (telemetryStore.connected()) {
          <span class="badge badge-success">SSE Connected</span>
        } @else {
          <span class="badge badge-error">SSE Disconnected</span>
        }
      </span>
    </div>

    <svg #svgCanvas
         class="graph-svg"
         [attr.width]="'100%'"
         [attr.height]="'100%'"
         (mouseup)="onMouseUp()"
         (mousemove)="onMouseMove($event)">

      <!-- Arrow marker definition -->
      <defs>
        <marker id="arrowhead" markerWidth="10" markerHeight="7"
                refX="10" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--accent-blue)" />
        </marker>
        <marker id="arrowhead-kafka" markerWidth="10" markerHeight="7"
                refX="10" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--accent-purple)" />
        </marker>
      </defs>

      <!-- Edges (arrows) -->
      @for (edge of graphStore.edges(); track edge.id) {
        <g class="edge-group">
          <line
            [attr.x1]="getNodeX(edge.sourceNodeId)"
            [attr.y1]="getNodeY(edge.sourceNodeId)"
            [attr.x2]="getNodeX(edge.targetNodeId)"
            [attr.y2]="getNodeY(edge.targetNodeId)"
            [attr.stroke]="edge.edgeType === 'KAFKA' ? 'var(--accent-purple)' : 'var(--accent-blue)'"
            [attr.stroke-dasharray]="edge.edgeType === 'KAFKA' ? '8,4' : 'none'"
            stroke-width="2"
            [attr.marker-end]="edge.edgeType === 'KAFKA' ? 'url(#arrowhead-kafka)' : 'url(#arrowhead)'"
          />
          @if (edge.latencyMs != null) {
            <text
              [attr.x]="(getNodeX(edge.sourceNodeId) + getNodeX(edge.targetNodeId)) / 2"
              [attr.y]="(getNodeY(edge.sourceNodeId) + getNodeY(edge.targetNodeId)) / 2 - 8"
              fill="var(--text-secondary)"
              font-size="11"
              text-anchor="middle">
              {{ edge.latencyMs }}ms
            </text>
          }
          @if (edge.label) {
            <text
              [attr.x]="(getNodeX(edge.sourceNodeId) + getNodeX(edge.targetNodeId)) / 2"
              [attr.y]="(getNodeY(edge.sourceNodeId) + getNodeY(edge.targetNodeId)) / 2 + 14"
              fill="var(--text-secondary)"
              font-size="10"
              text-anchor="middle">
              {{ edge.label }}
            </text>
          }
        </g>
      }

      <!-- Nodes (circles) -->
      @for (node of graphStore.nodes(); track node.id) {
        <g class="node-group"
           [class.selected]="node.id === graphStore.selectedNodeId()"
           (mousedown)="onNodeMouseDown($event, node)"
           (click)="graphStore.selectNode(node.id!)">

          <!-- Node circle -->
          <circle
            [attr.cx]="node.positionX"
            [attr.cy]="node.positionY"
            r="35"
            [attr.fill]="getNodeColor(node)"
            [attr.stroke]="node.id === graphStore.selectedNodeId() ? 'white' : 'var(--border-color)'"
            stroke-width="2"
            class="node-circle"
          />

          <!-- Node name -->
          <text
            [attr.x]="node.positionX"
            [attr.y]="node.positionY - 4"
            fill="white"
            font-size="11"
            font-weight="600"
            text-anchor="middle"
            dominant-baseline="middle">
            {{ truncateName(node.name) }}
          </text>

          <!-- Status indicator -->
          <text
            [attr.x]="node.positionX"
            [attr.y]="node.positionY + 12"
            [attr.fill]="getStatusColor(node.status)"
            font-size="9"
            text-anchor="middle">
            {{ node.status }}
          </text>
        </g>
      }
    </svg>
  `,
  styles: [`
    :host { display: block; width: 100%; height: 100%; position: relative; }
    .toolbar {
      position: absolute;
      top: 12px;
      left: 12px;
      z-index: 10;
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .graph-svg { cursor: default; }
    .node-group { cursor: grab; }
    .node-group:active { cursor: grabbing; }
    .node-circle { transition: stroke 0.15s; }
    .node-group:hover .node-circle { stroke: var(--accent-blue); }
  `],
})
export class GraphCanvasComponent {
  readonly graphStore = inject(GraphStore);
  readonly telemetryStore = inject(TelemetryStore);

  @ViewChild('svgCanvas') svgCanvas!: ElementRef<SVGSVGElement>;

  private draggingNodeId: number | null = null;

  getNodeX(nodeId: number): number {
    return this.graphStore.nodes().find(n => n.id === nodeId)?.positionX ?? 0;
  }

  getNodeY(nodeId: number): number {
    return this.graphStore.nodes().find(n => n.id === nodeId)?.positionY ?? 0;
  }

  getNodeColor(node: ServiceNode): string {
    switch (node.status) {
      case 'ONLINE': return '#166534';
      case 'OFFLINE': return '#991b1b';
      case 'DEGRADED': return '#854d0e';
      default: return 'var(--bg-panel)';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ONLINE': return 'var(--accent-green)';
      case 'OFFLINE': return 'var(--accent-red)';
      case 'DEGRADED': return 'var(--accent-yellow)';
      default: return 'var(--text-secondary)';
    }
  }

  truncateName(name: string): string {
    return name.length > 10 ? name.substring(0, 9) + '...' : name;
  }

  addNewNode(): void {
    const node: ServiceNode = {
      name: 'New Service',
      baseUrl: 'http://localhost:8080',
      status: 'UNKNOWN',
      positionX: 200 + Math.random() * 400,
      positionY: 200 + Math.random() * 300,
    };
    this.graphStore.addNode(node);
  }

  onNodeMouseDown(event: MouseEvent, node: ServiceNode): void {
    event.stopPropagation();
    if (node.id != null) {
      this.draggingNodeId = node.id;
    }
  }

  onMouseMove(event: MouseEvent): void {
    if (this.draggingNodeId === null) return;

    const svg = this.svgCanvas.nativeElement;
    const rect = svg.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    this.graphStore.updateNodePosition(this.draggingNodeId, x, y);
  }

  onMouseUp(): void {
    this.draggingNodeId = null;
  }
}
