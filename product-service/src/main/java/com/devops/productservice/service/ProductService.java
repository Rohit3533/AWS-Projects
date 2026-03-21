package com.devops.productservice.service;

import com.devops.productservice.dto.product.ProductRequest;
import com.devops.productservice.model.Product;
import com.devops.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product create(Product product) {
        log.info("Creating product: name={}, price={}, stock={}",
                product.getName(), product.getPrice(), product.getStock());
        Product saved = productRepository.save(product);
        log.info("Product created — id: {}", saved.getId());
        return saved;
    }

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        log.info("Fetching product by id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found — id: {}", id);
                    return new RuntimeException("Product not found: " + id);
                });
    }

    @Cacheable(value = "products")
    public List<Product> findAll() {
        log.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        log.info("Found {} products", products.size());
        return products;
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product update(Long id, ProductRequest request) {
        log.info("Updating product — id: {}", id);
        Product product = findById(id);

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());

        Product updated = productRepository.save(product);
        log.info("Product updated — id: {}", updated.getId());
        return updated;
    }

    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting product — id: {}", id);
        Product product = findById(id);
        productRepository.delete(product);
        log.info("Product deleted — id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product reserveStock(Long productId, Integer quantity) {
        log.info("Reserving stock — productId: {}, quantity: {}", productId, quantity);

        Product product = findById(productId);

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStock()
                    + ", Requested: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        Product updated = productRepository.save(product);
        log.info("Stock reserved — productId: {}, newStock: {}", productId, updated.getStock());
        return updated;
    }
}
