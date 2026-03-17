package de.signaliduna.visualizer.adapter.database;

import de.signaliduna.visualizer.adapter.database.model.AppEntity;
import de.signaliduna.visualizer.adapter.database.model.ServiceEdgeEntity;
import de.signaliduna.visualizer.adapter.database.model.ServiceNodeEntity;
import de.signaliduna.visualizer.model.AppDto;
import de.signaliduna.visualizer.model.ServiceEdgeDto;
import de.signaliduna.visualizer.model.ServiceNodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GraphMapper {

    @Mapping(target = "id", ignore = true)
    ServiceNodeEntity toEntity(ServiceNodeDto dto);

    ServiceNodeDto toDto(ServiceNodeEntity entity);

    @Mapping(target = "id", ignore = true)
    ServiceEdgeEntity toEntity(ServiceEdgeDto dto);

    ServiceEdgeDto toDto(ServiceEdgeEntity entity);

    @Mapping(target = "id", ignore = true)
    AppEntity toEntity(AppDto dto);

    @Mapping(target = "nodeIds", ignore = true)
    AppDto toDto(AppEntity entity);
}
