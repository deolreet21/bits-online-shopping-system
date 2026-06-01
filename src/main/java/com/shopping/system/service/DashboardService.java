// Owner: Mehwish | Admin Dashboard | Service for admin dashboard metrics and summaries
package com.shopping.system.service;

import com.shopping.system.entity.Feedback;
import com.shopping.system.entity.Order;
import com.shopping.system.entity.Product;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackService feedbackService;

    public long getTotalProducts() {
        return productRepository.count();
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getTotalCustomers() {
        return userService.getTotalCustomers();
    }

    public BigDecimal getTodaysSales() {
        BigDecimal result = orderRepository.findTodaysTotalSales();
        return result != null ? result : BigDecimal.ZERO;
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getQuantityOnHand() < 5)
                .toList();
    }

    public List<Order> getRecentOrders() {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .limit(10)
                .toList();
    }

    public List<Feedback> getRecentFeedback() {
        List<Feedback> all = feedbackService.getAllFeedback();
        return all.stream().limit(5).toList();
    }
}
