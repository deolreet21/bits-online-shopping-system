# Flow 09: Sales Analytics Flow

**End-to-End Trace: Admin opens sales dashboard → aggregated totals computed from orders → fast/slow products ranked → sales by category**

---

## ASCII Flow Diagram

```
Browser                  Controller              Service            Repository / DB
  │                          │                      │                      │
  │  GET /admin/sales         │                      │                      │
  │──────────────────────────>│                      │                      │
  │                    SalesController               │                      │
  │                    .salesDashboard()             │                      │
  │                          │  getWeeklySales()     │                      │
  │                          │─────────────────────>│                      │
  │                          │               SalesAnalysisService          │
  │                          │               now() - 7 days                │
  │                          │                      │  findTotalSalesBetweenDates(start, end)
  │                          │                      │──────────────────────>│
  │                          │                      │   SELECT COALESCE(SUM(total_amount),0)
  │                          │                      │   FROM orders WHERE order_date BETWEEN ?
  │                          │                      │   AND status != CANCELLED────────────>
  │                          │                      │                      │<──────────────
  │                          │  getMonthlySales()   │                      │
  │                          │  getQuarterlySales() │  (same query,        │
  │                          │  getYearlySales()    │   different window)  │
  │                          │─────────────────────>│──────────────────────>│
  │                          │                      │                      │
  │                          │  getFastMovingProducts()                    │
  │                          │─────────────────────>│                      │
  │                          │                      │  findTopSellingProducts()
  │                          │                      │──────────────────────>│
  │                          │                      │  SELECT oi.product, SUM(oi.quantity)
  │                          │                      │  FROM order_items oi JOIN orders o
  │                          │                      │  WHERE o.status != CANCELLED
  │                          │                      │  GROUP BY oi.product ORDER BY qty DESC
  │                          │                      │                      │<──────────────
  │                          │               take top 10                   │
  │                          │                      │                      │
  │                          │  getSlowMovingProducts()                    │
  │                          │─────────────────────>│                      │
  │                          │               same results, take bottom 10  │
  │                          │               + products with 0 orders      │
  │                          │                      │  findAll() (products)│
  │                          │                      │──────────────────────>│
  │                          │                      │                      │<──────────────
  │                          │  getSalesByCategory()│                      │
  │                          │─────────────────────>│                      │
  │                          │                      │  findAll() (orders + items + products)
  │                          │                      │──────────────────────>│
  │                          │                      │                      │<──────────────
  │                          │               group subtotals by category   │
  │  admin/sales/dashboard    │                      │                      │
  │<──────────────────────────│                      │                      │
```

---

## Step-by-Step Walkthrough

### Step 1: Controller Entry + Admin Guard

**File:** `SalesController.java`
```java
@GetMapping({"", "/"})
public String salesDashboard(Model model, HttpSession session) {
    if (!isAdmin(session)) return "redirect:/customer/dashboard";

    model.addAttribute("weeklySales",       salesAnalysisService.getWeeklySales());
    model.addAttribute("monthlySales",      salesAnalysisService.getMonthlySales());
    model.addAttribute("quarterlySales",    salesAnalysisService.getQuarterlySales());
    model.addAttribute("yearlySales",       salesAnalysisService.getYearlySales());
    model.addAttribute("fastMovingProducts", salesAnalysisService.getFastMovingProducts());
    model.addAttribute("slowMovingProducts", salesAnalysisService.getSlowMovingProducts());
    model.addAttribute("salesByCategory",   salesAnalysisService.getSalesByCategory());
    return "admin/sales/dashboard";
}
```

All attributes are computed in-line before the view renders — no lazy evaluation.

**Period-specific endpoints** (`/admin/sales/weekly`, `/monthly`, etc.) load only the relevant figure into the model and return the same `admin/sales/dashboard` view. The template uses `th:if` to show/hide sections based on which attributes are present.

---

### Step 2: Period Sales Totals — Shared DB Query

**File:** `SalesAnalysisService.java`
```java
public BigDecimal getWeeklySales() {
    LocalDateTime end   = LocalDateTime.now();
    LocalDateTime start = end.minusWeeks(1);
    return orderRepository.findTotalSalesBetweenDates(start, end);
}
```

All four period methods (`weekly`, `monthly`, `quarterly`, `yearly`) use the same pattern — compute a `start` date and call the same repository query:

**File:** `OrderRepository.java`
```java
@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
       "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
       "AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED")
BigDecimal findTotalSalesBetweenDates(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate")   LocalDateTime endDate);
```

**`COALESCE(..., 0)`** — if there are no orders in the range, `SUM` returns `NULL`. `COALESCE` converts it to `0` so the service always gets a valid `BigDecimal` (never `null`).

**`status != CANCELLED`** — cancelled orders are excluded from revenue totals. A cancelled order means the customer didn't pay / payment was reversed.

**`totalAmount`** is the pre-calculated sum stored on the Order at creation time (`OrderService` sums all `orderItem.subtotal` values and sets it). The query sums those stored totals — it does not re-aggregate from order items.

---

### Step 3: Fast-Moving Products

**File:** `SalesAnalysisService.java`
```java
public List<Map<String, Object>> getFastMovingProducts() {
    List<Object[]> results = orderRepository.findTopSellingProducts();
    List<Map<String, Object>> list = new ArrayList<>();
    int limit = Math.min(results.size(), 10);
    for (int i = 0; i < limit; i++) {
        Object[] row = results.get(i);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("product",   (Product) row[0]);
        entry.put("totalSold", (Long)    row[1]);
        list.add(entry);
    }
    return list;
}
```

**The underlying query:**
```java
@Query("SELECT oi.product, SUM(oi.quantity) as totalQty FROM OrderItem oi " +
       "JOIN oi.order o WHERE o.status != com.shopping.system.entity.OrderStatus.CANCELLED " +
       "GROUP BY oi.product ORDER BY totalQty DESC")
List<Object[]> findTopSellingProducts();
```

Returns rows of `[Product entity, Long quantity]` ordered highest-to-lowest. The service wraps each `Object[]` into a named map so the Thymeleaf template can use `entry['product']` and `entry['totalSold']` instead of array indices.

`Math.min(results.size(), 10)` — caps at top 10 without throwing if fewer than 10 products have been ordered.

---

### Step 4: Slow-Moving Products

**File:** `SalesAnalysisService.java`
```java
public List<Map<String, Object>> getSlowMovingProducts() {
    List<Object[]> results = orderRepository.findTopSellingProducts();
    // Take the bottom 10 (least sold, but ordered at least once)
    int size  = results.size();
    int start = Math.max(0, size - 10);
    for (int i = size - 1; i >= start; i--) { ... }

    // Also include products that have never been ordered
    List<Product> allProducts = productRepository.findAll();
    Set<Long> soldProductIds = new HashSet<>();
    for (Object[] row : results) {
        soldProductIds.add(((Product) row[0]).getId());
    }
    for (Product p : allProducts) {
        if (!soldProductIds.contains(p.getId()) && list.size() < 10) {
            entry.put("product",   p);
            entry.put("totalSold", 0L);
            list.add(entry);
        }
    }
    return list;
}
```

**Two sources of "slow" products:**
1. Products that appear in orders but at the bottom of the ranked list (low volume sellers)
2. Products that never appear in any order at all (`totalSold = 0`)

**Why reuse `findTopSellingProducts()`** instead of a separate `ORDER BY ASC` query? The same sorted list is already computed — taking the tail is cheaper than an extra query. Never-ordered products can't appear in that query at all (they have no order items to join), so a separate `findAll()` pass handles them.

`soldProductIds` set provides O(1) lookup to check if a product has ever sold.

---

### Step 5: Sales by Category

**File:** `SalesAnalysisService.java`
```java
public Map<String, BigDecimal> getSalesByCategory() {
    Map<String, BigDecimal> salesByCategory = new LinkedHashMap<>();
    for (Category category : Category.values()) {
        salesByCategory.put(category.getDisplayName(), BigDecimal.ZERO);
    }

    List<Order> allOrders = orderRepository.findAll();
    for (Order order : allOrders) {
        if (order.getStatus() == OrderStatus.CANCELLED) continue;
        for (OrderItem item : order.getOrderItems()) {
            String catName  = item.getProduct().getCategory().getDisplayName();
            BigDecimal current = salesByCategory.getOrDefault(catName, BigDecimal.ZERO);
            salesByCategory.put(catName, current.add(item.getSubtotal()));
        }
    }
    return salesByCategory;
}
```

**Pre-populate with zero for every category.** The map is initialized with all `Category` enum values set to `BigDecimal.ZERO`. This guarantees every category appears in the result — even ones with no sales. The template can render all categories in a consistent bar chart without null checks.

**Why in-memory aggregation here (not a DB query)?** JPA doesn't handle `GROUP BY` on enum string values cleanly across all databases with a single portable JPQL query. The trade-off is that `findAll()` loads all orders into memory. For the current dataset size this is fine; at scale, a native SQL query with `GROUP BY p.category` would be preferable.

**`item.getSubtotal()`** is the pre-calculated `price × quantity` stored on each `OrderItem` at order creation (via `@PrePersist`/`@PreUpdate`). This avoids re-multiplying at query time.

---

## Period Windows Reference

| Method | Window | `LocalDateTime` operation |
|--------|--------|--------------------------|
| `getWeeklySales()` | Last 7 days | `now().minusWeeks(1)` |
| `getMonthlySales()` | Last 30 days | `now().minusMonths(1)` |
| `getQuarterlySales()` | Last 90 days | `now().minusMonths(3)` |
| `getYearlySales()` | Last 365 days | `now().minusYears(1)` |

All windows are rolling (relative to "now"), not calendar-aligned (e.g., not "January 1 to today").

---

## Files Involved

| File | Role |
|------|------|
| `admin/sales/dashboard.html` | Sales dashboard template |
| `SalesController.java` | `GET /admin/sales` and period-specific routes |
| `SalesAnalysisService.java` | Period totals, fast/slow products, category aggregation |
| `OrderRepository.java` | `findTotalSalesBetweenDates()`, `findTopSellingProducts()` |
| `ProductRepository.java` | `findAll()` for never-ordered products |
| `Order.java` | `totalAmount`, `status`, `orderItems` |
| `OrderItem.java` | `subtotal`, `product` (for category lookup) |
| `Category.java` | Enum with `getDisplayName()` — drives category map keys |
