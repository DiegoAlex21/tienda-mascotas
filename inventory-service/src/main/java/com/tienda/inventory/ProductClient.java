package com.tienda.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductClient {

    private final RestClient client;

    public ProductClient(@Value("${product.service.url}") String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    public void validarProductoExiste(Long productId) {
        try {
            client.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto " + productId + " no existe");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Product-Service no está disponible");
        }
    }
}