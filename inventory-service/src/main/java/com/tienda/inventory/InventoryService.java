package com.tienda.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repo;
    private final InventoryMovementRepository movementRepo;
    private final ProductClient productClient;

    public List<InventoryDTOs.Response> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public InventoryDTOs.Response findByProductId(Long productId) {
        Inventory inv = repo.findByProductId(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay inventario para el producto " + productId));
        return toResponse(inv);
    }

    public List<InventoryMovement> movimientos(Long productId) {
        return movementRepo.findByProductIdOrderByFechaMovimientoDesc(productId);
    }

    @Transactional
    public InventoryDTOs.Response create(InventoryDTOs.Create dto) {
        productClient.validarProductoExiste(dto.productId());
        if (repo.existsByProductId(dto.productId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe inventario para ese producto");
        }
        Inventory inv = repo.save(new Inventory(null, dto.productId(), dto.stockActual(),
                dto.stockMinimo(), LocalDateTime.now()));
        if (dto.stockActual() > 0) {
            registrarMovimiento(dto.productId(), TipoMovimiento.ENTRADA, dto.stockActual());
        }
        return toResponse(inv);
    }

    @Transactional
    public InventoryDTOs.Response update(Long id, InventoryDTOs.Update dto) {
        Inventory inv = repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventario no encontrado"));

        int nuevoStock = switch (dto.tipoMovimiento()) {
            case ENTRADA -> inv.getStockActual() + dto.cantidad();
            case SALIDA -> inv.getStockActual() - dto.cantidad();
            case AJUSTE -> dto.cantidad();
        };
        if (nuevoStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El inventario no puede quedar negativo");
        }

        inv.setStockActual(nuevoStock);
        if (dto.stockMinimo() != null) {
            inv.setStockMinimo(dto.stockMinimo());
        }
        inv.setUltimaActualizacion(LocalDateTime.now());
        repo.save(inv);
        registrarMovimiento(inv.getProductId(), dto.tipoMovimiento(), dto.cantidad());
        return toResponse(inv);
    }

    private void registrarMovimiento(Long productId, TipoMovimiento tipo, Integer cantidad) {
        movementRepo.save(new InventoryMovement(null, productId, tipo, cantidad, LocalDateTime.now()));
    }

    private InventoryDTOs.Response toResponse(Inventory inv) {
        boolean alerta = inv.getStockActual() < inv.getStockMinimo();
        if (alerta) {
            log.warn("ALERTA: stock bajo para el producto {} (actual: {}, mínimo: {})",
                    inv.getProductId(), inv.getStockActual(), inv.getStockMinimo());
        }
        return new InventoryDTOs.Response(inv.getId(), inv.getProductId(), inv.getStockActual(),
                inv.getStockMinimo(), inv.getUltimaActualizacion(), alerta);
    }
}