package jar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La categoría es obligatoria") String categoria,
        String marca,
        String descripcion,
        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo") BigDecimal precio,
        @NotBlank(message = "El código de barras es obligatorio") String codigoBarras,
        String estado
) {}