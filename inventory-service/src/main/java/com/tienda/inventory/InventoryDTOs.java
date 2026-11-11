package com.tienda.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public class InventoryDTOs {

    public record Create(
            @NotNull(message = "El productId es obligatorio") Long productId,
            @NotNull(message = "El stock actual es obligatorio")
            @PositiveOrZero(message = "El stock no puede ser negativo") Integer stockActual,
            @NotNull(message = "El stock mínimo es obligatorio")
            @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo
    ) {}

    public record Update(
            @NotNull(message = "El tipo de movimiento es obligatorio") TipoMovimiento tipoMovimiento,
            @NotNull(message = "La cantidad es obligatoria")
            @PositiveOrZero(message = "La cantidad no puede ser negativa") Integer cantidad,
            @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo
    ) {}

    public record Response(
            Long id,
            Long productId,
            Integer stockActual,
            Integer stockMinimo,
            LocalDateTime ultimaActualizacion,
            boolean alertaStockBajo
    ) {}
}