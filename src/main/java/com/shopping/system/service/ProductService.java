// Owner: Aliya | Product Management | Service for product CRUD, search, and category operations
package com.shopping.system.service;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Product;
import com.shopping.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getById(Long id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public Product update(Long id, Product updatedProduct) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setQuantityOnHand(updatedProduct.getQuantityOnHand());
        existing.setCategory(updatedProduct.getCategory());
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> getByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    public List<Product> searchWithCategory(String keyword, Category category) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category);
    }

    public long getTotalProducts() {
        return productRepository.count();
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getQuantityOnHand() < threshold)
                .toList();
    }
}
