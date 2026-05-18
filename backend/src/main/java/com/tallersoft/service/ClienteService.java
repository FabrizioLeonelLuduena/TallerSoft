package com.tallersoft.service;

import com.tallersoft.dto.ClienteRequest;
import com.tallersoft.dto.ClienteResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.ClienteMapper;
import com.tallersoft.model.Cliente;
import com.tallersoft.repository.ClienteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic service for Cliente entity
 */
@Slf4j
@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    /**
     * Create a new client
     *
     * @param request ClienteRequest with client details
     * @return ClienteResponse with created client
     */
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setActivo(true);
        Cliente saved = clienteRepository.save(cliente);
        log.info("Cliente creado: {}", saved.getNombre());
        return clienteMapper.toResponse(saved);
    }

    /**
     * Get client by ID
     *
     * @param id Client ID
     * @return ClienteResponse
     * @throws EntityNotFoundException if client not found
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
        return clienteMapper.toResponse(cliente);
    }

    /**
     * List all active clients, optionally filtered by name
     *
     * @param nombre Optional name filter
     * @return List of ClienteResponse
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes(String nombre) {
        List<Cliente> clientes;
        if (nombre != null && !nombre.isBlank()) {
            clientes = clienteRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre);
        } else {
            clientes = clienteRepository.findByActivoTrue();
        }
        return clienteMapper.toResponseList(clientes);
    }

    /**
     * Update an existing client
     *
     * @param id Client ID
     * @param request ClienteRequest with updated details
     * @return ClienteResponse
     * @throws EntityNotFoundException if client not found
     */
    @Transactional
    public ClienteResponse editarCliente(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
        
        cliente.setNombre(request.getNombre());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        
        Cliente updated = clienteRepository.save(cliente);
        log.info("Cliente actualizado: {}", updated.getNombre());
        return clienteMapper.toResponse(updated);
    }

    /**
     * Soft delete a client (sets activo=false)
     *
     * @param id Client ID
     * @throws EntityNotFoundException if client not found
     */
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
        
        cliente.setActivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente eliminado (soft delete): {}", cliente.getNombre());
    }
}
