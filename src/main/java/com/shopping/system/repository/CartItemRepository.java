// Owner: Mehwish | Customer Dashboard | JpaRepository for cart item operations
package com.shopping.system.repository;

import com.shopping.system.entity.CartItem;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // th:text="${cartCount}" on the cart nav badge reads this count
    long countByUser(User user);

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
}
