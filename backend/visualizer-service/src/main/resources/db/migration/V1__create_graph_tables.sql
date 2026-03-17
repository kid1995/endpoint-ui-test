CREATE TABLE app (
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name       TEXT NOT NULL,
    description TEXT
);

CREATE TABLE service_node (
    id             BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name           TEXT NOT NULL,
    base_url       TEXT NOT NULL,
    kafka_topic    TEXT,
    mock_response  TEXT,
    status         TEXT NOT NULL DEFAULT 'UNKNOWN',
    position_x     DOUBLE PRECISION NOT NULL DEFAULT 0,
    position_y     DOUBLE PRECISION NOT NULL DEFAULT 0,
    app_id         BIGINT REFERENCES app(id)
);

CREATE TABLE service_edge (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    source_node_id  BIGINT NOT NULL REFERENCES service_node(id),
    target_node_id  BIGINT NOT NULL REFERENCES service_node(id),
    edge_type       TEXT NOT NULL,
    label           TEXT,
    latency_ms      BIGINT
);

CREATE INDEX idx_node_app_id ON service_node(app_id);
CREATE INDEX idx_edge_source ON service_edge(source_node_id);
CREATE INDEX idx_edge_target ON service_edge(target_node_id);
