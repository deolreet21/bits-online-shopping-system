// Owner: Mehwish | Database & Config | JpaRepository for product catalogue operations
package com.shopping.system.repository;

import com.shopping.system.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Used by admin dashboard low-stock alerts (threshold < 5)
    @Query("SELECT p FROM Product p WHERE p.quantityOnHand < 5")
    List<Product> findLowStockProducts();

    List<Product> findByCategory(String category);
}
