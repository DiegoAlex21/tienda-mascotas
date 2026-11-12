package jar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repo;

    @InjectMocks
    private ProductService service;

    private Product producto(Long id, String codigo) {
        return new Product(id, "Croquetas Premium", "Alimentos", "DogChow",
                "Bolsa de 10 kg", new BigDecimal("85000"), codigo, "ACTIVO");
    }

    private ProductDTO dto(String nombre, String codigo) {
        return new ProductDTO(null, nombre, "Alimentos", "DogChow",
                "Bolsa de 10 kg", new BigDecimal("85000"), codigo, "ACTIVO");
    }

    @Test
    void crearProducto_guardaCorrectamente() {
        when(repo.existsByCodigoBarras("123")).thenReturn(false);
        when(repo.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Product result = service.create(dto("Croquetas Premium", "123"));

        assertEquals(1L, result.getId());
        assertEquals("Croquetas Premium", result.getNombre());
        verify(repo).save(any(Product.class));
    }

    @Test
    void crearProducto_codigoBarrasDuplicado_lanza409() {
        when(repo.existsByCodigoBarras("123")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(dto("Croquetas Premium", "123")));

        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void consultarProductos_retornaLista() {
        when(repo.findAll()).thenReturn(List.of(producto(1L, "1"), producto(2L, "2")));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void consultarProducto_porId_retornaProducto() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto(1L, "123")));

        Product result = service.findById(1L);

        assertEquals("123", result.getCodigoBarras());
    }

    @Test
    void consultarProducto_noExiste_lanza404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findById(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void actualizarProducto_cambiaLosDatos() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto(1L, "123")));
        when(repo.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductDTO cambios = new ProductDTO(null, "Pelota Kong", "Juguetes", "Kong",
                "Pelota de goma", new BigDecimal("20000"), "123", "ACTIVO");
        Product result = service.update(1L, cambios);

        assertEquals("Pelota Kong", result.getNombre());
        assertEquals("Juguetes", result.getCategoria());
        assertEquals(new BigDecimal("20000"), result.getPrecio());
    }

    @Test
    void eliminarProducto_invocaDelete() {
        Product existente = producto(1L, "123");
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        service.delete(1L);

        verify(repo).delete(existente);
    }
}
