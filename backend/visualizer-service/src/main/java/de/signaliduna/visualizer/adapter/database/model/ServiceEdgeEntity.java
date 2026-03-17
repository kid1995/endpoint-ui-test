package de.signaliduna.visualizer.adapter.database.model;

import de.signaliduna.visualizer.model.ServiceEdgeDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_edge")
public class ServiceEdgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type", nullable = false)
    private ServiceEdgeDto.EdgeType edgeType;

    private String label;

    @Column(name = "latency_ms")
    private Long latencyMs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(Long sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public Long getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(Long targetNodeId) { this.targetNodeId = targetNodeId; }
    public ServiceEdgeDto.EdgeType getEdgeType() { return edgeType; }
    public void setEdgeType(ServiceEdgeDto.EdgeType edgeType) { this.edgeType = edgeType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
}
