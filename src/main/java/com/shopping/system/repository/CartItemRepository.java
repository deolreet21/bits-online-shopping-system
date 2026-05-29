// Owner: Aliya | Shopping Cart | JpaRepository for cart item lookup by cart and product
package com.shopping.system.repository;

import com.shopping.system.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
