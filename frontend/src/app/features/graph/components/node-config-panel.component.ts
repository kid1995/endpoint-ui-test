import { Component, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { GraphStore } from '@core/stores/graph.store';
import { TelemetryStore } from '@core/stores/telemetry.store';
import { ServiceNode, ProxyRequest, ProxyResponse, KafkaMessage } from '@shared/models/graph.model';

@Component({
  selector: 'app-node-config-panel',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="panel">
      <h3>Node Configuration</h3>

      @if (node(); as n) {
        <div class="form-group">
          <label>Name</label>
          <input [ngModel]="n.name" (ngModelChange)="updateField('name', $event)" />
        </div>
        <div class="form-group">
          <label>Base URL</label>
          <input [ngModel]="n.baseUrl" (ngModelChange)="updateField('baseUrl', $event)" />
        </div>
        <div class="form-group">
          <label>Kafka Topic</label>
          <input [ngModel]="n.kafkaTopic ?? ''" (ngModelChange)="updateField('kafkaTopic', $event)" />
        </div>
        <div class="form-group">
          <label>Status</label>
          <select [ngModel]="n.status" (ngModelChange)="updateField('status', $event)">
            <option value="ONLINE">ONLINE</option>
            <option value="OFFLINE">OFFLINE</option>
            <option value="DEGRADED">DEGRADED</option>
            <option value="UNKNOWN">UNKNOWN</option>
          </select>
        </div>

        <div class="form-group">
          <button class="btn btn-primary" (click)="saveNode()">Save</button>
          <button class="btn btn-danger" (click)="graphStore.deleteNode(n.id!)">Delete</button>
        </div>

        <hr />

        <!-- HTTP Test -->
        <h4>Send HTTP Request</h4>
        <div class="form-group">
          <label>Method</label>
          <select [(ngModel)]="httpMethod">
            <option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option>
          </select>
        </div>
        <div class="form-group">
          <label>Path</label>
          <input [(ngModel)]="httpPath" placeholder="/hints" />
        </div>
        <div class="form-group">
          <label>Body (JSON)</label>
          <textarea [(ngModel)]="httpBody" rows="5"></textarea>
        </div>
        <button class="btn btn-success" (click)="sendHttpRequest()">Send Request</button>

        @if (lastResponse) {
          <div class="response-box">
            <div class="response-header">
              <span class="badge" [class.badge-success]="lastResponse.statusCode < 400"
                    [class.badge-error]="lastResponse.statusCode >= 400">
                {{ lastResponse.statusCode }}
              </span>
              <span>{{ lastResponse.latencyMs }}ms</span>
            </div>
            <pre>{{ lastResponse.body | json }}</pre>
          </div>
        }

        <hr />

        <!-- Kafka Test -->
        @if (n.kafkaTopic) {
          <h4>Send Kafka Message</h4>
          <div class="form-group">
            <label>Payload (JSON)</label>
            <textarea [(ngModel)]="kafkaPayload" rows="4"></textarea>
          </div>
          <button class="btn btn-success" (click)="sendKafkaMessage()">Produce Message</button>
        }
      } @else {
        <p class="hint-text">Select a node to configure it</p>
      }
    </div>
  `,
  styles: [`
    h3 { margin-bottom: 12px; font-size: 15px; }
    h4 { margin: 8px 0; font-size: 13px; color: var(--accent-blue); }
    hr { border: none; border-top: 1px solid var(--border-color); margin: 12px 0; }
    .form-group { margin-bottom: 8px; }
    .form-group label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
    .form-group button { margin-right: 8px; }
    textarea { font-family: monospace; font-size: 12px; }
    .hint-text { color: var(--text-secondary); font-size: 13px; }
    .response-box {
      margin-top: 8px;
      padding: 8px;
      background: var(--bg-primary);
      border-radius: 6px;
      font-size: 12px;
    }
    .response-header { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
    pre { white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; }
  `],
})
export class NodeConfigPanelComponent {
  readonly graphStore = inject(GraphStore);
  private readonly telemetryStore = inject(TelemetryStore);
  private readonly http = inject(HttpClient);

  readonly node = computed(() => this.graphStore.selectedNode());

  httpMethod = 'GET';
  httpPath = '/hints';
  httpBody = '';
  kafkaPayload = '';
  lastResponse: ProxyResponse | null = null;

  private editedFields: Partial<ServiceNode> = {};

  updateField(field: string, value: unknown): void {
    this.editedFields = { ...this.editedFields, [field]: value };
  }

  saveNode(): void {
    const current = this.node();
    if (current?.id != null) {
      const updated = { ...current, ...this.editedFields } as ServiceNode;
      this.graphStore.updateNode(current.id, updated);
      this.editedFields = {};
    }
  }

  sendHttpRequest(): void {
    const n = this.node();
    if (!n) return;

    const request: ProxyRequest = {
      targetUrl: n.baseUrl + this.httpPath,
      method: this.httpMethod as ProxyRequest['method'],
      body: this.httpBody || undefined,
      sessionId: this.telemetryStore.sessionId(),
      sourceNodeName: 'visualizer',
      targetNodeName: n.name,
    };

    this.http.post<ProxyResponse>('/api/proxy/http', request).subscribe({
      next: res => this.lastResponse = res,
      error: err => console.error('Proxy request failed', err),
    });
  }

  sendKafkaMessage(): void {
    const n = this.node();
    if (!n?.kafkaTopic) return;

    const message: KafkaMessage = {
      topic: n.kafkaTopic,
      payload: this.kafkaPayload,
      sessionId: this.telemetryStore.sessionId(),
      sourceNodeName: n.name,
    };

    this.http.post('/api/proxy/kafka', message).subscribe({
      error: err => console.error('Kafka produce failed', err),
    });
  }
}
