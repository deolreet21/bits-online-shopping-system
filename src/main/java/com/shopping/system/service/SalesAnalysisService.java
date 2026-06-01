// Owner: Aliya | Sales Analysis | Service for weekly/monthly/quarterly/yearly sales and product movement
package com.shopping.system.service;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Order;
import com.shopping.system.entity.Product;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SalesAnalysisService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public BigDecimal getWeeklySales() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusWeeks(1);
        return orderRepository.findTotalSalesBetweenDates(start, end);
    }

    public BigDecimal getMonthlySales() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMonths(1);
        return orderRepository.findTotalSalesBetweenDates(start, end);
    }

    public BigDecimal getQuarterlySales() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMonths(3);
        return orderRepository.findTotalSalesBetweenDates(start, end);
    }

    public BigDecimal getYearlySales() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(1);
        return orderRepository.findTotalSalesBetweenDates(start, end);
    }

    public List<Map<String, Object>> getFastMovingProducts() {
        List<Object[]> results = orderRepository.findTopSellingProducts();
        List<Map<String, Object>> list = new ArrayList<>();
        int limit = Math.min(results.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            Product product = (Product) row[0];
            Long qty = (Long) row[1];
            entry.put("product", product);
            entry.put("totalSold", qty);
            list.add(entry);
        }
        return list;
    }

    public List<Map<String, Object>> getSlowMovingProducts() {
        List<Object[]> results = orderRepository.findTopSellingProducts();
        List<Map<String, Object>> list = new ArrayList<>();
        // Take the bottom 10 (least sold)
        int size = results.size();
        int start = Math.max(0, size - 10);
        for (int i = size - 1; i >= start; i--) {
            Object[] row = results.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            Product product = (Product) row[0];
            Long qty = (Long) row[1];
            entry.put("product", product);
            entry.put("totalSold", qty);
            list.add(entry);
        }
        // Also include products that have never been ordered
        List<Product> allProducts = productRepository.findAll();
        Set<Long> soldProductIds = new HashSet<>();
        for (Object[] row : results) {
            soldProductIds.add(((Product) row[0]).getId());
        }
        for (Product p : allProducts) {
            if (!soldProductIds.contains(p.getId()) && list.size() < 10) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("product", p);
                entry.put("totalSold", 0L);
                list.add(entry);
            }
        }
        return list;
    }

    public Map<String, BigDecimal> getSalesByCategory() {
        Map<String, BigDecimal> salesByCategory = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            salesByCategory.put(category.getDisplayName(), BigDecimal.ZERO);
        }

        List<Order> allOrders = orderRepository.findAll();
        for (Order order : allOrders) {
            if (order.getStatus() == com.shopping.system.entity.OrderStatus.CANCELLED) continue;
            for (com.shopping.system.entity.OrderItem item : order.getOrderItems()) {
                String catName = item.getProduct().getCategory().getDisplayName();
                BigDecimal current = salesByCategory.getOrDefault(catName, BigDecimal.ZERO);
                salesByCategory.put(catName, current.add(item.getSubtotal()));
            }
        }
        return salesByCategory;
    }

    public List<Order> getOrdersBetween(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findOrdersBetweenDates(start, end);
    }
}
