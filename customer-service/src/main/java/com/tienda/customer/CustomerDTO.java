package com.tienda.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CustomerDTO(
        Long id,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String apellido,
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @Email(message = "El correo no tiene un formato válido") String correo,
        String direccion,
        LocalDate fechaRegistro
) {}