package com.tienda.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository repo;

    @Mock
    private InventoryMovementRepository movementRepo;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private InventoryService service;

    private Inventory inventario(int stock, int minimo) {
        return new Inventory(1L, 1L, stock, minimo, LocalDateTime.now());
    }

    @Test
    void registrarExistencias_creaInventarioYMovimientoDeEntrada() {
        when(repo.save(any(Inventory.class))).thenAnswer(i -> {
            Inventory inv = i.getArgument(0);
            inv.setId(1L);
            return inv;
        });

        InventoryDTOs.Response result = service.create(new InventoryDTOs.Create(1L, 20, 5));

        assertEquals(20, result.stockActual());
        assertFalse(result.alertaStockBajo());
        verify(productClient).validarProductoExiste(1L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepo).save(captor.capture());
        assertEquals(TipoMovimiento.ENTRADA, captor.getValue().getTipoMovimiento());
        assertEquals(20, captor.getValue().getCantidad());
    }

    @Test
    void registrarExistencias_productoInexistente_noGuarda() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no existe"))
                .when(productClient).validarProductoExiste(999L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(new InventoryDTOs.Create(999L, 20, 5)));

        assertEquals(400, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void registrarExistencias_inventarioDuplicado_lanza409() {
        when(repo.existsByProductId(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(new InventoryDTOs.Create(1L, 20, 5)));

        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void actualizarStock_entradaSumaLaCantidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(inventario(20, 5)));

        InventoryDTOs.Response result = service.update(1L,
                new InventoryDTOs.Update(TipoMovimiento.ENTRADA, 30, null));

        assertEquals(50, result.stockActual());
        assertFalse(result.alertaStockBajo());
    }

    @Test
    void actualizarStock_salidaRegistraMovimientoYGeneraAlerta() {
        when(repo.findById(1L)).thenReturn(Optional.of(inventario(20, 5)));

        InventoryDTOs.Response result = service.update(1L,
                new InventoryDTOs.Update(TipoMovimiento.SALIDA, 18, null));

        assertEquals(2, result.stockActual());
        assertTrue(result.alertaStockBajo());

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepo).save(captor.capture());
        assertEquals(TipoMovimiento.SALIDA, captor.getValue().getTipoMovimiento());
        assertEquals(18, captor.getValue().getCantidad());
    }

    @Test
    void actualizarStock_noPermiteInventarioNegativo() {
        when(repo.findById(1L)).thenReturn(Optional.of(inventario(20, 5)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, new InventoryDTOs.Update(TipoMovimiento.SALIDA, 100, null)));

        assertEquals(400, ex.getStatusCode().value());
        verify(movementRepo, never()).save(any());
    }
}
