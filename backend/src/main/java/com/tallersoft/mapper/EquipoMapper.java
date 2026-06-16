package com.tallersoft.mapper;

import com.tallersoft.dto.EquipoRequest;
import com.tallersoft.dto.EquipoResponse;
import com.tallersoft.model.Equipo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Equipo entity <-> DTO conversions
 */
@Mapper(componentModel = "spring")
public interface EquipoMapper {

    Equipo toEntity(EquipoRequest request);

    @Mapping(source = "cliente.id", target = "clienteId")
    EquipoResponse toResponse(Equipo entity);

    @Mapping(source = "cliente.id", target = "clienteId")
    List<EquipoResponse> toResponseList(List<Equipo> entities);
}
