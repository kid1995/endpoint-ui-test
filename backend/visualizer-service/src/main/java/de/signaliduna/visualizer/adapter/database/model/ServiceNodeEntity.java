package de.signaliduna.visualizer.adapter.database.model;

import de.signaliduna.visualizer.model.ServiceNodeDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_node")
public class ServiceNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "kafka_topic")
    private String kafkaTopic;

    @Column(name = "mock_response", columnDefinition = "TEXT")
    private String mockResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceNodeDto.ServiceStatus status;

    @Column(name = "position_x", nullable = false)
    private Double positionX;

    @Column(name = "position_y", nullable = false)
    private Double positionY;

    @Column(name = "app_id")
    private Long appId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getKafkaTopic() { return kafkaTopic; }
    public void setKafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; }
    public String getMockResponse() { return mockResponse; }
    public void setMockResponse(String mockResponse) { this.mockResponse = mockResponse; }
    public ServiceNodeDto.ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceNodeDto.ServiceStatus status) { this.status = status; }
    public Double getPositionX() { return positionX; }
    public void setPositionX(Double positionX) { this.positionX = positionX; }
    public Double getPositionY() { return positionY; }
    public void setPositionY(Double positionY) { this.positionY = positionY; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
}
