package de.signaliduna.visualizer.core;

import de.signaliduna.visualizer.adapter.database.AppRepository;
import de.signaliduna.visualizer.adapter.database.GraphMapper;
import de.signaliduna.visualizer.adapter.database.ServiceEdgeRepository;
import de.signaliduna.visualizer.adapter.database.ServiceNodeRepository;
import de.signaliduna.visualizer.model.AppDto;
import de.signaliduna.visualizer.model.ServiceEdgeDto;
import de.signaliduna.visualizer.model.ServiceNodeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GraphService {

    private final ServiceNodeRepository nodeRepository;
    private final ServiceEdgeRepository edgeRepository;
    private final AppRepository appRepository;
    private final GraphMapper mapper;

    public GraphService(ServiceNodeRepository nodeRepository,
                        ServiceEdgeRepository edgeRepository,
                        AppRepository appRepository,
                        GraphMapper mapper) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.appRepository = appRepository;
        this.mapper = mapper;
    }

    // --- Nodes ---

    public List<ServiceNodeDto> getAllNodes() {
        return nodeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    public Optional<ServiceNodeDto> getNodeById(Long id) {
        return nodeRepository.findById(id).map(mapper::toDto);
    }

    @Transactional
    public ServiceNodeDto createNode(ServiceNodeDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = nodeRepository.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional
    public ServiceNodeDto updateNode(Long id, ServiceNodeDto dto) {
        var entity = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + id));
        entity.setName(dto.name());
        entity.setBaseUrl(dto.baseUrl());
        entity.setKafkaTopic(dto.kafkaTopic());
        entity.setMockResponse(dto.mockResponse());
        entity.setStatus(dto.status());
        entity.setPositionX(dto.positionX());
        entity.setPositionY(dto.positionY());
        entity.setAppId(dto.appId());
        var saved = nodeRepository.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteNode(Long id) {
        edgeRepository.findAllBySourceNodeIdOrTargetNodeId(id, id)
                .forEach(edge -> edgeRepository.deleteById(edge.getId()));
        nodeRepository.deleteById(id);
    }

    // --- Edges ---

    public List<ServiceEdgeDto> getAllEdges() {
        return edgeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ServiceEdgeDto createEdge(ServiceEdgeDto dto) {
        validateNodeExists(dto.sourceNodeId());
        validateNodeExists(dto.targetNodeId());
        var entity = mapper.toEntity(dto);
        var saved = edgeRepository.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteEdge(Long id) {
        edgeRepository.deleteById(id);
    }

    // --- Apps ---

    public List<AppDto> getAllApps() {
        return appRepository.findAll().stream()
                .map(entity -> {
                    var baseDto = mapper.toDto(entity);
                    var nodeIds = nodeRepository.findAllByAppId(entity.getId()).stream()
                            .map(n -> n.getId())
                            .toList();
                    return new AppDto(baseDto.id(), baseDto.name(), baseDto.description(), nodeIds);
                })
                .toList();
    }

    @Transactional
    public AppDto createApp(AppDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = appRepository.save(entity);
        return new AppDto(saved.getId(), saved.getName(), saved.getDescription(), List.of());
    }

    @Transactional
    public void deleteApp(Long id) {
        nodeRepository.findAllByAppId(id).forEach(node -> {
            node.setAppId(null);
            nodeRepository.save(node);
        });
        appRepository.deleteById(id);
    }

    private void validateNodeExists(Long nodeId) {
        if (!nodeRepository.existsById(nodeId)) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
    }
}
