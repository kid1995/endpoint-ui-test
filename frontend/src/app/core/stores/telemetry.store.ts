import { Injectable, signal, inject, OnDestroy } from '@angular/core';
import { TransactionEvent } from '@shared/models/graph.model';
import { GraphStore } from './graph.store';

@Injectable({ providedIn: 'root' })
export class TelemetryStore implements OnDestroy {
  private readonly graphStore = inject(GraphStore);

  private readonly _events = signal<readonly TransactionEvent[]>([]);
  private readonly _sessionId = signal<string>('session-' + Date.now());
  private readonly _connected = signal(false);
  private eventSource: EventSource | null = null;

  readonly events = this._events.asReadonly();
  readonly sessionId = this._sessionId.asReadonly();
  readonly connected = this._connected.asReadonly();

  connect(): void {
    this.disconnect();

    const url = `/api/telemetry/${this._sessionId()}/events`;
    this.eventSource = new EventSource(url);

    this.eventSource.onopen = () => this._connected.set(true);
    this.eventSource.onerror = () => {
      this._connected.set(false);
      setTimeout(() => this.connect(), 3000);
    };

    for (const type of ['HTTP_REQUEST', 'HTTP_RESPONSE', 'KAFKA_PRODUCE', 'KAFKA_CONSUME']) {
      this.eventSource.addEventListener(type, (event: MessageEvent) => {
        const txEvent: TransactionEvent = JSON.parse(event.data);
        this._events.update(events => [...events, txEvent]);

        if (txEvent.eventType === 'HTTP_RESPONSE' && txEvent.latencyMs != null) {
          const nodes = this.graphStore.nodes();
          const source = nodes.find(n => n.name === txEvent.sourceNode);
          const target = nodes.find(n => n.name === txEvent.targetNode);
          if (source?.id != null && target?.id != null) {
            this.graphStore.updateEdgeLatency(source.id, target.id, txEvent.latencyMs);
          }
        }
      });
    }
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this._connected.set(false);
    }
  }

  clearEvents(): void {
    this._events.set([]);
  }

  newSession(): void {
    this.disconnect();
    this._sessionId.set('session-' + Date.now());
    this._events.set([]);
    this.connect();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
