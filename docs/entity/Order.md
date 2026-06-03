# Order.java

**File:** `src/main/java/com/shopping/system/entity/Order.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** JPA Entity  
**Purpose:** Represents a customer's placed order. Created from a cart snapshot when "Place Order" is submitted. Stores the shipping address, total, status lifecycle (PENDING → CONFIRMED → SHIPPED → DELIVERED), and line items. Once created, the cart is cleared.

---

## Class-Level Annotations

```java
@Entity
@Table(name = "orders")
```
`name = "orders"` is important — `order` is a reserved SQL keyword (used in `ORDER BY`). Without this, Hibernate might fail when trying to create the table.

---

## Fields

### `user`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```
One user can have many orders. FK is `user_id` in `orders` table. LAZY — user details are not loaded unless accessed. Controllers and services that need to display `order.user.username` must use JOIN FETCH queries (which is exactly what `OrderRepository.findByIdWithDetails()` does).

---

### `orderDate`
```java
@Column(name = "order_date")
private LocalDateTime orderDate;
```
Set by `@PrePersist` to `LocalDateTime.now()`. The timestamp when the order was placed.

---

### `totalAmount`
```java
@Column(name = "total_amount", precision = 10, scale = 2)
private BigDecimal totalAmount;
```
Sum of all `OrderItem.subtotal` values. Calculated in `OrderService.createOrderFromCart()` and set before saving. Stored redundantly in the DB for quick reporting (avoids summing line items every time).

---

### `status`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private OrderStatus status;
```
The order's current lifecycle stage. Stored as a string. Default set in `@PrePersist` to `PENDING`.

**Status Transitions:**
```
PENDING → CONFIRMED → SHIPPED → DELIVERED
    └──────────────────────────────→ CANCELLED (only from PENDING)
```

---

### `shippingAddress`
```java
@Column(name = "shipping_address", length = 255)
private String shippingAddress;
```
Captured at order time from the cart page form field. Stored with the order so the address cannot change after ordering.

---

### `orderItems`
```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems = new ArrayList<>();
```
Same pattern as `Cart.cartItems`. `cascade = CascadeType.ALL` means saving an Order automatically saves all its OrderItems. `orphanRemoval = true` means removing an item from this list deletes it from the DB.

---

## Lifecycle Method

```java
@PrePersist
public void prePersist() {
    this.orderDate = LocalDateTime.now();
    if (this.status == null) {
        this.status = OrderStatus.PENDING;
    }
}
```

Sets `orderDate` automatically. Also sets `status = PENDING` if not already set — this is a safety net in case `OrderService` forgets to set it (though `OrderService` always calls `order.setStatus(OrderStatus.PENDING)` explicitly too).

---

## Forward Linkage

| File | How |
|------|-----|
| `OrderRepository` | All order queries; JOIN FETCH queries load this entity with items |
| `OrderService` | Creates, cancels, updates status |
| `OrderController` | Handles HTTP for order flow |
| `DashboardService` | `getRecentOrders()` returns List<Order> |
| `SalesAnalysisService` | Iterates `order.getOrderItems()` for category sales |
| `ReportController` | Sales report queries |
| Templates | `orders/history.html`, `orders/details.html`, `orders/confirmation.html` |
| `EmailNotificationService` | Order details are passed (as strings) to email methods |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| user | user_id | BIGINT | FK → users.id, NOT NULL |
| orderDate | order_date | DATETIME | nullable (set by @PrePersist) |
| totalAmount | total_amount | DECIMAL(10,2) | nullable |
| status | status | VARCHAR(255) | NOT NULL |
| shippingAddress | shipping_address | VARCHAR(255) | nullable |
