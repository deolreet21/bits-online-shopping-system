// Owner: HeenuReet | Ordering | Service for creating orders from cart, cancelling, and history
package com.shopping.system.service;

import com.shopping.system.entity.*;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Order createOrderFromCart(User user, String shippingAddress) {
        Cart cart = cartService.getOrCreateCart(user.getId());
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place order.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();

            if (product.getQuantityOnHand() < qty) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem(order, product, qty, cartItem.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(cartItem.getSubtotal());

            // Deduct inventory
            product.setQuantityOnHand(product.getQuantityOnHand() - qty);
            productRepository.save(product);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Clear the cart after order placement
        cartService.clearCart(user.getId());
        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled.");
        }

        // Restore inventory
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setQuantityOnHand(product.getQuantityOnHand() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public boolean canCancelOrder(Order order) {
        return order.getStatus() == OrderStatus.PENDING;
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
