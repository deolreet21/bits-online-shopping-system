# DashboardService.java

**File:** `src/main/java/com/shopping/system/service/DashboardService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** Mehwish  
**Type:** Spring Service  
**Purpose:** Aggregates metrics and data for the admin dashboard. Acts as a facade — pulls from multiple sources (ProductRepository, OrderRepository, UserService, FeedbackService) and presents a unified API to `AdminController`.

---

## Dependencies

```java
@Autowired private ProductRepository productRepository;
@Autowired private OrderRepository orderRepository;
@Autowired private UserService userService;
@Autowired private FeedbackService feedbackService;
```

Injects both repositories directly AND services — this is the aggregation layer.

---

## Methods

### `getTotalProducts` / `getTotalOrders` / `getTotalCustomers`
```java
public long getTotalProducts()   → productRepository.count()
public long getTotalOrders()     → orderRepository.count()
public long getTotalCustomers()  → userService.getTotalCustomers()
```
Count metrics displayed as stat cards on the admin dashboard.

---

### `getTodaysSales`
```java
public BigDecimal getTodaysSales() {
    BigDecimal result = orderRepository.findTodaysTotalSales();
    return result != null ? result : BigDecimal.ZERO;
}
```

Calls the custom `@Query` in `OrderRepository`. The null check is a safety net — `COALESCE` in the JPQL already returns 0, but defensive programming prevents NullPointerException.

---

### `getLowStockProducts`
```java
public List<Product> getLowStockProducts() {
    return productRepository.findAll().stream()
            .filter(p -> p.getQuantityOnHand() < 5)
            .toList();
}
```
Threshold hardcoded at 5. Products with fewer than 5 units appear in the dashboard's "Low Stock Alert" table.

---

### `getRecentOrders`
```java
public List<Order> getRecentOrders() {
    return orderRepository.findAll().stream()
            .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
            .limit(10)
            .toList();
}
```
Loads all orders, sorts by date descending, returns 10. At scale, this should use a paginated repository query — loading all orders to get 10 is inefficient. This is a known trade-off for simplicity in the project scope.

---

### `getRecentFeedback`
```java
public List<Feedback> getRecentFeedback() {
    List<Feedback> all = feedbackService.getAllFeedback(); // already sorted desc
    return all.stream().limit(5).toList();
}
```
Reuses `FeedbackService.getAllFeedback()` (already sorted) and limits to 5 for the dashboard widget.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `AdminController` | All methods — populates the admin dashboard model |
