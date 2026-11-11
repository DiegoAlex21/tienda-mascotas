package com.tienda.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SaleDTOs {

    public record Item(
            @NotNull(message = "El productId es obligatorio") Long productId,
            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad
    ) {}

    public record Create(
            @NotNull(message = "El customerId es obligatorio") Long customerId,
            @NotEmpty(message = "La venta debe tener al menos un producto") @Valid List<Item> items
    ) {}

    public record UpdateEstado(
            @NotNull(message = "El estado es obligatorio") Sale.Estado estado
    ) {}

    public record DetailResponse(
            Long id, Long productId, Integer cantidad,
            BigDecimal precioUnitario, BigDecimal subtotal
    ) {}

    public record Response(
            Long id, Long customerId, LocalDateTime fechaVenta,
            BigDecimal total, Sale.Estado estado, List<DetailResponse> detalles
    ) {}
}
