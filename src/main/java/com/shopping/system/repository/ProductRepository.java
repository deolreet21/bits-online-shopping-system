// Owner: Aliya | Product Management | JpaRepository for product search and category filtering
package com.shopping.system.repository;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(Category category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, Category category);
}
