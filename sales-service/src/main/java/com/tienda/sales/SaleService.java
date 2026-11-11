package com.tienda.sales;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository repo;
    private final ServiceClients clients;

    private record Linea(Long productId, int cantidad, Long inventoryId) {}

    public List<SaleDTOs.Response> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public SaleDTOs.Response findById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SaleDTOs.Response create(SaleDTOs.Create dto) {
        clients.validarCliente(dto.customerId());

        Sale sale = new Sale();
        sale.setCustomerId(dto.customerId());
        sale.setFechaVenta(LocalDateTime.now());
        sale.setEstado(Sale.Estado.FACTURADA);

        BigDecimal total = BigDecimal.ZERO;
        List<Linea> lineas = new ArrayList<>();

        for (SaleDTOs.Item item : dto.items()) {
            BigDecimal precio = clients.precioProducto(item.productId());
            Map<String, Object> inv = clients.inventarioDe(item.productId());
            int stock = ((Number) inv.get("stockActual")).intValue();
            if (stock < item.cantidad()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el producto " + item.productId()
                                + " (disponible: " + stock + ")");
            }
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad()));

            SaleDetail d = new SaleDetail();
            d.setSale(sale);
            d.setProductId(item.productId());
            d.setCantidad(item.cantidad());
            d.setPrecioUnitario(precio);
            d.setSubtotal(subtotal);
            sale.getDetalles().add(d);

            total = total.add(subtotal);
            lineas.add(new Linea(item.productId(), item.cantidad(),
                    ((Number) inv.get("id")).longValue()));
        }

        sale.setTotal(total);
        Sale saved = repo.save(sale);

        for (Linea l : lineas) {
            clients.moverStock(l.inventoryId(), "SALIDA", l.cantidad());
        }
        return toResponse(saved);
    }

    @Transactional
    public SaleDTOs.Response updateEstado(Long id, SaleDTOs.UpdateEstado dto) {
        Sale sale = find(id);
        if (dto.estado() != Sale.Estado.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se permite cambiar el estado a CANCELADA");
        }
        if (sale.getEstado() == Sale.Estado.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La venta ya esta cancelada");
        }
        for (SaleDetail d : sale.getDetalles()) {
            Map<String, Object> inv = clients.inventarioDe(d.getProductId());
            clients.moverStock(((Number) inv.get("id")).longValue(), "ENTRADA", d.getCantidad());
        }
        sale.setEstado(Sale.Estado.CANCELADA);
        return toResponse(repo.save(sale));
    }

    private Sale find(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    private SaleDTOs.Response toResponse(Sale s) {
        List<SaleDTOs.DetailResponse> detalles = s.getDetalles().stream()
                .map(d -> new SaleDTOs.DetailResponse(d.getId(), d.getProductId(),
                        d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
        return new SaleDTOs.Response(s.getId(), s.getCustomerId(), s.getFechaVenta(),
                s.getTotal(), s.getEstado(), detalles);
    }
}
