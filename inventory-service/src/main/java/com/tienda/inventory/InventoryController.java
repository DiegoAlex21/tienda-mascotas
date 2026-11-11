package com.tienda.inventory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping
    public List<InventoryDTOs.Response> all() {
        return service.findAll();
    }

    @GetMapping("/{productId}")
    public InventoryDTOs.Response byProduct(@PathVariable Long productId) {
        return service.findByProductId(productId);
    }

    @GetMapping("/{productId}/movements")
    public List<InventoryMovement> movements(@PathVariable Long productId) {
        return service.movimientos(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDTOs.Response create(@Valid @RequestBody InventoryDTOs.Create dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public InventoryDTOs.Response update(@PathVariable Long id,
                                         @Valid @RequestBody InventoryDTOs.Update dto) {
        return service.update(id, dto);
    }
}