package com.devops.productservice.service;

import com.devops.productservice.model.Product;
import com.devops.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public Product create(Product product) {
        log.info("Creating product: name={}, price={}, stock={}",
                product.getName(), product.getPrice(), product.getStock());
        Product saved = productRepository.save(product);
        log.info("Product created — id: {}", saved.getId());
        return saved;
    }

    public Product findById(Long id) {
        log.info("Fetching product by id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found — id: {}", id);
                    return new RuntimeException("Product not found: " + id);
                });
    }

    public List<Product> findAll() {
        log.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        log.info("Found {} products", products.size());
        return products;
    }

    @Transactional
    public Product reserveStock(Long productId, Integer quantity) {
        log.info("Reserving stock — productId: {}, quantity: {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Reserve failed — product not found: {}", productId);
                    return new RuntimeException("Product not found: " + productId);
                });

        if (product.getStock() < quantity) {
            log.warn("Reserve failed — insufficient stock for productId: {} (available: {}, requested: {})",
                    productId, product.getStock(), quantity);
            throw new RuntimeException("Insufficient stock. Available: " + product.getStock()
                    + ", Requested: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        Product updated = productRepository.save(product);
        log.info("Stock reserved — productId: {}, newStock: {}", productId, updated.getStock());
        return updated;
    }
}
