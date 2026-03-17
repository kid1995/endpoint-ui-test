package de.signaliduna.visualizer.adapter.database;

import de.signaliduna.visualizer.adapter.database.model.AppEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRepository extends JpaRepository<AppEntity, Long> {
}
