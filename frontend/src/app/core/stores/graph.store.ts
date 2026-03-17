import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ServiceNode, ServiceEdge, App } from '@shared/models/graph.model';

@Injectable({ providedIn: 'root' })
export class GraphStore {
  private readonly http = inject(HttpClient);

  private readonly _nodes = signal<readonly ServiceNode[]>([]);
  private readonly _edges = signal<readonly ServiceEdge[]>([]);
  private readonly _apps = signal<readonly App[]>([]);
  private readonly _selectedNodeId = signal<number | null>(null);
  private readonly _loading = signal(false);

  readonly nodes = this._nodes.asReadonly();
  readonly edges = this._edges.asReadonly();
  readonly apps = this._apps.asReadonly();
  readonly selectedNodeId = this._selectedNodeId.asReadonly();
  readonly loading = this._loading.asReadonly();

  readonly selectedNode = computed(() => {
    const id = this._selectedNodeId();
    return id !== null ? this._nodes().find(n => n.id === id) ?? null : null;
  });

  selectNode(id: number | null): void {
    this._selectedNodeId.set(id);
  }

  loadGraph(): void {
    this._loading.set(true);
    this.http.get<ServiceNode[]>('/api/graph/nodes').subscribe({
      next: nodes => this._nodes.set(nodes),
      error: err => console.error('Failed to load nodes', err),
    });
    this.http.get<ServiceEdge[]>('/api/graph/edges').subscribe({
      next: edges => this._edges.set(edges),
      error: err => console.error('Failed to load edges', err),
      complete: () => this._loading.set(false),
    });
    this.http.get<App[]>('/api/graph/apps').subscribe({
      next: apps => this._apps.set(apps),
      error: err => console.error('Failed to load apps', err),
    });
  }

  addNode(node: ServiceNode): void {
    this.http.post<ServiceNode>('/api/graph/nodes', node).subscribe({
      next: created => this._nodes.update(nodes => [...nodes, created]),
      error: err => console.error('Failed to create node', err),
    });
  }

  updateNode(id: number, node: ServiceNode): void {
    this.http.put<ServiceNode>(`/api/graph/nodes/${id}`, node).subscribe({
      next: updated => this._nodes.update(nodes =>
        nodes.map(n => n.id === id ? updated : n)
      ),
      error: err => console.error('Failed to update node', err),
    });
  }

  updateNodePosition(id: number, x: number, y: number): void {
    const node = this._nodes().find(n => n.id === id);
    if (node) {
      const updated = { ...node, positionX: x, positionY: y };
      this._nodes.update(nodes => nodes.map(n => n.id === id ? updated : n));
      this.http.put<ServiceNode>(`/api/graph/nodes/${id}`, updated).subscribe({
        error: err => console.error('Failed to save node position', err),
      });
    }
  }

  deleteNode(id: number): void {
    this.http.delete(`/api/graph/nodes/${id}`).subscribe({
      next: () => {
        this._nodes.update(nodes => nodes.filter(n => n.id !== id));
        this._edges.update(edges =>
          edges.filter(e => e.sourceNodeId !== id && e.targetNodeId !== id)
        );
        if (this._selectedNodeId() === id) {
          this._selectedNodeId.set(null);
        }
      },
      error: err => console.error('Failed to delete node', err),
    });
  }

  addEdge(edge: ServiceEdge): void {
    this.http.post<ServiceEdge>('/api/graph/edges', edge).subscribe({
      next: created => this._edges.update(edges => [...edges, created]),
      error: err => console.error('Failed to create edge', err),
    });
  }

  deleteEdge(id: number): void {
    this.http.delete(`/api/graph/edges/${id}`).subscribe({
      next: () => this._edges.update(edges => edges.filter(e => e.id !== id)),
      error: err => console.error('Failed to delete edge', err),
    });
  }

  updateEdgeLatency(sourceId: number, targetId: number, latencyMs: number): void {
    this._edges.update(edges =>
      edges.map(e =>
        e.sourceNodeId === sourceId && e.targetNodeId === targetId
          ? { ...e, latencyMs }
          : e
      )
    );
  }
}
