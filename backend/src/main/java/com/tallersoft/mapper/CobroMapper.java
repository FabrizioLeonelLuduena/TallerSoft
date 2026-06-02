package com.tallersoft.mapper;

import com.tallersoft.dto.CobroResponse;
import com.tallersoft.model.Cobro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CobroMapper {

    @Mapping(source = "orden.id", target = "ordenId")
    @Mapping(source = "orden.cliente.nombre", target = "clienteNombre")
    CobroResponse toResponse(Cobro cobro);
}
