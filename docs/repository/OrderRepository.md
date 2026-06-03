# OrderRepository.java

**File:** `src/main/java/com/shopping/system/repository/OrderRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** HeenuReet  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** The most complex repository in the system. Provides order lookup, eager loading with JOIN FETCH (to avoid N+1 problem), date-range queries, and sales analysis aggregations used in the admin dashboard, reports, and sales analysis.

---

## Interface Declaration

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> { ... }
```

---

## Standard Derived Methods

```java
List<Order> findByUser(User user);
List<Order> findByUserId(Long userId);
List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
```

| Method | SQL | Used By |
|--------|-----|---------|
| `findByUser` | `WHERE user = ?` | Not used directly (superseded by JOIN FETCH version) |
| `findByUserId` | `WHERE user_id = ?` | Not used directly |
| `findByUserIdOrderByOrderDateDesc` | `WHERE user_id = ? ORDER BY order_date DESC` | Not used directly |

These are kept for flexibility but the system mostly uses the JOIN FETCH custom queries below.

---

## Custom @Query Methods — Detailed

### `findByIdWithDetails`
```java
@Query("SELECT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
Optional<Order> findByIdWithDetails(@Param("id") Long id);
```

**Purpose:** Load a single order with all related data in ONE SQL query.

**Why JOIN FETCH?**  
Without it, accessing `order.getUser().getUsername()` in a template would trigger a separate SQL query (N+1 problem). With JOIN FETCH, everything is loaded upfront.

**Breaking down the JPQL:**
| Clause | Meaning |
|--------|---------|
| `JOIN FETCH o.user` | INNER JOIN — order must have a user; loads User fields |
| `LEFT JOIN FETCH o.orderItems oi` | LEFT OUTER JOIN — loads OrderItems (none = empty list, not null) |
| `LEFT JOIN FETCH oi.product` | LEFT OUTER JOIN — loads Product for each OrderItem |

**Generated SQL equivalent:**
```sql
SELECT o.*, u.*, oi.*, p.*
FROM orders o
INNER JOIN users u ON o.user_id = u.id
LEFT JOIN order_items oi ON oi.order_id = o.id
LEFT JOIN products p ON oi.product_id = p.id
WHERE o.id = ?
```

**Used by:** `OrderService.getOrderById()` → `OrderController.orderDetails()`, `cancelConfirmPage()`

---

### `findUserOrdersWithItems`
```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
List<Order> findUserOrdersWithItems(@Param("userId") Long userId);
```

**Purpose:** Get all orders for a user, with items pre-loaded, newest first.

**Why DISTINCT?**  
When Hibernate does a JOIN with a one-to-many relationship, it returns one row per join. An order with 3 items produces 3 rows — without DISTINCT, we'd get 3 copies of the same order in the result list.

**Used by:** `OrderService.getUserOrders()` → `OrderController.orderHistory()`, `CustomerController.customerDashboard()`

---

### `findAllOrdersWithItems`
```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user LEFT JOIN FETCH o.orderItems ORDER BY o.orderDate DESC")
List<Order> findAllOrdersWithItems();
```

**Purpose:** Admin view — load ALL orders with user and items. Same JOIN FETCH and DISTINCT logic.

**Used by:** `OrderService.getAllOrders()` → `OrderController.orderHistory()` when user is ADMIN.

---

### `findOrdersBetweenDates`
```java
@Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
```

**Purpose:** Filter orders by date range for the sales report.  
**BETWEEN** includes both endpoints.  
**Used by:** `SalesAnalysisService.getOrdersBetween()` → `ReportController.salesReport()`

---

### `findTotalSalesBetweenDates`
```java
@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED")
BigDecimal findTotalSalesBetweenDates(...)
```

**Purpose:** Sum of revenue in a date range, excluding cancelled orders.

**Key clauses:**
| Clause | Reason |
|--------|--------|
| `COALESCE(SUM(...), 0)` | If no orders exist in the range, `SUM` returns NULL. `COALESCE` converts NULL to 0, preventing NullPointerException in the service. |
| `o.status != OrderStatus.CANCELLED` | Cancelled orders did not generate revenue |
| Full enum class path | JPQL doesn't auto-import enum types; fully-qualified name required |

**Used by:** `SalesAnalysisService` (weekly/monthly/quarterly/yearly sales methods)

---

### `findTopSellingProducts`
```java
@Query("SELECT oi.product, SUM(oi.quantity) as totalQty FROM OrderItem oi JOIN oi.order o WHERE o.status != com.shopping.system.entity.OrderStatus.CANCELLED GROUP BY oi.product ORDER BY totalQty DESC")
List<Object[]> findTopSellingProducts();
```

**Purpose:** Rank products by total units sold across all non-cancelled orders.

**Returns:** `List<Object[]>` — each array contains `[Product, Long]`.  
This is because JPQL cannot return a custom type in a single query; `Object[]` is the standard pattern.

**In `SalesAnalysisService.getFastMovingProducts()`:**
```java
Object[] row = results.get(i);
Product product = (Product) row[0];  // cast first element
Long qty = (Long) row[1];            // cast second element
```

**SQL equivalent:**
```sql
SELECT p.*, SUM(oi.quantity) as totalQty
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
WHERE o.status != 'CANCELLED'
GROUP BY p.id
ORDER BY totalQty DESC
```

---

### `countTodaysOrders` and `findTodaysTotalSales`
```java
@Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE AND o.status != ...")
long countTodaysOrders();

@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE AND o.status != ...")
BigDecimal findTodaysTotalSales();
```

**Key:** `DATE(o.orderDate) = CURRENT_DATE` — extracts just the date part from the datetime, ignoring time. `CURRENT_DATE` is a JPQL/SQL built-in for today's date.

**Used by:** `DashboardService.getTodaysSales()` → `AdminController` dashboard metric card.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `OrderService` | `findByIdWithDetails`, `findUserOrdersWithItems`, `findAllOrdersWithItems`, `save`, `findById`, `count` |
| `DashboardService` | `findTodaysTotalSales`, `count`, `findAll` |
| `SalesAnalysisService` | `findTotalSalesBetweenDates`, `findTopSellingProducts`, `findOrdersBetweenDates`, `findAll` |
