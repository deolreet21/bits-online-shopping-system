# OrderItemRepository.java

**File:** `src/main/java/com/shopping/system/repository/OrderItemRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** HeenuReet  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `OrderItem` entities. Minimal usage — most order item access happens via `order.getOrderItems()` through the `@OneToMany` relationship on `Order`, not through this repository directly.

---

## Interface Declaration

```java
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> { ... }
```

---

## Custom Method

### `findByOrderId`
```java
List<OrderItem> findByOrderId(Long orderId);
```
**Generated SQL:** `SELECT * FROM order_items WHERE order_id = ?`  
Returns all line items for a given order.

**Why it's rarely called directly:**  
The `OrderRepository.findByIdWithDetails()` query uses `JOIN FETCH o.orderItems` to load items alongside the order in a single query. This repository method would cause a separate DB round-trip. It's here as a utility for any code that only has an order ID and needs items without the full Order object.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `OrderService` (indirect) | Items accessed via `order.getOrderItems()` not this repository |
| Available as utility | `findByOrderId` available if needed in future |
