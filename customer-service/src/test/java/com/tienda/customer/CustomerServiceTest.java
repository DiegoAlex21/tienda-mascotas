package com.tienda.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repo;

    @InjectMocks
    private CustomerService service;

    private Customer cliente(Long id) {
        return new Customer(id, "Laura", "Gomez", "1012345678", "3001234567",
                "laura@correo.com", "Calle 10 # 5-20", LocalDate.now());
    }

    private CustomerDTO dto(String documento, String correo) {
        return new CustomerDTO(null, "Laura", "Gomez", documento, "3001234567",
                correo, "Calle 10 # 5-20", null);
    }

    @Test
    void registrarCliente_guardaYAsignaFecha() {
        when(repo.save(any(Customer.class))).thenAnswer(i -> {
            Customer c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        Customer result = service.create(dto("1012345678", "laura@correo.com"));

        assertEquals(1L, result.getId());
        assertNotNull(result.getFechaRegistro());
        verify(repo).save(any(Customer.class));
    }

    @Test
    void registrarCliente_documentoDuplicado_lanza409() {
        when(repo.existsByDocumento("1012345678")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(dto("1012345678", "laura@correo.com")));

        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void registrarCliente_correoRepetido_lanza409() {
        when(repo.existsByCorreo("laura@correo.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(dto("1012345678", "laura@correo.com")));

        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void consultarCliente_porId_retornaCliente() {
        when(repo.findById(1L)).thenReturn(Optional.of(cliente(1L)));

        Customer result = service.findById(1L);

        assertEquals("Laura", result.getNombre());
    }

    @Test
    void consultarCliente_noExiste_lanza404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findById(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void actualizarCliente_cambiaLaInformacion() {
        when(repo.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(repo.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        CustomerDTO cambios = new CustomerDTO(null, "Maria", "Lopez", "1012345678",
                "3109998888", "maria@correo.com", "Carrera 7", null);
        Customer result = service.update(1L, cambios);

        assertEquals("Maria", result.getNombre());
        assertEquals("3109998888", result.getTelefono());
        assertEquals("maria@correo.com", result.getCorreo());
    }

    @Test
    void eliminarCliente_invocaDelete() {
        Customer existente = cliente(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        service.delete(1L);

        verify(repo).delete(existente);
    }
}
