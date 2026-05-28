// Owner: Mehwish | Customer Dashboard | JpaRepository for order lookup and stats queries
package com.shopping.system.repository;

import com.shopping.system.entity.Order;
import com.shopping.system.entity.OrderStatus;
import com.shopping.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Used by customer dashboard — ordered desc so stream().limit(5) gets the 5 most recent
    List<Order> findByUserOrderByOrderDateDesc(User user);

    long countByUser(User user);

    // Used by admin dashboard for today's sales
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal sumTotalAmountByOrderDateBetween(LocalDateTime start, LocalDateTime end);
}
