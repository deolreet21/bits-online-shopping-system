# Flow 05: Cancel Order Flow

**End-to-End Trace: Customer clicks "Cancel" → PENDING check → status updated → email sent**

---

## ASCII Flow Diagram

```
Browser                 Controller           Service              Repository          DB
  │                         │                   │                    │               │
  │  POST /orders/{id}/cancel│                  │                    │               │
  │─────────────────────────>│                  │                    │               │
  │                   OrderController           │                    │               │
  │                   .cancelOrder()            │                    │               │
  │                         │                  │                    │               │
  │                Authorization check:        │                    │               │
  │                is order.user == session.user│                   │               │
  │                         │                  │                    │               │
  │                         │  cancelOrder(id) │                    │               │
  │                         │─────────────────>│                    │               │
  │                         │                  │  findById(id)──────>│               │
  │                         │                  │                    │──SELECT orders─>
  │                         │                  │                    │<──────────────│
  │                         │                  │                    │               │
  │                         │           status == PENDING?          │               │
  │                         │           (only PENDING can cancel)   │               │
  │                         │                  │                    │               │
  │                         │           order.setStatus(CANCELLED)  │               │
  │                         │           orderRepository.save()──────>│               │
  │                         │                  │                    │──UPDATE status─>
  │                         │                  │                    │               │
  │                         │   Extract: email, name, orderId       │               │
  │                         │   sendCancellationEmail() (ASYNC)─────────────────────>Gmail
  │                         │                  │                    │               │
  │  redirect:/orders (success flash)          │                    │               │
  │<─────────────────────────│                 │                    │               │
```

---

## Step-by-Step Walkthrough

### Step 1: Cancel Button in Template

**File:** `orders/history.html` or `orders/details.html`
```html
<form th:if="${order.status.name() == 'PENDING'}"
      th:action="@{/orders/{id}/cancel(id=${order.id})}" method="post">
    <button type="submit" class="btn btn-outline-danger btn-sm"
            onclick="return confirm('Cancel this order?')">
        Cancel Order
    </button>
</form>
```

The cancel button only appears for `PENDING` orders — `th:if` hides it for CONFIRMED, SHIPPED, etc.

`onclick="return confirm(...)"` — browser-level confirmation dialog. If user clicks Cancel on the dialog, `return false` stops the form submit.

### Step 2: Controller Authorization Check

**File:** `OrderController.java`
```java
@PostMapping("/orders/{id}/cancel")
public String cancelOrder(@PathVariable Long id, ...) {
    User user = (User) session.getAttribute("loggedInUser");

    Order order = orderService.getOrderById(id)
            .orElseThrow(() -> new RuntimeException("Order not found: " + id));

    // Authorization: customer can only cancel their OWN orders
    if (user.getRole() == UserRole.CUSTOMER &&
        !order.getUser().getId().equals(user.getId())) {
        return "redirect:/customer/dashboard";
    }
```

Without this check, any logged-in customer could cancel `POST /orders/1/cancel` for someone else's order. The check compares `order.getUser().getId()` (DB value) with `user.getId()` (session value).

### Step 3: Service — Status Check

**File:** `OrderService.java`
```java
public void cancelOrder(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new IllegalStateException(
            "Cannot cancel order with status: " + order.getStatus());
    }

    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);
```

**Why only PENDING can cancel?** Once an order is CONFIRMED, the warehouse may have started packing. Once SHIPPED, it's in transit — cancellation would require a return process. This business rule is enforced in code.

`IllegalStateException` propagates up to `OrderController`, which catches it:
```java
catch (IllegalStateException e) {
    redirectAttributes.addFlashAttribute("error", e.getMessage());
    return "redirect:/orders";
}
```

### Step 4: Email Notification

```java
// Extract before @Async (avoids LazyInitializationException)
String userEmail = order.getUser().getEmail();
String userName = order.getUser().getUsername();
Long orderId = order.getId();

emailNotificationService.sendCancellationEmail(userEmail, userName, orderId);
```

Same pattern as order confirmation: extract strings before the Hibernate session closes, then call the `@Async` method with plain values.

### Step 5: Redirect with Flash

```java
redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " has been cancelled.");
return "redirect:/orders";
```

User sees their order history. The cancelled order now shows the CANCELLED badge (red in `main.css`: `.status-cancelled { background-color: #dc3545; }`).

---

## What Happens if the Order is Not PENDING

```
POST /orders/42/cancel
  │
  OrderService.cancelOrder(42)
    → order.getStatus() == SHIPPED
    → throw IllegalStateException("Cannot cancel order with status: SHIPPED")
  │
  OrderController catches IllegalStateException
    → redirectAttributes.addFlashAttribute("error", "Cannot cancel order with status: SHIPPED")
    → redirect:/orders
  │
  orders/history.html shows red error alert
```

---

## Files Involved

| File | Role |
|------|------|
| `orders/history.html` | Cancel button (only for PENDING) |
| `orders/details.html` | Also has cancel button |
| `orders/cancel-confirm.html` | Optional confirmation page (GET before POST) |
| `OrderController.java` | Authorization check, exception catch |
| `OrderService.java` | PENDING guard, status update |
| `OrderRepository.java` | `save()` for status update |
| `EmailNotificationService.java` | `@Async` cancellation email |
