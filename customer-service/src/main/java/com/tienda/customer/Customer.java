package com.tienda.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String apellido;

    @Column(unique = true, nullable = false)
    private String documento;

    @Column(nullable = false)
    private String telefono;

    @Column(unique = true)
    private String correo;

    private String direccion;

    private LocalDate fechaRegistro;
}