# Flow 06: Admin Update Order Status Flow

**End-to-End Trace: Admin changes order status → customer email notification sent**

---

## ASCII Flow Diagram

```
Browser (Admin)       Controller           Service              Repository         Email
  │                       │                   │                    │               │
  │  GET /orders           │                   │                   │               │
  │──────────────────────>│                   │                    │               │
  │                 OrderController           │                    │               │
  │                 .allOrders()              │                    │               │
  │                       │  findAllWithItems()│                   │               │
  │                       │──────────────────>│                    │               │
  │                       │                   │ findAllOrdersWith───>│               │
  │                       │                   │ Items()            │──SELECT DISTINCT orders
  │                       │                   │                    │  JOIN FETCH user,items─>
  │                       │                   │<───────────────────│               │
  │  orders/history.html  │                   │                    │               │
  │  (admin sees all orders, status dropdown) │                    │               │
  │<──────────────────────│                   │                    │               │
  │                       │                   │                    │               │
  │  POST /orders/{id}/status                 │                    │               │
  │  {status=CONFIRMED}   │                   │                    │               │
  │──────────────────────>│                   │                    │               │
  │                 Admin role check          │                    │               │
  │                       │ updateOrderStatus(id, CONFIRMED)       │               │
  │                       │──────────────────>│                    │               │
  │                       │                   │  findById──────────>│               │
  │                       │                   │                    │──SELECT───────>│
  │                       │                   │                    │<──────────────│
  │                       │                   │  order.setStatus(CONFIRMED)        │
  │                       │                   │  orderRepository.save()────────────>│
  │                       │                   │                    │──UPDATE────────>
  │                       │                   │                    │               │
  │                       │   Extract: email, name, orderId, status│               │
  │                       │   sendStatusUpdateEmail() (ASYNC)──────────────────────>Gmail
  │                       │                   │                    │               │
  │  redirect:/orders (flash: "Order #42 updated to CONFIRMED")    │               │
  │<──────────────────────│                   │                    │               │
```

---

## Step-by-Step Walkthrough

### Step 1: Admin Sees All Orders

**File:** `OrderController.java`
```java
@GetMapping("/orders")
public String viewOrders(Model model, HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");

    List<Order> orders;
    if (user.getRole() == UserRole.ADMIN) {
        orders = orderService.getAllOrdersWithItems();  // ALL orders
    } else {
        orders = orderService.getUserOrders(user.getId());  // only this user's orders
    }
    model.addAttribute("orders", orders);
    model.addAttribute("orderStatuses", OrderStatus.values());
```

Same endpoint (`GET /orders`) serves both roles, but returns different data.

**`getAllOrdersWithItems()`:**  
Calls `orderRepository.findAllOrdersWithItems()`:
```sql
SELECT DISTINCT o FROM Order o
JOIN FETCH o.user
JOIN FETCH o.orderItems oi
JOIN FETCH oi.product
ORDER BY o.orderDate DESC
```

`DISTINCT` — without it, a 3-item order would appear 3 times (one row per JOIN result).  
`JOIN FETCH` — loads all associations in one query (avoids N+1 — no separate queries per order/item).

### Step 2: Status Dropdown in Template

**File:** `orders/history.html`
```html
<!-- Admin-only status change form -->
<form th:if="${currentUser.role.name() == 'ADMIN'}"
      th:action="@{/orders/{id}/status(id=${order.id})}" method="post"
      class="d-flex gap-2">
    <select name="status" class="form-select form-select-sm">
        <option th:each="s : ${orderStatuses}"
                th:value="${s.name()}"
                th:text="${s.name()}"
                th:selected="${s == order.status}"></option>
    </select>
    <button type="submit" class="btn btn-sm btn-primary">Update</button>
</form>
```

`th:selected="${s == order.status}"` — pre-selects the current status. Admin sees the dropdown already on the current value.

`orderStatuses` = `OrderStatus.values()` = `[PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED]`

### Step 3: Controller Handles Status Change

**File:** `OrderController.java`
```java
@PostMapping("/orders/{id}/status")
public String updateOrderStatus(@PathVariable Long id,
                                 @RequestParam String status, ...) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";

    OrderStatus newStatus = OrderStatus.valueOf(status);
    orderService.updateOrderStatus(id, newStatus);
```

`OrderStatus.valueOf(status)` — converts the string `"CONFIRMED"` to the enum constant `OrderStatus.CONFIRMED`. Throws `IllegalArgumentException` if the string doesn't match any enum value.

### Step 4: Service — Status Update with Email Trigger

**File:** `OrderService.java`
```java
public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    order.setStatus(newStatus);
    order = orderRepository.save(order);

    // Extract before @Async
    String userEmail = order.getUser().getEmail();
    String userName = order.getUser().getUsername();

    switch (newStatus) {
        case CONFIRMED  -> emailNotificationService.sendOrderConfirmation(userEmail, userName, orderId, order.getTotalAmount());
        case SHIPPED    -> emailNotificationService.sendShippingNotification(userEmail, userName, orderId);
        case DELIVERED  -> emailNotificationService.sendDeliveryConfirmation(userEmail, userName, orderId);
        case CANCELLED  -> emailNotificationService.sendCancellationEmail(userEmail, userName, orderId);
        default -> {}   // PENDING: no email
    }
}
```

**`switch` expression (Java 14+):** Arrow cases (`->`) don't fall through and don't need `break`. Each status triggers a different email type.

**Why no email for PENDING?** PENDING is the initial status set when an order is created. The `sendOrderConfirmation` email is sent from `createOrderFromCart`, not from here. If admin manually sets back to PENDING (unusual), no email is sent.

---

## Email Types by Status

| Status Changed To | Email Subject |
|-------------------|---------------|
| CONFIRMED | "Your Order #42 is Confirmed!" |
| SHIPPED | "Your Order #42 Has Been Shipped!" |
| DELIVERED | "Your Order #42 Has Been Delivered!" |
| CANCELLED | "Your Order #42 Has Been Cancelled" |
| PENDING | No email |

---

## Files Involved

| File | Role |
|------|------|
| `orders/history.html` | Admin status dropdown form |
| `OrderController.java` | Admin role check, status update |
| `OrderService.java` | `updateOrderStatus()`, switch + email trigger |
| `OrderRepository.java` | `findById()`, `save()` |
| `EmailNotificationService.java` | All status-specific `@Async` email methods |
