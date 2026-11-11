package com.tienda.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByCorreo(String correo);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    boolean existsByCorreoAndIdNot(String correo, Long id);
}