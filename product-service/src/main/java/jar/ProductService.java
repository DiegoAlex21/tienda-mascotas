package jar;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;

    public List<Product> findAll() {
        return repo.findAll();
    }

    public Product findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    public Product create(ProductDTO dto) {
        if (repo.existsByCodigoBarras(dto.codigoBarras())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de barras duplicado");
        }
        Product p = new Product(null, dto.nombre(), dto.categoria(), dto.marca(),
                dto.descripcion(), dto.precio(), dto.codigoBarras(), dto.estado());
        return repo.save(p);
    }

    public Product update(Long id, ProductDTO dto) {
        Product p = findById(id);
        p.setNombre(dto.nombre());
        p.setCategoria(dto.categoria());
        p.setMarca(dto.marca());
        p.setDescripcion(dto.descripcion());
        p.setPrecio(dto.precio());
        p.setEstado(dto.estado());
        return repo.save(p);
    }

    public void delete(Long id) {
        repo.delete(findById(id));
    }
}