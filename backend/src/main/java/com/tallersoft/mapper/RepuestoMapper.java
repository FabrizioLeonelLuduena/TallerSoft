package com.tallersoft.mapper;

import com.tallersoft.model.Repuesto;
import com.tallersoft.dto.RepuestoRequest;
import com.tallersoft.dto.RepuestoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RepuestoMapper {
    
    RepuestoResponse toResponse(Repuesto repuesto);
    
    Repuesto toEntity(RepuestoRequest request);
    
    default Boolean calculateCritico(Repuesto repuesto) {
        if (repuesto.getStockActual() == null || repuesto.getStockMinimo() == null) {
            return false;
        }
        return repuesto.getStockActual() <= repuesto.getStockMinimo();
    }

    default Boolean calculateBajo(Repuesto repuesto) {
        if (repuesto.getStockActual() == null || repuesto.getStockMinimo() == null || repuesto.getStockBajo() == null) {
            return false;
        }
        return repuesto.getStockActual() > repuesto.getStockMinimo()
                && repuesto.getStockActual() <= repuesto.getStockBajo();
    }
}
