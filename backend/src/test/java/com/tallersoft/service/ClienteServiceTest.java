package com.tallersoft.service;

import com.tallersoft.dto.ClienteRequest;
import com.tallersoft.dto.ClienteResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.ClienteMapper;
import com.tallersoft.model.Cliente;
import com.tallersoft.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClienteService
 * Tests CRUD operations and business logic
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente testCliente;
    private ClienteRequest testRequest;

    @BeforeEach
    void setUp() {
        testCliente = Cliente.builder()
                .id(1L)
                .nombre("Test Cliente")
                .telefono("123456789")
                .email("cliente@example.com")
                .direccion("Test Address")
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = new ClienteRequest();
        testRequest.setNombre("Test Cliente");
        testRequest.setTelefono("123456789");
        testRequest.setEmail("cliente@example.com");
        testRequest.setDireccion("Test Address");
    }

    @Test
    void testCrearCliente() {
        when(clienteMapper.toEntity(testRequest)).thenReturn(testCliente);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(testCliente);
        when(clienteMapper.toResponse(testCliente))
                .thenReturn(new ClienteResponse(1L, "Test Cliente", "123456789", 
                        "cliente@example.com", "Test Address", true, LocalDateTime.now()));

        ClienteResponse response = clienteService.crearCliente(testRequest);

        assertNotNull(response);
        assertEquals("Test Cliente", response.getNombre());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void testObtenerCliente_Found() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(testCliente));
        when(clienteMapper.toResponse(testCliente))
                .thenReturn(new ClienteResponse(1L, "Test Cliente", "123456789", 
                        "cliente@example.com", "Test Address", true, LocalDateTime.now()));

        ClienteResponse response = clienteService.obtenerCliente(1L);

        assertNotNull(response);
        assertEquals("Test Cliente", response.getNombre());
    }

    @Test
    void testObtenerCliente_NotFound() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.obtenerCliente(999L));
    }

    @Test
    void testListarClientes_NoFilter() {
        List<Cliente> clientes = Arrays.asList(testCliente);
        when(clienteRepository.findByActivoTrue()).thenReturn(clientes);
        when(clienteMapper.toResponseList(clientes))
                .thenReturn(Arrays.asList(
                        new ClienteResponse(1L, "Test Cliente", "123456789", 
                                "cliente@example.com", "Test Address", true, LocalDateTime.now())
                ));

        List<ClienteResponse> responses = clienteService.listarClientes(null, false);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(clienteRepository, times(1)).findByActivoTrue();
    }

    @Test
    void testListarClientes_WithFilter() {
        List<Cliente> clientes = Arrays.asList(testCliente);
        when(clienteRepository.findByNombreContainingIgnoreCaseAndActivoTrue("Test"))
                .thenReturn(clientes);
        when(clienteMapper.toResponseList(clientes))
                .thenReturn(Arrays.asList(
                        new ClienteResponse(1L, "Test Cliente", "123456789", 
                                "cliente@example.com", "Test Address", true, LocalDateTime.now())
                ));

        List<ClienteResponse> responses = clienteService.listarClientes("Test", false);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(clienteRepository, times(1))
                .findByNombreContainingIgnoreCaseAndActivoTrue("Test");
    }

    @Test
    void testEditarCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(testCliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(testCliente);
        when(clienteMapper.toResponse(testCliente))
                .thenReturn(new ClienteResponse(1L, "Test Cliente Updated", "123456789", 
                        "cliente@example.com", "Test Address", true, LocalDateTime.now()));

        testRequest.setNombre("Test Cliente Updated");
        ClienteResponse response = clienteService.editarCliente(1L, testRequest);

        assertNotNull(response);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void testEliminarCliente_SoftDelete() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(testCliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(testCliente);

        clienteService.eliminarCliente(1L);

        verify(clienteRepository, times(1)).save(any(Cliente.class));
        assertFalse(testCliente.isActivo());
    }

    @Test
    void testEliminarCliente_NotFound() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.eliminarCliente(999L));
    }
}
