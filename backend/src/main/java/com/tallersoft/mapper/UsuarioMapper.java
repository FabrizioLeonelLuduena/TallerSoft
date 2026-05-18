package com.tallersoft.mapper;

import com.tallersoft.dto.UsuarioRequest;
import com.tallersoft.dto.UsuarioResponse;
import com.tallersoft.model.Usuario;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Usuario entity <-> DTO conversions
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    
    Usuario toEntity(UsuarioRequest request);
    
    UsuarioResponse toResponse(Usuario entity);
    
    List<UsuarioResponse> toResponseList(List<Usuario> entities);
}
