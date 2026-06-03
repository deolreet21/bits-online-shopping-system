# Flow 07: Email Notification Flow

**End-to-End Trace: How @Async email works, why it doesn't block the user, and the LazyInitializationException challenge**

---

## ASCII Flow Diagram

```
HTTP Request Thread               Spring Thread Pool           Gmail SMTP
  │                                    │                           │
  │  @Transactional method running     │                           │
  │  (createOrderFromCart)             │                           │
  │                                    │                           │
  │  Strings extracted from entities   │                           │
  │  before @Transactional closes:     │                           │
  │    userEmail = user.getEmail()     │                           │
  │    orderId = order.getId()         │                           │
  │                                    │                           │
  │  emailService.send(email, orderId) │                           │
  │  ←── returns immediately ──────────│                           │
  │  (async, non-blocking)            │ @Async task submitted────>│
  │                                    │                           │
  │  HTTP response sent to browser     │  sendOrderConfirmation()  │
  │  (redirect to confirmation page)   │  running in thread pool   │
  │                                    │                           │
  │  BROWSER ALREADY SEES THE PAGE     │   SMTP connection opens   │
  │                                    │   message sent────────────>
  │                                    │                           │
  │                                    │   Email arrives           │
  │                                    │   ~1-2 seconds later      │
```

---

## The @Async Mechanism

### Setup: `@EnableAsync` in Application

**File:** `OnlineShoppingSystemApplication.java` — or a `@Configuration` class
```java
@SpringBootApplication
@EnableAsync   // enables Spring's @Async infrastructure
public class OnlineShoppingSystemApplication { ... }
```

Without `@EnableAsync`, the `@Async` annotation is silently ignored — methods run synchronously.

### Service Method: `@Async`

**File:** `EmailNotificationService.java`
```java
@Service
public class EmailNotificationService {

    @Autowired private JavaMailSender mailSender;

    @Async
    public void sendOrderConfirmation(String to, String username, Long orderId, BigDecimal total) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Order Confirmed - e-Kiosk #" + orderId);
        msg.setText("Hi " + username + "!\n\nYour order #" + orderId +
                    " for ₹" + total + " has been placed successfully.\n\nThank you!");
        mailSender.send(msg);
    }
}
```

**`@Async`** — Spring wraps this call in a `Runnable` and submits it to a thread pool. The calling thread returns immediately.

**`SimpleMailMessage`** — Spring's basic email class. For HTML emails with attachments, `MimeMessage` would be used instead.

**`JavaMailSender.send()`** — opens SMTP connection, authenticates, sends, closes. This is the blocking I/O operation that justifies `@Async`.

---

## Gmail SMTP Configuration

Set via environment variables (`.env`), mapped to Spring Mail properties:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=465
spring.mail.username=ekiosk.shopping@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx   # 16-char App Password
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.auth=true
```

**Port 465 + SSL** — SMTPS (SMTP over SSL). Alternative is port 587 with STARTTLS.

**App Password, not Gmail password:** Gmail requires App Passwords for third-party apps since 2022 (when "Less secure app access" was removed). The 16-char App Password is generated in Google Account → Security → 2-Step Verification → App Passwords.

**Why App Password in `.env` not `application.properties`?** Credentials are never committed to git. `.env` is in `.gitignore`. Other developers replace the credentials with their own Gmail App Password.

---

## The LazyInitializationException Challenge

### What is it?

JPA entities use `FetchType.LAZY` by default for relations. When you access `order.getUser()`, JPA does NOT query the DB immediately — it returns a Hibernate proxy. The actual `SELECT * FROM users` only runs when you call `.getEmail()` on that proxy.

This works while a Hibernate Session is open (within `@Transactional`). When `@Transactional` commits, the Session closes.

**Problem scenario:**
```java
@Transactional
public Order createOrderFromCart(...) {
    ...
    Order order = orderRepository.save(order);
    emailNotificationService.sendOrderConfirmation(order);  // ← passing entity
    return order;
}
```

```java
@Async
public void sendOrderConfirmation(Order order) {
    String email = order.getUser().getEmail();  // ← LAZY LOAD HERE
    // But: @Transactional is already COMMITTED. Session is CLOSED.
    // LazyInitializationException: could not initialize proxy - no Session
}
```

### The Fix

Extract all needed values as plain Strings/Longs **before** the `@Transactional` method returns:

```java
@Transactional
public Order createOrderFromCart(...) {
    ...
    Order order = orderRepository.save(order);

    // ── Extract BEFORE @Transactional closes ───────────────────
    String userEmail = order.getUser().getEmail();   // loads User NOW (session still open)
    String userName  = order.getUser().getUsername();
    Long orderId     = order.getId();
    BigDecimal total = order.getTotalAmount();
    // ───────────────────────────────────────────────────────────

    emailNotificationService.sendOrderConfirmation(userEmail, userName, orderId, total);
    // ↑ passes Strings + Long + BigDecimal — all primitives/immutables, no lazy loading
    return order;
}
```

By the time `sendOrderConfirmation` runs in the async thread, it only has `String`, `Long`, `BigDecimal` — no JPA entities, no lazy proxies.

---

## Why @Async Matters for UX

Without `@Async`:
```
User clicks "Place Order"
  → Controller calls createOrderFromCart (200ms)
  → Gmail SMTP: open connection + authenticate (500ms–2000ms)
  → Response sent to browser (2200ms total)
  → User waits 2+ seconds
```

With `@Async`:
```
User clicks "Place Order"
  → Controller calls createOrderFromCart (200ms)
  → emailService.send() returns immediately (0ms, submits to thread pool)
  → Response sent to browser (200ms total)
  → User sees confirmation page FAST
  ↓ (in background)
  → Email thread: Gmail SMTP (500ms–2000ms) → email delivered
```

User experience improves significantly — checkout feels instant.

---

## Error Handling in @Async

If `mailSender.send()` throws (e.g., wrong App Password, network timeout):
- The exception is caught by Spring's `AsyncUncaughtExceptionHandler`
- Default behavior: logs the exception to console
- **The order is NOT rolled back** — `@Transactional` already committed before `@Async` was called
- The user has a valid order; they just don't get a confirmation email

This is the correct trade-off: the order is the critical operation; email is best-effort.

---

## Files Involved

| File | Role |
|------|------|
| `OnlineShoppingSystemApplication.java` | `@EnableAsync` |
| `EmailNotificationService.java` | All `@Async` email methods |
| `OrderService.java` | String extraction before `@Async` call |
| `.env` | SMTP credentials (gitignored) |
| `application.properties` | `${SPRING_MAIL_*}` references |
