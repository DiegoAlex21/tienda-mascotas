package com.tienda.sales;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService service;

    @GetMapping
    public List<SaleDTOs.Response> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SaleDTOs.Response one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDTOs.Response create(@Valid @RequestBody SaleDTOs.Create dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public SaleDTOs.Response update(@PathVariable Long id,
                                    @Valid @RequestBody SaleDTOs.UpdateEstado dto) {
        return service.updateEstado(id, dto);
    }
}
