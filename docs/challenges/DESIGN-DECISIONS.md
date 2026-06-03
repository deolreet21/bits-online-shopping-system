# Design Decisions & Challenges

**File:** `docs/challenges/DESIGN-DECISIONS.md`  
**Purpose:** Documents every significant design choice, the challenge that prompted it, the alternatives considered, and why the current approach was chosen. This is the "why" behind the code.

---

## 1. Session-Based Auth vs JWT

### Decision
Use `HttpSession` for authentication, not JWT tokens.

### Why Session-Based
- **Simpler for server-rendered apps**: Thymeleaf templates run on the server — the server already has access to session state. No token management needed in JavaScript.
- **Invalidation is trivial**: `session.invalidate()` immediately kills the session. With JWT, you need a blacklist or short expiry.
- **No XSS risk from token storage**: JWT stored in `localStorage` is vulnerable to XSS attacks. Session cookies with `HttpOnly` flag are not accessible to JavaScript.
- **Appropriate for scale**: This is a university project, not a distributed microservices system.

### Why Not JWT
- JWT shines when you have multiple stateless services (microservices) that need to verify a token without calling a central auth server.
- For a single monolithic server-rendered app, JWT adds complexity with no benefit.

### Implementation
```java
// Login: write to session
session.setAttribute("loggedInUser", user);
session.setMaxInactiveInterval(30 * 60);  // 30-minute timeout

// Every request: read from session
User user = (User) session.getAttribute("loggedInUser");

// Logout: invalidate
session.invalidate();
```

---

## 2. BCrypt Only vs Full Spring Security

### Decision
Use only `spring-security-crypto` for BCrypt password hashing. NOT the full `spring-boot-starter-security`.

### Why Not Full Spring Security
Including `spring-boot-starter-security` auto-configures:
- Form-based login at `/login` (conflicts with our `AuthController`)
- HTTP Basic auth
- CSRF protection (would break all our POST forms unless we add `_csrf` tokens)
- Default `UserDetailsService` expecting a specific user model
- Auto-redirect of all endpoints to the login page

All of these would **override** the custom authentication built in `AuthController` and `SessionInterceptor`. We'd spend more time disabling Spring Security features than using them.

### Trade-Off
Full Spring Security provides method-level security (`@PreAuthorize`), role hierarchy, OAuth2, remember-me, and hardened defaults. For a project where auth is manually handled, the overhead is not justified.

### BCrypt Usage
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

```java
// Bean declaration in main class
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// Usage in UserService
passwordEncoder.encode(rawPassword);      // on register
passwordEncoder.matches(raw, stored);     // on login
```

---

## 3. LazyInitializationException — The @Async + @Transactional Challenge

### The Problem
```java
@Transactional
public Order createOrderFromCart(...) {
    Order order = orderRepository.save(order);
    emailService.sendEmail(order);  // passes JPA entity to @Async method
}

@Async
public void sendEmail(Order order) {
    String email = order.getUser().getEmail();
    // ↑ LazyInitializationException: Session already closed!
}
```

When `@Transactional` commits, the Hibernate session closes. Any lazy-loaded association accessed after that point (in the `@Async` thread) throws `LazyInitializationException` because there's no session to load from.

### The Fix
Extract all needed values as plain types **before** `@Transactional` returns:

```java
@Transactional
public Order createOrderFromCart(...) {
    Order order = orderRepository.save(order);

    // While session is still open:
    String userEmail = order.getUser().getEmail();
    String userName  = order.getUser().getUsername();
    Long orderId     = order.getId();
    BigDecimal total = order.getTotalAmount();

    // Only primitives/Strings — no JPA entities:
    emailService.sendOrderConfirmation(userEmail, userName, orderId, total);
}
```

### Why This Works
`String`, `Long`, `BigDecimal` are Java primitives/immutables — they are fully loaded into memory. No Hibernate proxy, no session needed. The `@Async` method only manipulates these in-memory values.

### Alternative Not Taken: `FetchType.EAGER`
Setting `@ManyToOne(fetch = FetchType.EAGER)` on `Order.user` would load the user eagerly in the same query. But:
- EAGER loading can cause N+1 problems elsewhere (every order load also loads the user)
- It changes behavior globally, not just for the email case
- The string-extraction fix is surgical and doesn't affect other code paths

---

## 4. N+1 Problem and JOIN FETCH Solution

### The Problem
```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    System.out.println(order.getUser().getUsername());  // N+1: 1 query per order!
}
```

With 100 orders: 1 query to fetch orders + 100 queries to fetch each user = 101 queries.

### The Fix: JOIN FETCH
```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user JOIN FETCH o.orderItems oi JOIN FETCH oi.product ORDER BY o.orderDate DESC")
List<Order> findAllOrdersWithItems();
```

This single JPQL query generates one SQL with multiple JOINs — all data loaded in one round trip.

`DISTINCT` is required because JOIN on a one-to-many (Order → OrderItems) would return duplicate Order rows — one row per item. `DISTINCT` deduplicates at the JPA level.

### Why Not Fix It With EAGER Fetch
Same reasoning as above: EAGER changes all queries globally. JOIN FETCH in a specific `@Query` gives precise control — we pay the join cost only where we know we need all the data.

---

## 5. @Transactional for Order Creation

### The Problem
Order creation involves multiple DB operations:
1. Deduct inventory for each product
2. Create the Order row
3. Create OrderItem rows
4. Clear the cart

If step 3 fails (DB constraint violation), steps 1 and 2 have already modified the DB. The data is inconsistent.

### The Fix
```java
@Transactional
public Order createOrderFromCart(...) {
    // All DB operations run in one transaction
    // If ANY operation fails → ALL are rolled back
}
```

### How `@Transactional` Works
Spring wraps the method in a proxy. When the method starts, a DB transaction begins (`BEGIN`). When the method returns normally, the transaction commits (`COMMIT`). If a `RuntimeException` is thrown, it rolls back (`ROLLBACK`).

### Why Not Multiple Smaller Transactions
Each `@Transactional` method can participate in an outer transaction. `cartService.clearCart()` (also `@Transactional`) joins the existing transaction from `createOrderFromCart` — they share the same connection and commit together.

---

## 6. CascadeType.ALL + orphanRemoval for Cart

### Cart → CartItems Relationship
```java
@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
private List<CartItem> cartItems = new ArrayList<>();
```

**`CascadeType.ALL`** — any operation on Cart (save, merge, remove) cascades to its CartItems. So `cartRepository.save(cart)` also saves/updates all CartItems.

**`orphanRemoval = true`** — if a CartItem is removed from the `cart.cartItems` list, Hibernate automatically deletes it from the DB on the next `save()`. This enables:
```java
cart.getCartItems().clear();
cartRepository.save(cart);
// → DELETE FROM cart_items WHERE cart_id = ?
```

Without `orphanRemoval`, removing items from the list would NOT delete DB rows — they'd stay as orphaned records.

### Same Pattern on Order → OrderItems
```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems = new ArrayList<>();
```

Saving the order cascades to save all order items. This is why `orderRepository.save(order)` (after adding items to `order.getOrderItems()`) inserts all item rows.

---

## 7. @PrePersist / @PreUpdate for Timestamps

### Decision
Use JPA lifecycle callbacks for automatic timestamp management.

```java
@PrePersist
public void prePersist() {
    this.createdDate = LocalDateTime.now();
    this.updatedDate = LocalDateTime.now();
}

@PreUpdate
public void preUpdate() {
    this.updatedDate = LocalDateTime.now();
}
```

### Why Not Set Timestamps in Service Layer
If timestamps were set in `OrderService.createOrder()`, every service that creates/updates an entity must remember to set them. `@PrePersist`/`@PreUpdate` are called by Hibernate automatically — no service code needed, impossible to forget.

### Why Not `@CreationTimestamp`/`@UpdateTimestamp` (Hibernate Annotations)
Hibernate-specific annotations work but they tie the code to Hibernate's API. JPA lifecycle callbacks (`@PrePersist`) are JPA standard — portable to any JPA implementation (EclipseLink, etc.).

---

## 8. @ControllerAdvice for Centralized Exception Handling

### Problem
Without centralized handling, every controller would need try-catch:
```java
try {
    Order order = orderService.getById(id).orElseThrow(...);
} catch (RuntimeException e) {
    model.addAttribute("error", e.getMessage());
    return "error";
}
```

### Solution
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntime(RuntimeException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }
}
```

`@ControllerAdvice` makes the class apply to all controllers. `@ExceptionHandler` declares which exception types to intercept. If any controller method throws `RuntimeException`, Spring routes it here instead of showing a 500 white page.

### Specific Handlers
- `NoResourceFoundException` → 404 → `error/404.html`
- `RuntimeException` → 500 → `error.html`
- `IllegalArgumentException` → 400 → `error.html`
- `IllegalStateException` → 409 → `error.html` (e.g., cancel non-PENDING order)

---

## 9. `@Enumerated(EnumType.STRING)` vs ORDINAL

### Decision
```java
@Enumerated(EnumType.STRING)
@Column(name = "status")
private OrderStatus status;
```

### Why STRING Not ORDINAL
`EnumType.ORDINAL` stores the enum's position (0, 1, 2...). If you ever **reorder** the enum values or **insert** a new value in the middle, all existing DB values become wrong.

`EnumType.STRING` stores the name (`"PENDING"`, `"CONFIRMED"`). Adding `PROCESSING` between `CONFIRMED` and `SHIPPED` in the future does not break existing data.

**The rule:** Always use `EnumType.STRING`. ORDINAL is a maintenance trap.

---

## 10. Price Captured at Cart Add Time

### Decision
```java
cartItem.setPrice(product.getPrice());  // captured when added, not current price
```

### Why
If an admin reduces the iPhone price from ₹79,999 to ₹69,999 after a customer has already added it to their cart, should the cart reflect the new price?

**Answer: No.** E-commerce convention is price-at-add-time. The customer made a decision at ₹79,999. Changing it mid-session creates a confusing experience (cart total changes without customer action).

The same principle applies to OrderItem — `price` is captured at order creation time. Historical orders always reflect the price at the time of purchase, even if the product price changes later.

---

## 11. `ddl-auto=update` in Development

### Decision
```properties
spring.jpa.hibernate.ddl-auto=update
```

### Why
Convenience during development: adding a field to an entity automatically adds the column. No manual `ALTER TABLE` needed.

### Risk
`update` never drops columns. If you rename `quantityOnHand` to `stockCount`, `update` creates a new `stock_count` column and leaves the old `quantity_on_hand` column with its data. The old column becomes orphaned.

### Production Recommendation
Use `validate` (checks schema matches entities, throws if not) or `none` (do nothing, manage schema with Flyway/Liquibase migrations). For this project, `update` is acceptable.

---

## 12. `HandlerInterceptor` for Session Guard Instead of Spring Security Filter

### Decision
Session-checking logic is in `SessionInterceptor` (a Spring MVC `HandlerInterceptor`), not a Spring Security filter.

### Why HandlerInterceptor
`HandlerInterceptor.preHandle()` runs after the request is matched to a controller but before the controller method executes. It has access to `HttpServletRequest`/`HttpServletResponse` — enough to check the session and redirect.

Spring Security filters run earlier in the pipeline (Servlet filter chain) and have different APIs — heavier to configure correctly without full Spring Security.

For simple session existence checking, `HandlerInterceptor` is the right tool.

### Whitelist Pattern
```java
private static final List<String> PUBLIC_PATHS = List.of("/login", "/register", "/logout", "/css/", "/js/", "/favicon");

if (PUBLIC_PATHS.stream().anyMatch(path -> requestURI.startsWith(path))) {
    return true;  // allow without session
}
```

Static resources (`/css/`, `/js/`) must be whitelisted or they'll be blocked before login.

---

## 13. `record` for CustomerSummary in ReportController

### Decision
```java
public record CustomerSummary(User user, int orderCount) {}
```

### Why Record
`CustomerSummary` is a temporary data carrier — it exists only to pass a (User + orderCount) pair to the Thymeleaf template. A full class would need: private fields, constructor, getters, `equals`, `hashCode`, `toString`.

A Java `record` (Java 16+) generates all of that automatically in one line. It is:
- Immutable (fields are `final`)
- Concise
- Perfectly suited for view models / DTOs

Used in Thymeleaf as: `${summary.user.username}`, `${summary.orderCount}`.

---

## Summary Table

| Decision | Alternative Not Taken | Reason |
|----------|----------------------|--------|
| Session auth | JWT | Monolith + server-rendered; JWT adds complexity |
| BCrypt only | Full Spring Security | Full Security overrides custom auth |
| String extraction before @Async | EAGER fetch | EAGER causes N+1 elsewhere |
| JOIN FETCH | EAGER fetch | EAGER is global; JOIN FETCH is targeted |
| @Transactional on createOrder | Manual try-catch | Atomic guarantee with no boilerplate |
| orphanRemoval=true | Manual delete | Simplifies clearCart to list.clear() |
| @PrePersist for timestamps | Service-layer timestamps | Impossible to forget; not service's concern |
| @ControllerAdvice | Per-controller try-catch | Single place for all exceptions |
| EnumType.STRING | EnumType.ORDINAL | ORDINAL breaks on reorder/insert |
| Price at add time | Live product price | E-commerce convention; no surprise price changes |
| ddl-auto=update | Flyway migrations | Development convenience; acceptable for project |
