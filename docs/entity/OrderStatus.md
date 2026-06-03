# OrderStatus.java

**File:** `src/main/java/com/shopping/system/entity/OrderStatus.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** Java Enum  
**Purpose:** Defines the five stages of an order's lifecycle. Used in `Order.status` and throughout the system to control what operations are allowed (e.g. only PENDING orders can be cancelled).

---

## Enum Values

```java
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
```

| Status | Meaning | Set By |
|--------|---------|--------|
| `PENDING` | Order just placed; awaiting admin action | `Order.@PrePersist` + `OrderService.createOrderFromCart()` |
| `CONFIRMED` | Admin has confirmed the order | `OrderService.updateOrderStatus()` |
| `SHIPPED` | Order dispatched | `OrderService.updateOrderStatus()` |
| `DELIVERED` | Customer received the order | `OrderService.updateOrderStatus()` |
| `CANCELLED` | Order was cancelled | `OrderService.cancelOrder()` |

---

## Status Transition Rules

```
PENDING ──→ CONFIRMED ──→ SHIPPED ──→ DELIVERED
   │
   └──→ CANCELLED   (only PENDING orders can be cancelled)
```

**Enforcement in `OrderService.cancelOrder()`:**
```java
if (order.getStatus() != OrderStatus.PENDING) {
    throw new IllegalStateException("Only PENDING orders can be cancelled.");
}
```

Once an order is CONFIRMED/SHIPPED/DELIVERED, cancellation is blocked. This prevents stock inconsistencies (the inventory was already decremented; allowing cancel of a shipped order would require complex reversal logic).

---

## How It Appears in the UI

In `orders/history.html` and `orders/details.html`, status badges are color-coded:

| Status | Bootstrap class | Color |
|--------|----------------|-------|
| PENDING | `badge bg-warning` | Yellow |
| CONFIRMED | `badge bg-info` | Blue |
| SHIPPED | `badge bg-primary` | Blue |
| DELIVERED | `badge bg-success` | Green |
| CANCELLED | `badge bg-danger` | Red |

---

## Used in JPQL Queries

```java
// Exclude cancelled orders from sales sums
AND o.status != com.shopping.system.entity.OrderStatus.CANCELLED
```

The full qualified class name is used in `@Query` strings because JPQL does not auto-import enum types.

---

## Email Notifications Triggered by Status Changes

In `OrderService.updateOrderStatus()`:
```java
case CONFIRMED -> emailNotificationService.sendOrderConfirmed(...)
case SHIPPED   -> emailNotificationService.sendOrderShipped(...)
case DELIVERED -> emailNotificationService.sendOrderDelivered(...)
```

---

## Forward Linkage

| File | Usage |
|------|-------|
| `Order.java` | Type of the `status` field |
| `OrderService` | Validates cancellability; sets status on creation and update |
| `OrderRepository` | Used in all `@Query` strings that exclude CANCELLED |
| `SalesAnalysisService` | `getSalesByCategory()` skips CANCELLED orders |
| `DashboardService` | `getTodaysSales()` calls repository query excluding CANCELLED |
| Templates | Status badges in order history and detail pages |
