# SalesAnalysisService.java

**File:** `src/main/java/com/shopping/system/service/SalesAnalysisService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** Aliya  
**Type:** Spring Service  
**Purpose:** Business logic for all sales analytics. Provides revenue summaries (weekly/monthly/quarterly/yearly), product movement analysis (fast-moving and slow-moving products), and sales breakdown by category. Powers the admin sales dashboard and reports.

---

## Dependencies

```java
@Autowired private OrderRepository orderRepository;
@Autowired private ProductRepository productRepository;
```

---

## Methods

### Period Sales (Weekly/Monthly/Quarterly/Yearly)

```java
public BigDecimal getWeeklySales() {
    LocalDateTime end = LocalDateTime.now();
    LocalDateTime start = end.minusWeeks(1);
    return orderRepository.findTotalSalesBetweenDates(start, end);
}
```

Same pattern for all four periods:
| Method | Start Date |
|--------|-----------|
| `getWeeklySales()` | `now.minusWeeks(1)` |
| `getMonthlySales()` | `now.minusMonths(1)` |
| `getQuarterlySales()` | `now.minusMonths(3)` |
| `getYearlySales()` | `now.minusYears(1)` |

All call `orderRepository.findTotalSalesBetweenDates()` which excludes CANCELLED orders.

---

### `getFastMovingProducts`

```java
public List<Map<String, Object>> getFastMovingProducts() {
    List<Object[]> results = orderRepository.findTopSellingProducts();
    List<Map<String, Object>> list = new ArrayList<>();
    int limit = Math.min(results.size(), 10);
    for (int i = 0; i < limit; i++) {
        Object[] row = results.get(i);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("product", (Product) row[0]);
        entry.put("totalSold", (Long) row[1]);
        list.add(entry);
    }
    return list;
}
```

**Why `List<Map<String, Object>>`?**  
The repository query returns `Object[]` pairs (product, quantity). Rather than passing raw arrays to templates, this converts each pair into a named Map — `entry.get("product")` and `entry.get("totalSold")` — which is template-friendly: `${item.product.name}` and `${item.totalSold}`.

`LinkedHashMap` preserves insertion order (important — we want rank order maintained).

---

### `getSlowMovingProducts`

```java
public List<Map<String, Object>> getSlowMovingProducts() {
    List<Object[]> results = orderRepository.findTopSellingProducts(); // sorted DESC
    // Take bottom 10 (least sold)
    int size = results.size();
    int start = Math.max(0, size - 10);
    for (int i = size - 1; i >= start; i--) { ... }
    // Also include products with ZERO sales (never ordered)
    Set<Long> soldProductIds = new HashSet<>();
    for (Object[] row : results) soldProductIds.add(((Product) row[0]).getId());
    for (Product p : productRepository.findAll()) {
        if (!soldProductIds.contains(p.getId()) && list.size() < 10) {
            entry.put("totalSold", 0L);
            list.add(entry);
        }
    }
    return list;
}
```

**Two sources of "slow" products:**
1. Bottom 10 from the ordered sales list (least-sold products)
2. Products with ZERO sales (never appeared in an order)

A `HashSet` of sold product IDs allows O(1) lookup to identify which products have never been ordered.

---

### `getSalesByCategory`

```java
public Map<String, BigDecimal> getSalesByCategory() {
    Map<String, BigDecimal> salesByCategory = new LinkedHashMap<>();
    for (Category category : Category.values()) {
        salesByCategory.put(category.getDisplayName(), BigDecimal.ZERO); // initialize all to 0
    }
    List<Order> allOrders = orderRepository.findAll();
    for (Order order : allOrders) {
        if (order.getStatus() == OrderStatus.CANCELLED) continue;
        for (OrderItem item : order.getOrderItems()) {
            String catName = item.getProduct().getCategory().getDisplayName();
            BigDecimal current = salesByCategory.getOrDefault(catName, BigDecimal.ZERO);
            salesByCategory.put(catName, current.add(item.getSubtotal()));
        }
    }
    return salesByCategory;
}
```

**Why initialize all categories to 0 first?**  
If a category has no sales, it still appears in the result with 0 — the template shows all 6 categories, not just those with sales.

**Why not use a DB query?**  
A GROUP BY query on category would be more efficient, but this approach works correctly and is easier to reason about. Trade-off: at scale, this would need optimization.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `SalesController` | All methods — populates sales dashboard views |
| `ReportController` | `getOrdersBetween` for the date-range sales report |
