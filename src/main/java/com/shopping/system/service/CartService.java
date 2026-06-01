// Owner: Aliya | Shopping Cart | Service for cart operations: add, remove, update, clear, total
package com.shopping.system.service;

import com.shopping.system.entity.Cart;
import com.shopping.system.entity.CartItem;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.CartRepository;
import com.shopping.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            Cart cart = new Cart(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addToCart(Long userId, Product product, int quantity) {
        Cart cart = getOrCreateCart(userId);
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart, product, quantity, product.getPrice());
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public Cart updateQuantity(Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        if (quantity <= 0) {
            Long cartId = item.getCart().getId();
            cartItemRepository.delete(item);
            return cartRepository.findById(cartId).orElseThrow();
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return cartRepository.findById(item.getCart().getId()).orElseThrow();
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    public BigDecimal getCartTotal(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotal)
                .orElse(BigDecimal.ZERO);
    }

    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(c -> c.getCartItems().size())
                .orElse(0);
    }
}
