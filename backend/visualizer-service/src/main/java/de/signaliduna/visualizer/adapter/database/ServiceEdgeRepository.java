package de.signaliduna.visualizer.adapter.database;

import de.signaliduna.visualizer.adapter.database.model.ServiceEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceEdgeRepository extends JpaRepository<ServiceEdgeEntity, Long> {

    List<ServiceEdgeEntity> findAllBySourceNodeIdOrTargetNodeId(Long sourceNodeId, Long targetNodeId);
}
