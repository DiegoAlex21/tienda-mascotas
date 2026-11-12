package com.tienda.sales;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository repo;

    @Mock
    private ServiceClients clients;

    @InjectMocks
    private SaleService service;

    private Map<String, Object> inventario(long id, int stock) {
        return Map.of("id", id, "stockActual", stock);
    }

    private Sale ventaFacturada() {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setCustomerId(1L);
        sale.setTotal(new BigDecimal("170000"));
        sale.setEstado(Sale.Estado.FACTURADA);
        SaleDetail d = new SaleDetail();
        d.setSale(sale);
        d.setProductId(1L);
        d.setCantidad(2);
        d.setPrecioUnitario(new BigDecimal("85000"));
        d.setSubtotal(new BigDecimal("170000"));
        sale.getDetalles().add(d);
        return sale;
    }

    @Test
    void crearVenta_registraYActualizaElInventario() {
        when(clients.precioProducto(1L)).thenReturn(new BigDecimal("85000"));
        when(clients.inventarioDe(1L)).thenReturn(inventario(1L, 50));
        when(repo.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        SaleDTOs.Response result = service.create(new SaleDTOs.Create(1L,
                List.of(new SaleDTOs.Item(1L, 2))));

        assertEquals(Sale.Estado.FACTURADA, result.estado());
        assertEquals(1, result.detalles().size());
        verify(clients).validarCliente(1L);
        verify(clients).moverStock(1L, "SALIDA", 2);
    }

    @Test
    void crearVenta_calculaSubtotalesYTotal() {
        when(clients.precioProducto(1L)).thenReturn(new BigDecimal("85000"));
        when(clients.precioProducto(2L)).thenReturn(new BigDecimal("20000"));
        when(clients.inventarioDe(1L)).thenReturn(inventario(1L, 50));
        when(clients.inventarioDe(2L)).thenReturn(inventario(2L, 10));
        when(repo.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        SaleDTOs.Response result = service.create(new SaleDTOs.Create(1L,
                List.of(new SaleDTOs.Item(1L, 2), new SaleDTOs.Item(2L, 1))));

        assertEquals(0, new BigDecimal("190000").compareTo(result.total()));
        assertEquals(0, new BigDecimal("170000").compareTo(result.detalles().get(0).subtotal()));
        assertEquals(0, new BigDecimal("20000").compareTo(result.detalles().get(1).subtotal()));
        verify(clients, times(2)).moverStock(anyLong(), anyString(), anyInt());
    }

    @Test
    void crearVenta_sinStock_lanza400YNoGuarda() {
        when(clients.precioProducto(1L)).thenReturn(new BigDecimal("85000"));
        when(clients.inventarioDe(1L)).thenReturn(inventario(1L, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(new SaleDTOs.Create(1L,
                        List.of(new SaleDTOs.Item(1L, 5)))));

        assertEquals(400, ex.getStatusCode().value());
        verify(repo, never()).save(any());
        verify(clients, never()).moverStock(anyLong(), anyString(), anyInt());
    }

    @Test
    void crearVenta_clienteInexistente_lanza400YNoGuarda() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no existe"))
                .when(clients).validarCliente(999L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(new SaleDTOs.Create(999L,
                        List.of(new SaleDTOs.Item(1L, 2)))));

        assertEquals(400, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void cancelarVenta_devuelveElStockAlInventario() {
        when(repo.findById(1L)).thenReturn(Optional.of(ventaFacturada()));
        when(clients.inventarioDe(1L)).thenReturn(inventario(1L, 48));
        when(repo.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        SaleDTOs.Response result = service.updateEstado(1L,
                new SaleDTOs.UpdateEstado(Sale.Estado.CANCELADA));

        assertEquals(Sale.Estado.CANCELADA, result.estado());
        verify(clients).moverStock(1L, "ENTRADA", 2);
    }

    @Test
    void cancelarVenta_yaCancelada_lanza409() {
        Sale sale = ventaFacturada();
        sale.setEstado(Sale.Estado.CANCELADA);
        when(repo.findById(1L)).thenReturn(Optional.of(sale));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateEstado(1L, new SaleDTOs.UpdateEstado(Sale.Estado.CANCELADA)));

        assertEquals(409, ex.getStatusCode().value());
        verify(clients, never()).moverStock(anyLong(), anyString(), anyInt());
    }
}
