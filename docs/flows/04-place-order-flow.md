# Flow 04: Place Order Flow

**End-to-End Trace: Customer clicks "Place Order" → order created → inventory deducted → cart cleared → email sent**

This is the most complex flow in the project — involves 5 services/repositories, a `@Transactional` atomic operation, and `@Async` email.

---

## ASCII Flow Diagram

```
Browser               Controller          Service (Transactional)     Repo/DB         Email
  │                       │                       │                     │               │
  │  POST /orders/place   │                       │                     │               │
  │  {shippingAddress}    │                       │                     │               │
  │──────────────────────>│                       │                     │               │
  │                 OrderController               │                     │               │
  │                 .placeOrder()                 │                     │               │
  │                       │  createOrderFromCart  │                     │               │
  │                       │──────────────────────>│                     │               │
  │                       │                 @Transactional begins        │               │
  │                       │                       │  findByUserId       │               │
  │                       │                       │  (get cart)─────────>│               │
  │                       │                       │                     │──SELECT cart──>│
  │                       │                       │                     │<──────────────│
  │                       │                       │                     │               │
  │                       │               For each CartItem:            │               │
  │                       │                       │  findById(productId)│               │
  │                       │                       │─────────────────────>│               │
  │                       │                       │          SELECT products WHERE id=? >│
  │                       │                       │                     │<──────────────│
  │                       │                       │                     │               │
  │                       │              Check stock available?         │               │
  │                       │              Deduct: product.qty -= ordered │               │
  │                       │              productRepository.save()───────>│               │
  │                       │                       │                     │──UPDATE────────>
  │                       │                       │                     │               │
  │                       │              Create Order + OrderItems      │               │
  │                       │              orderRepository.save()─────────>│               │
  │                       │                       │                     │──INSERT order─>│
  │                       │                       │                     │──INSERT items─>│
  │                       │                       │                     │               │
  │                       │              Clear cart items               │               │
  │                       │              cartService.clearCart()        │               │
  │                       │                       │  cart.cartItems.clear()─────────────>│
  │                       │                       │                     │──DELETE items─>│
  │                       │                 @Transactional commits       │               │
  │                       │                       │                     │               │
  │                       │              Extract strings for email      │               │
  │                       │              (before @Async call)           │               │
  │                       │              emailService.sendOrderConfirm() (ASYNC)────────>
  │                       │                       │                     │     Gmail SMTP│
  │                       │                       │                     │               │
  │  redirect:/orders/confirmation/{id}           │                     │               │
  │<──────────────────────│                       │                     │               │
```

---

## Step-by-Step Walkthrough

### Step 1: Cart Page Form

**Template:** `cart/cart.html`
```html
<form th:action="@{/orders/place}" method="post" id="checkoutForm">
    <textarea name="shippingAddress" required></textarea>
    <button type="submit">Place Order</button>
</form>
```

The only input is `shippingAddress`. All other order data comes from the cart (which is in the DB, not the browser).

### Step 2: Controller Entry

**File:** `OrderController.java`
```java
@PostMapping("/orders/place")
public String placeOrder(@RequestParam String shippingAddress, ...) {
    User user = (User) session.getAttribute("loggedInUser");
    Cart cart = cartService.getCartForUser(user.getId());

    if (cart == null || cart.getCartItems().isEmpty()) {
        redirectAttributes.addFlashAttribute("error", "Your cart is empty.");
        return "redirect:/cart";
    }
```

Guards: no null cart, no empty cart.

```java
    Order order = orderService.createOrderFromCart(user, cart, shippingAddress);
    return "redirect:/orders/confirmation/" + order.getId();
```

### Step 3: `@Transactional` — The Atomic Block

**File:** `OrderService.java`
```java
@Transactional
public Order createOrderFromCart(User user, Cart cart, String shippingAddress) {
```

`@Transactional` guarantees all-or-nothing: if any step throws an exception, all DB changes are rolled back. Without it, a failure after deducting inventory but before saving the order would leave inconsistent data.

**Step 3a: Create the Order shell**
```java
Order order = new Order();
order.setUser(user);
order.setShippingAddress(shippingAddress);
order.setOrderDate(LocalDateTime.now());
order.setStatus(OrderStatus.PENDING);   // @PrePersist also sets this
order.setTotalAmount(BigDecimal.ZERO);
```

**Step 3b: For each CartItem — validate stock and deduct**
```java
for (CartItem cartItem : cart.getCartItems()) {
    Product product = productRepository.findById(cartItem.getProduct().getId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

    if (product.getQuantityOnHand() < cartItem.getQuantity()) {
        throw new RuntimeException("Insufficient stock for: " + product.getName());
    }

    // Deduct inventory
    product.setQuantityOnHand(product.getQuantityOnHand() - cartItem.getQuantity());
    productRepository.save(product);
```

**Why re-fetch the product here** (instead of using `cartItem.getProduct()`)? The cart item's product reference is a lazy proxy — calling `getQuantityOnHand()` outside a transaction would cause `LazyInitializationException`. Re-fetching with `findById` loads a fresh, managed entity.

**Step 3c: Create OrderItem (snapshot)**
```java
    OrderItem orderItem = new OrderItem();
    orderItem.setOrder(order);
    orderItem.setProduct(product);
    orderItem.setQuantity(cartItem.getQuantity());
    orderItem.setPrice(cartItem.getPrice());       // price from cart (captured at add time)
    orderItem.setSubtotal(cartItem.getSubtotal()); // pre-calculated
    order.getOrderItems().add(orderItem);
    order.setTotalAmount(order.getTotalAmount().add(orderItem.getSubtotal()));
}
```

**Step 3d: Save the order (cascades to OrderItems)**
```java
order = orderRepository.save(order);
```

`Order` has `@OneToMany(cascade = CascadeType.ALL)` on `orderItems`. One `save()` inserts the Order row + all OrderItem rows.

**Step 3e: Clear the cart**
```java
cartService.clearCart(user.getId());
```

`CartService.clearCart()`:
```java
cart.getCartItems().clear();
cartRepository.save(cart);
```

`orphanRemoval = true` on `Cart.cartItems` means clearing the list + saving the cart triggers `DELETE FROM cart_items WHERE cart_id = ?`. The Cart row itself remains for future orders.

**Step 3f: Extract strings before `@Async` call**
```java
// CRITICAL: extract all values before @Transactional closes
String userEmail = order.getUser().getEmail();
String userName = order.getUser().getUsername();
Long orderId = order.getId();
BigDecimal total = order.getTotalAmount();

emailNotificationService.sendOrderConfirmation(userEmail, userName, orderId, total);
return order;
```

**Why extract?** `emailNotificationService.sendOrderConfirmation()` is `@Async` — it runs in a new thread after `@Transactional` commits. By that point, the Hibernate session is closed. If you passed the `order` entity directly and the async method tried to lazily load `order.getUser().getEmail()`, it would get `LazyInitializationException` (no active session in the async thread). Extracting primitive values/strings before the async call avoids this entirely.

### Step 4: `@Async` Email Sending

**File:** `EmailNotificationService.java`
```java
@Async
public void sendOrderConfirmation(String to, String name, Long orderId, BigDecimal total) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setSubject("Order Confirmed #" + orderId);
    msg.setText("Hi " + name + ", your order #" + orderId + " for ₹" + total + " is confirmed!");
    mailSender.send(msg);
}
```

`@Async` = runs in a Spring thread pool. The calling thread (handling the HTTP request) does not wait. The user gets the redirect in ~100ms; the email may arrive 1–2 seconds later.

### Step 5: Confirmation Page

`redirect:/orders/confirmation/42` → `GET /orders/confirmation/42`

**File:** `OrderController.java`
```java
@GetMapping("/orders/confirmation/{id}")
public String orderConfirmation(@PathVariable Long id, ...) {
    Order order = orderService.getOrderWithDetails(id);
    model.addAttribute("order", order);
    return "orders/confirmation";
}
```

`getOrderWithDetails()` uses `findByIdWithDetails()` — a JOIN FETCH query that loads user + orderItems + product in one SQL call (avoids N+1 problem on the confirmation page).

---

## What Gets Rolled Back if Something Fails

If `productRepository.save()` for the 2nd product fails (e.g., DB constraint), `@Transactional` rolls back:
- The order (not inserted)
- The first product's inventory deduction (reverted)
- All order items (not inserted)
- Cart items remain (user can retry)

This is correct behavior — the user sees an error and can try again.

---

## Files Involved

| File | Role |
|------|------|
| `cart/cart.html` | Checkout form with shippingAddress |
| `OrderController.java` | Cart validation, calls `createOrderFromCart`, redirect |
| `OrderService.java` | `@Transactional` atomic block |
| `CartService.java` | `clearCart()` with orphanRemoval |
| `ProductRepository.java` | Re-fetch for stock deduction |
| `OrderRepository.java` | `save()` with cascade to items |
| `EmailNotificationService.java` | `@Async` confirmation email |
| `orders/confirmation.html` | Success page with order details |
