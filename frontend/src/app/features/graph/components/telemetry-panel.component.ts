import { Component, inject, computed } from '@angular/core';
import { TelemetryStore } from '@core/stores/telemetry.store';
import { TransactionEvent } from '@shared/models/graph.model';

@Component({
  selector: 'app-telemetry-panel',
  standalone: true,
  template: `
    <div class="panel telemetry-panel">
      <div class="panel-header">
        <h3>Telemetry</h3>
        <div class="actions">
          <button class="btn btn-primary" (click)="telemetryStore.newSession()">New Session</button>
          <button class="btn btn-danger" (click)="telemetryStore.clearEvents()">Clear</button>
        </div>
      </div>

      <div class="event-list">
        @for (event of recentEvents(); track event.id) {
          <div class="event-row" [class]="'event-' + event.status.toLowerCase()">
            <div class="event-header">
              <span class="event-type">{{ formatType(event.eventType) }}</span>
              <span class="badge"
                    [class.badge-success]="event.status === 'SUCCESS'"
                    [class.badge-error]="event.status === 'FAILURE'"
                    [class.badge-inflight]="event.status === 'IN_FLIGHT'">
                {{ event.status }}
              </span>
            </div>
            <div class="event-route">
              {{ event.sourceNode }} → {{ event.targetNode }}
            </div>
            @if (event.latencyMs != null) {
              <div class="event-latency">{{ event.latencyMs }}ms</div>
            }
            @if (event.httpStatus) {
              <div class="event-status-code">HTTP {{ event.httpStatus }}</div>
            }
          </div>
        }

        @if (telemetryStore.events().length === 0) {
          <p class="empty-text">No events yet. Send a request to see telemetry.</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .telemetry-panel { flex: 1; display: flex; flex-direction: column; min-height: 0; }
    .panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .panel-header h3 { font-size: 15px; }
    .actions { display: flex; gap: 6px; }
    .event-list { flex: 1; overflow-y: auto; }
    .event-row {
      padding: 8px;
      border-radius: 6px;
      margin-bottom: 4px;
      background: var(--bg-primary);
      font-size: 12px;
    }
    .event-header { display: flex; justify-content: space-between; align-items: center; }
    .event-type { font-weight: 600; color: var(--accent-blue); }
    .event-route { color: var(--text-secondary); margin-top: 2px; }
    .event-latency { color: var(--accent-green); font-weight: 500; }
    .event-status-code { color: var(--text-secondary); }
    .event-FAILURE .event-type { color: var(--accent-red); }
    .empty-text { color: var(--text-secondary); font-size: 13px; text-align: center; margin-top: 24px; }
  `],
})
export class TelemetryPanelComponent {
  readonly telemetryStore = inject(TelemetryStore);

  readonly recentEvents = computed(() =>
    [...this.telemetryStore.events()].reverse().slice(0, 50)
  );

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }
}
