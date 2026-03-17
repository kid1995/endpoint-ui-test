package de.signaliduna.visualizer.adapter.http;

import de.signaliduna.visualizer.core.GraphService;
import de.signaliduna.visualizer.model.AppDto;
import de.signaliduna.visualizer.model.ServiceEdgeDto;
import de.signaliduna.visualizer.model.ServiceNodeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    // --- Nodes ---

    @GetMapping("/nodes")
    public List<ServiceNodeDto> getAllNodes() {
        return graphService.getAllNodes();
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<ServiceNodeDto> getNodeById(@PathVariable Long id) {
        return graphService.getNodeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/nodes")
    public ResponseEntity<ServiceNodeDto> createNode(@Valid @RequestBody ServiceNodeDto dto) {
        var created = graphService.createNode(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/nodes/{id}")
    public ServiceNodeDto updateNode(@PathVariable Long id, @Valid @RequestBody ServiceNodeDto dto) {
        return graphService.updateNode(id, dto);
    }

    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id) {
        graphService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }

    // --- Edges ---

    @GetMapping("/edges")
    public List<ServiceEdgeDto> getAllEdges() {
        return graphService.getAllEdges();
    }

    @PostMapping("/edges")
    public ResponseEntity<ServiceEdgeDto> createEdge(@Valid @RequestBody ServiceEdgeDto dto) {
        var created = graphService.createEdge(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/edges/{id}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long id) {
        graphService.deleteEdge(id);
        return ResponseEntity.noContent().build();
    }

    // --- Apps ---

    @GetMapping("/apps")
    public List<AppDto> getAllApps() {
        return graphService.getAllApps();
    }

    @PostMapping("/apps")
    public ResponseEntity<AppDto> createApp(@Valid @RequestBody AppDto dto) {
        var created = graphService.createApp(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/apps/{id}")
    public ResponseEntity<Void> deleteApp(@PathVariable Long id) {
        graphService.deleteApp(id);
        return ResponseEntity.noContent().build();
    }
}
