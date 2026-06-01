// Owner: HeenuReet | Ordering | JpaRepository for order queries including sales analysis
package com.shopping.system.repository;

import com.shopping.system.entity.Order;
import com.shopping.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED")
    BigDecimal findTotalSalesBetweenDates(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT oi.product, SUM(oi.quantity) as totalQty FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.status != com.shopping.system.entity.OrderStatus.CANCELLED " +
           "GROUP BY oi.product ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts();

    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED")
    long countTodaysOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED")
    BigDecimal findTodaysTotalSales();
}
