package com.tallersoft.mapper;

import com.tallersoft.dto.EquipoRequest;
import com.tallersoft.dto.EquipoResponse;
import com.tallersoft.model.Equipo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Equipo entity <-> DTO conversions
 */
@Mapper(componentModel = "spring")
public interface EquipoMapper {
    
    Equipo toEntity(EquipoRequest request);
    
    EquipoResponse toResponse(Equipo entity);
    
    List<EquipoResponse> toResponseList(List<Equipo> entities);
}
