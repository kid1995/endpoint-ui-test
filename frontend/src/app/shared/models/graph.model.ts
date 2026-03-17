export interface ServiceNode {
  readonly id?: number;
  readonly name: string;
  readonly baseUrl: string;
  readonly kafkaTopic?: string;
  readonly mockResponse?: string;
  readonly status: ServiceStatus;
  readonly positionX: number;
  readonly positionY: number;
  readonly appId?: number;
}

export type ServiceStatus = 'ONLINE' | 'OFFLINE' | 'DEGRADED' | 'UNKNOWN';

export interface ServiceEdge {
  readonly id?: number;
  readonly sourceNodeId: number;
  readonly targetNodeId: number;
  readonly edgeType: EdgeType;
  readonly label?: string;
  readonly latencyMs?: number;
}

export type EdgeType = 'HTTP' | 'KAFKA';

export interface App {
  readonly id?: number;
  readonly name: string;
  readonly description?: string;
  readonly nodeIds: readonly number[];
}

export interface TransactionEvent {
  readonly id: string;
  readonly sessionId: string;
  readonly sourceNode: string;
  readonly targetNode: string;
  readonly eventType: EventType;
  readonly status: EventStatus;
  readonly latencyMs?: number;
  readonly timestamp: string;
  readonly traceId: string;
  readonly payload?: string;
  readonly responseBody?: string;
  readonly httpStatus?: number;
}

export type EventType = 'HTTP_REQUEST' | 'HTTP_RESPONSE' | 'KAFKA_PRODUCE' | 'KAFKA_CONSUME';
export type EventStatus = 'IN_FLIGHT' | 'SUCCESS' | 'FAILURE' | 'TIMEOUT';

export interface ProxyRequest {
  readonly targetUrl: string;
  readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  readonly headers?: Record<string, string>;
  readonly body?: string;
  readonly sessionId?: string;
  readonly sourceNodeName?: string;
  readonly targetNodeName?: string;
}

export interface ProxyResponse {
  readonly statusCode: number;
  readonly headers: Record<string, string>;
  readonly body: string;
  readonly latencyMs: number;
  readonly traceId: string;
}

export interface KafkaMessage {
  readonly topic: string;
  readonly key?: string;
  readonly payload: string;
  readonly sessionId?: string;
  readonly sourceNodeName?: string;
}
