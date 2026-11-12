# Diagramas de arquitectura

Diagramas del sistema de gestión para tienda de mascotas, escritos en Mermaid. GitHub los dibuja automáticamente al abrir este archivo.

## 1. Arquitectura general

Cuatro microservicios Spring Boot, cada uno con su propia base de datos PostgreSQL, dentro de una red interna de Docker Compose. Las flechas entre servicios son llamadas REST.

```mermaid
flowchart TB
    U["Usuario<br/>Postman / Swagger UI"]

    subgraph NET["Docker Compose - red interna tienda-net"]
        subgraph MS["Microservicios Spring Boot"]
            CUST["customer-service<br/>puerto 8082"]
            PROD["product-service<br/>puerto 8081"]
            INV["inventory-service<br/>puerto 8083"]
            SALES["sales-service<br/>puerto 8084"]
        end

        subgraph DB["PostgreSQL 16 - una base por servicio"]
            DBC[("postgres-customers<br/>customers_db")]
            DBP[("postgres-products<br/>products_db")]
            DBI[("postgres-inventory<br/>inventory_db")]
            DBS[("postgres-sales<br/>sales_db")]
        end
    end

    U -->|HTTP| CUST
    U -->|HTTP| PROD
    U -->|HTTP| INV
    U -->|HTTP| SALES

    SALES -->|valida cliente| CUST
    SALES -->|consulta precio| PROD
    SALES -->|verifica y descuenta stock| INV
    INV -->|valida producto| PROD

    CUST --> DBC
    PROD --> DBP
    INV --> DBI
    SALES --> DBS
```

## 2. Secuencia de aprendizaje por fases

Orden en que se construyó el proyecto y los conceptos que introduce cada fase.

```mermaid
flowchart LR
    F1["Fase 1<br/>Product-Service<br/>API REST, CRUD, JPA, DTOs, validaciones"]
    F2["Fase 2<br/>Customer-Service<br/>Dominios separados, base independiente"]
    F3["Fase 3<br/>Inventory-Service<br/>Comunicación REST, consistencia de datos"]
    F4["Fase 4<br/>Sales-Service<br/>Integración, reglas de negocio, transacciones"]
    F5["Fase 5<br/>Docker y Compose<br/>Variables de entorno, pruebas, despliegue"]

    F1 --> F2 --> F3 --> F4 --> F5
```

## 3. Flujo de registro de una venta

Qué ocurre cuando se envía `POST /sales`. Si el cliente no existe, el producto no existe, no tiene inventario o falta stock, la venta se rechaza con 400 y no se guarda nada.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant S as sales-service
    participant C as customer-service
    participant P as product-service
    participant I as inventory-service
    participant D as sales_db

    U->>S: POST /sales con customerId e items
    S->>C: GET /customers/{id}
    C-->>S: 200 OK
    Note over S,C: si el cliente no existe responde 400

    loop por cada producto de la venta
        S->>P: GET /products/{id}
        P-->>S: producto con su precio
        S->>I: GET /inventory/{productId}
        I-->>S: stockActual
        Note over S,I: si el stock es menor a la cantidad responde 400
    end

    S->>D: guarda la venta y sus detalles como FACTURADA

    loop por cada producto de la venta
        S->>I: PUT /inventory/{id} con movimiento SALIDA
        I-->>S: 200 OK
    end

    S-->>U: 201 Created con el total calculado
```

## 4. Modelo de datos

Cada base de datos es independiente. Las líneas punteadas son referencias lógicas por identificador entre servicios (validadas con llamadas REST, no con llaves foráneas). La única llave foránea real es `sale_details.sale_id`.

```mermaid
erDiagram
    PRODUCTS {
        bigint id PK
        varchar nombre
        varchar categoria
        varchar marca
        varchar descripcion
        numeric precio
        varchar codigo_barras UK
        varchar estado
    }

    CUSTOMERS {
        bigint id PK
        varchar nombre
        varchar apellido
        varchar documento UK
        varchar telefono
        varchar correo UK
        varchar direccion
        date fecha_registro
    }

    INVENTORY {
        bigint id PK
        bigint product_id UK
        int stock_actual
        int stock_minimo
        timestamp ultima_actualizacion
    }

    INVENTORY_MOVEMENTS {
        bigint id PK
        bigint product_id
        varchar tipo_movimiento
        int cantidad
        timestamp fecha_movimiento
    }

    SALES {
        bigint id PK
        bigint customer_id
        timestamp fecha_venta
        numeric total
        varchar estado
    }

    SALE_DETAILS {
        bigint id PK
        bigint sale_id FK
        bigint product_id
        int cantidad
        numeric precio_unitario
        numeric subtotal
    }

    SALES ||--o{ SALE_DETAILS : contiene
    SALES }o..|| CUSTOMERS : "customer_id logica"
    SALE_DETAILS }o..|| PRODUCTS : "product_id logica"
    INVENTORY |o..|| PRODUCTS : "product_id logica"
    INVENTORY_MOVEMENTS }o..|| PRODUCTS : "product_id logica"
```
