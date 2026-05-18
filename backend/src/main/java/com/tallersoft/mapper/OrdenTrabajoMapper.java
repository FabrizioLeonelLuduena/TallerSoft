package com.tallersoft.mapper;

import com.tallersoft.model.OrdenTrabajo;
import com.tallersoft.dto.OrdenTrabajoRequest;
import com.tallersoft.dto.OrdenTrabajoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrdenTrabajoMapper {
    
    @Mapping(source = "equipo.id", target = "equipoId")
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nombre", target = "clienteNombre")
    @Mapping(source = "tecnico.id", target = "tecnicoId")
    @Mapping(source = "tecnico.nombre", target = "tecnicoNombre")
    OrdenTrabajoResponse toResponse(OrdenTrabajo orden);
    
    OrdenTrabajo toEntity(OrdenTrabajoRequest request);
}
