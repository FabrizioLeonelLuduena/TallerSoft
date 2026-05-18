package com.tallersoft.service;

import com.tallersoft.dto.EquipoRequest;
import com.tallersoft.dto.EquipoResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.EquipoMapper;
import com.tallersoft.model.Cliente;
import com.tallersoft.model.Equipo;
import com.tallersoft.repository.ClienteRepository;
import com.tallersoft.repository.EquipoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic service for Equipo entity
 */
@Slf4j
@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EquipoMapper equipoMapper;

    /**
     * Create a new equipment
     *
     * @param request EquipoRequest with equipment details
     * @return EquipoResponse with created equipment
     * @throws EntityNotFoundException if client not found
     */
    @Transactional
    public EquipoResponse crearEquipo(EquipoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cliente no encontrado con ID: " + request.getClienteId()));
        
        Equipo equipo = Equipo.builder()
                .cliente(cliente)
                .tipo(request.getTipo())
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .numeroSerie(request.getNumeroSerie())
                .observaciones(request.getObservaciones())
                .build();
        
        Equipo saved = equipoRepository.save(equipo);
        log.info("Equipo creado: {} para cliente: {}", saved.getTipo(), cliente.getNombre());
        return equipoMapper.toResponse(saved);
    }

    /**
     * List all equipment for a specific client
     *
     * @param clienteId Client ID
     * @return List of EquipoResponse
     */
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarEquiposDelCliente(Long clienteId) {
        List<Equipo> equipos = equipoRepository.findByClienteId(clienteId);
        return equipoMapper.toResponseList(equipos);
    }

    /**
     * Update an existing equipment
     *
     * @param id Equipment ID
     * @param request EquipoRequest with updated details
     * @return EquipoResponse
     * @throws EntityNotFoundException if equipment not found
     */
    @Transactional
    public EquipoResponse editarEquipo(Long id, EquipoRequest request) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Equipo no encontrado con ID: " + id));
        
        equipo.setTipo(request.getTipo());
        equipo.setMarca(request.getMarca());
        equipo.setModelo(request.getModelo());
        equipo.setNumeroSerie(request.getNumeroSerie());
        equipo.setObservaciones(request.getObservaciones());
        
        Equipo updated = equipoRepository.save(equipo);
        log.info("Equipo actualizado: {}", updated.getTipo());
        return equipoMapper.toResponse(updated);
    }
}
