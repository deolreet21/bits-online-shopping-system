# OrderService.java

**File:** `src/main/java/com/shopping/system/service/OrderService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** HeenuReet  
**Type:** Spring Service  
**Purpose:** Core business logic for the ordering process. Creates orders from a user's cart (with stock validation and inventory deduction), cancels PENDING orders (with inventory restoration), provides order history, and triggers email notifications.

---

## Class-Level Annotation

```java
@Service
public class OrderService { ... }
```

---

## Dependencies

```java
@Autowired private OrderRepository orderRepository;
@Autowired private CartService cartService;
@Autowired private ProductRepository productRepository;
@Autowired private EmailNotificationService emailNotificationService;
```

| Dependency | Why Needed |
|------------|-----------|
| `OrderRepository` | Save and retrieve orders |
| `CartService` | Load cart contents and clear after order |
| `ProductRepository` | Deduct/restore inventory |
| `EmailNotificationService` | Send async order emails |

---

## Methods

### `createOrderFromCart` ← Most Important Method

```java
@Transactional
public Order createOrderFromCart(User user, String shippingAddress) {
```

**`@Transactional` — Critical:** This method touches multiple DB tables (products, orders, order_items, carts). If any step fails, the entire operation rolls back:
- If stock deduction fails midway → order not created, previous stock changes undone
- If saving the order fails → inventory changes undone
- This prevents partial state like "inventory deducted but no order created"

**Step-by-step flow:**
```
1. Load cart from DB
2. Throw if cart is empty
3. Create new Order object (status = PENDING)
4. For each CartItem:
   a. Get product + quantity
   b. Check product.quantityOnHand >= qty → throw if not
   c. Create OrderItem(order, product, qty, cartItem.getPrice())
   d. Add to order.orderItems list
   e. Deduct stock: product.setQuantityOnHand(current - qty)
   f. Save product with new stock
5. Set order.totalAmount = sum of all CartItem subtotals
6. Save order (cascade saves all OrderItems automatically)
7. Clear the cart
8. Send "order placed" email (async, non-blocking)
9. Return saved order
```

**Key decision — use `cartItem.getPrice()` not `product.getPrice()`:**  
If admin changed the price between cart add and checkout, the order captures the cart price (what the customer saw), not the new price. Fairness principle.

**Email call after save:**
```java
emailNotificationService.sendOrderPlaced(
    savedOrder.getUser().getEmail(),     // extracted before @Async fires
    savedOrder.getUser().getUsername(),  // extracted before @Async fires
    savedOrder.getId(),
    savedOrder.getTotalAmount().toString());
```

User email/username are extracted as plain `String` values **before** calling the async method. This is critical — if we passed `savedOrder` to an `@Async` method, Hibernate's session would be closed by the time the async thread runs, causing `LazyInitializationException` on any lazy-loaded field access.

---

### `cancelOrder`

```java
@Transactional
public Order cancelOrder(Long orderId) {
    Order order = orderRepository.findById(orderId)...;
    if (order.getStatus() != OrderStatus.PENDING) {
        throw new IllegalStateException("Only PENDING orders can be cancelled.");
    }
    // Restore inventory
    for (OrderItem item : order.getOrderItems()) {
        Product product = item.getProduct();
        product.setQuantityOnHand(product.getQuantityOnHand() + item.getQuantity());
        productRepository.save(product);
    }
    order.setStatus(OrderStatus.CANCELLED);
    Order saved = orderRepository.save(order);
    emailNotificationService.sendOrderCancelled(
        saved.getUser().getEmail(), saved.getUser().getUsername(), saved.getId());
    return saved;
}
```

**Flow:**
1. Load order
2. Validate status == PENDING (only cancellable state)
3. For each order item: restore stock to product
4. Set status to CANCELLED
5. Save order
6. Send cancellation email (async)

**Why only PENDING can be cancelled?**  
If order is SHIPPED, the goods are already in transit — restoring stock would be incorrect. The business rule is enforced at service level.

---

### `updateOrderStatus`

```java
@Transactional
public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
    Order order = orderRepository.findById(orderId)...;
    order.setStatus(newStatus);
    Order saved = orderRepository.save(order);
    switch (newStatus) {
        case CONFIRMED  -> emailNotificationService.sendOrderConfirmed(...);
        case SHIPPED    -> emailNotificationService.sendOrderShipped(...);
        case DELIVERED  -> emailNotificationService.sendOrderDelivered(...);
        default -> {}
    }
    return saved;
}
```

Used by admin from the order detail page. The switch expression triggers the appropriate email for each status transition.

---

### Other Methods

```java
public List<Order> getUserOrders(Long userId)  → orderRepository.findUserOrdersWithItems(userId)
public Optional<Order> getOrderById(Long id)   → orderRepository.findByIdWithDetails(id)
public boolean canCancelOrder(Order order)      → order.getStatus() == OrderStatus.PENDING
public long getTotalOrders()                    → orderRepository.count()
public List<Order> getAllOrders()               → orderRepository.findAllOrdersWithItems()
```

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `OrderController` | `createOrderFromCart`, `cancelOrder`, `getOrderById`, `getUserOrders`, `getAllOrders`, `canCancelOrder` |
| `CustomerController` | `getUserOrders` |
| `DashboardService` | `getTotalOrders`, `getAllOrders` (via `orderRepository.count()` / `findAll()`) |
| `ReportController` | `getUserOrders` for customer summaries |
