package de.signaliduna.visualizer.adapter.database;

import de.signaliduna.visualizer.adapter.database.model.ServiceNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceNodeRepository extends JpaRepository<ServiceNodeEntity, Long> {

    List<ServiceNodeEntity> findAllByAppId(Long appId);
}
