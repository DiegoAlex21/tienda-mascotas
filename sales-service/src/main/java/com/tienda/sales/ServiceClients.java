package com.tienda.sales;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ServiceClients {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient customers;
    private final RestClient products;
    private final RestClient inventory;

    public ServiceClients(@Value("${customer.service.url}") String customerUrl,
                          @Value("${product.service.url}") String productUrl,
                          @Value("${inventory.service.url}") String inventoryUrl) {
        this.customers = RestClient.create(customerUrl);
        this.products = RestClient.create(productUrl);
        this.inventory = RestClient.create(inventoryUrl);
    }

    public void validarCliente(Long id) {
        try {
            customers.get().uri("/customers/{id}", id).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw bad("El cliente " + id + " no existe");
        } catch (ResourceAccessException e) {
            throw unavailable("Customer-Service");
        }
    }

    public BigDecimal precioProducto(Long id) {
        try {
            Map<String, Object> p = products.get().uri("/products/{id}", id)
                    .retrieve().body(MAP_TYPE);
            return new BigDecimal(String.valueOf(p.get("precio")));
        } catch (HttpClientErrorException.NotFound e) {
            throw bad("El producto " + id + " no existe");
        } catch (ResourceAccessException e) {
            throw unavailable("Product-Service");
        }
    }

    public Map<String, Object> inventarioDe(Long productId) {
        try {
            return inventory.get().uri("/inventory/{productId}", productId)
                    .retrieve().body(MAP_TYPE);
        } catch (HttpClientErrorException.NotFound e) {
            throw bad("El producto " + productId + " no tiene inventario");
        } catch (ResourceAccessException e) {
            throw unavailable("Inventory-Service");
        }
    }

    public void moverStock(Long inventoryId, String tipo, int cantidad) {
        try {
            inventory.put().uri("/inventory/{id}", inventoryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("tipoMovimiento", tipo, "cantidad", cantidad))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw bad("No se pudo actualizar el inventario");
        } catch (ResourceAccessException e) {
            throw unavailable("Inventory-Service");
        }
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException unavailable(String servicio) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                servicio + " no esta disponible");
    }
}
