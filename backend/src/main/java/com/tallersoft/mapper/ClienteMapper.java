package com.tallersoft.mapper;

import com.tallersoft.dto.ClienteRequest;
import com.tallersoft.dto.ClienteResponse;
import com.tallersoft.model.Cliente;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Cliente entity <-> DTO conversions
 */
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    
    Cliente toEntity(ClienteRequest request);
    
    ClienteResponse toResponse(Cliente entity);
    
    List<ClienteResponse> toResponseList(List<Cliente> entities);
}
