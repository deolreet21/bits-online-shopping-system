# OrderController.java

**File:** `src/main/java/com/shopping/system/controller/OrderController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring MVC Controller  
**Purpose:** Handles the complete order lifecycle from the HTTP perspective: placing an order from the cart, viewing order history (role-aware), viewing order details, confirming cancellation, and processing cancellation.

---

## Class-Level Annotations

```java
@Controller
@RequestMapping("/orders")
public class OrderController { ... }
```

All endpoints in this controller are prefixed with `/orders`.

---

## Dependency

```java
@Autowired private OrderService orderService;
```

---

## Endpoints

### `POST /orders/place` — Place Order

```java
@PostMapping("/place")
public String placeOrder(@RequestParam(required = false) String shippingAddress,
                         HttpSession session, RedirectAttributes redirectAttributes) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user == null) return "redirect:/login";

    if (shippingAddress == null || shippingAddress.isBlank()) {
        redirectAttributes.addFlashAttribute("error", "Shipping address is required.");
        return "redirect:/cart";
    }
    try {
        Order order = orderService.createOrderFromCart(user, shippingAddress);
        return "redirect:/orders/" + order.getId();
    } catch (IllegalStateException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/cart";
    }
}
```

**Flow:**
1. Validate user is logged in
2. Validate shipping address is provided (empty = back to cart with error)
3. Call `orderService.createOrderFromCart()` — does all business logic
4. On success: redirect to the new order's detail page
5. On `IllegalStateException` (empty cart, insufficient stock): flash error, back to cart

**`required = false` on shippingAddress:** The field might be missing entirely from the form (not just blank) — `required = false` prevents Spring from throwing an error before our validation runs.

---

### `GET /orders` — Order History

```java
@GetMapping
public String orderHistory(Model model, HttpSession session) {
    List<Order> orders;
    if (user.getRole() == UserRole.ADMIN) {
        orders = orderService.getAllOrders();    // all orders, all users
    } else {
        orders = orderService.getUserOrders(user.getId()); // only this user's orders
    }
    model.addAttribute("orders", orders);
    model.addAttribute("currentUser", user);
    return "orders/history";
}
```

**Role-aware behavior:** Same URL (`/orders`) serves both customers and admins. The template uses `${currentUser.role}` to conditionally show/hide the "Customer" column.

---

### `GET /orders/{id}` — Order Details

```java
@GetMapping("/{id}")
public String orderDetails(@PathVariable Long id, Model model, HttpSession session) {
    Order order = orderService.getOrderById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

    // Authorization: customers can only see their own orders
    if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
        throw new IllegalArgumentException("You are not authorized to view this order.");
    }

    model.addAttribute("order", order);
    model.addAttribute("canCancel", orderService.canCancelOrder(order));
    model.addAttribute("currentUser", user);
    return "orders/details";
}
```

**`@PathVariable Long id`** — Spring extracts the `{id}` value from the URL and converts it to `Long`.

**Authorization check:** Prevents a customer from accessing another customer's order by guessing the ID. Admins bypass this check.

**`canCancel`** — passed to the template so the "Cancel Order" button is only shown when the order is PENDING.

---

### `GET /orders/{id}/cancel` — Cancel Confirmation Page

Shows a confirmation form before actually cancelling. Validates:
1. Order exists
2. User is authorized to view it
3. Order is in PENDING status (redirects to details if not)

Returns `orders/cancel-confirm` template.

---

### `POST /orders/{id}/cancel` — Process Cancellation

```java
@PostMapping("/{id}/cancel")
public String cancelOrder(@PathVariable Long id, ...) {
    // Same auth checks
    try {
        orderService.cancelOrder(id);
        redirectAttributes.addFlashAttribute("success", "Order #" + id + " has been cancelled.");
    } catch (IllegalStateException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/orders/" + id;
}
```

After cancellation, redirects back to the order detail page where the updated CANCELLED status is shown.

---

## Template → Controller → Service → DB Flow for "Place Order"

```
cart/cart.html
  <form action="/orders/place" method="POST">
    <input name="shippingAddress" ...>
    <button>Place Order</button>
  </form>
          │
          ▼
POST /orders/place
  OrderController.placeOrder()
          │
          ▼
  orderService.createOrderFromCart(user, address)
          │
     @Transactional
          │
     ┌────┴────────────────────────────┐
     │ 1. cartService.getOrCreateCart  │
     │ 2. Validate cart not empty      │
     │ 3. For each cart item:          │
     │    - Check stock                │
     │    - Create OrderItem           │
     │    - Deduct product stock       │
     │ 4. Save Order (+cascade Items)  │
     │ 5. clearCart()                  │
     │ 6. sendOrderPlaced (async)      │
     └────────────────────────────────┘
          │
          ▼
  redirect:/orders/{newOrderId}
          │
          ▼
  orders/details.html (shows new order)
```
