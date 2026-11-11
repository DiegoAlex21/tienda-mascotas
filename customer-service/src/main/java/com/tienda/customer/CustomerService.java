package com.tienda.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repo;

    public List<Customer> findAll() {
        return repo.findAll();
    }

    public Customer findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    public Customer create(CustomerDTO dto) {
        if (repo.existsByDocumento(dto.documento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento duplicado");
        }
        if (dto.correo() != null && !dto.correo().isBlank() && repo.existsByCorreo(dto.correo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correo repetido");
        }
        Customer c = new Customer(null, dto.nombre(), dto.apellido(), dto.documento(),
                dto.telefono(), emptyToNull(dto.correo()), dto.direccion(), LocalDate.now());
        return repo.save(c);
    }

    public Customer update(Long id, CustomerDTO dto) {
        Customer c = findById(id);
        if (repo.existsByDocumentoAndIdNot(dto.documento(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento duplicado");
        }
        if (dto.correo() != null && !dto.correo().isBlank()
                && repo.existsByCorreoAndIdNot(dto.correo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correo repetido");
        }
        c.setNombre(dto.nombre());
        c.setApellido(dto.apellido());
        c.setDocumento(dto.documento());
        c.setTelefono(dto.telefono());
        c.setCorreo(emptyToNull(dto.correo()));
        c.setDireccion(dto.direccion());
        return repo.save(c);
    }

    public void delete(Long id) {
        repo.delete(findById(id));
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}