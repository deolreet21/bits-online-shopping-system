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

    // Eagerly load user + orderItems + products for single-order detail / cancel pages
    @Query("SELECT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    // User order history — load user + items so templates can call .size() and .user.username
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findUserOrdersWithItems(@Param("userId") Long userId);

    // Admin: all orders with user + items
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems ORDER BY o.orderDate DESC")
    List<Order> findAllOrdersWithItems();

    // Eagerly load user + orderItems + products for single-order detail / cancel pages
    @Query("SELECT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    // User order history — load user + items so templates can call .size() and .user.username
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findUserOrdersWithItems(@Param("userId") Long userId);

    // Admin: all orders with user + items
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems ORDER BY o.orderDate DESC")
    List<Order> findAllOrdersWithItems();

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
