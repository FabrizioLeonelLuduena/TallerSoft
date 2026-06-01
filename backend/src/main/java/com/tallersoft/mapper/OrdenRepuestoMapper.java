package com.tallersoft.mapper;

import com.tallersoft.model.OrdenRepuesto;
import com.tallersoft.dto.OrdenRepuestoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrdenRepuestoMapper {
    
    @Mapping(source = "orden.id", target = "ordenId")
    @Mapping(source = "repuesto.id", target = "repuestoId")
    @Mapping(source = "repuesto.nombre", target = "nombreRepuesto")
    @Mapping(target = "total", expression = "java(calculateTotal(ordenRepuesto))")
    OrdenRepuestoResponse toResponse(OrdenRepuesto ordenRepuesto);
    
    default BigDecimal calculateTotal(OrdenRepuesto ordenRepuesto) {
        if (ordenRepuesto.getCantidad() == null || ordenRepuesto.getPrecioUnit() == null) {
            return BigDecimal.ZERO;
        }
        return ordenRepuesto.getPrecioUnit().multiply(new BigDecimal(ordenRepuesto.getCantidad()));
    }
}
