// Owner: Aliya | Shopping Cart | JpaRepository for cart lookup by user
package com.shopping.system.repository;

import com.shopping.system.entity.Cart;
import com.shopping.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);
}
