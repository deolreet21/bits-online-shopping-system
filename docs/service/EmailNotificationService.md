# EmailNotificationService.java

**File:** `src/main/java/com/shopping/system/service/EmailNotificationService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** HeenuReet  
**Type:** Spring Service  
**Purpose:** Sends email notifications to customers at key order lifecycle events. All methods are `@Async` so email sending never blocks the HTTP request — the user gets an instant response, and the email is sent in a background thread.

---

## Class-Level Annotation

```java
@Service
public class EmailNotificationService { ... }
```

---

## Dependencies

```java
@Autowired
private JavaMailSender mailSender;
```

`JavaMailSender` — provided by `spring-boot-starter-mail`. Auto-configured by Spring Boot using the SMTP properties in `application.properties`. Uses Gmail SMTP on port 465 with SSL.

---

## Why @Async — The Design Decision

Without `@Async`, the "Place Order" HTTP request would:
1. Create order in DB
2. **Wait for Gmail SMTP connection** (can take 1-3 seconds)
3. **Wait for email to be sent**
4. Then return the response to the browser

With `@Async`:
1. Create order in DB
2. Start email in background thread (fire and forget)
3. **Immediately** return response to browser

The user sees the order confirmation page instantly. The email arrives a moment later.

**Enabled by `@EnableAsync` in `OnlineShoppingSystemApplication.java`.**

---

## The LazyInitializationException Challenge

**The problem:**  
```java
// WRONG — do NOT do this
@Async
public void sendOrderPlaced(Order order) {
    String email = order.getUser().getEmail();  // LazyInitializationException!
}
```

When `@Transactional` in `OrderService.createOrderFromCart()` completes, the Hibernate session closes. The `@Async` thread runs AFTER the session is closed. Accessing `order.getUser()` (which is LAZY-loaded) tries to fetch from a closed session → throws `LazyInitializationException`.

**The fix — extract plain strings before calling async:**
```java
// CORRECT
emailNotificationService.sendOrderPlaced(
    savedOrder.getUser().getEmail(),        // extracted INSIDE the @Transactional
    savedOrder.getUser().getUsername(),     // extracted INSIDE the @Transactional
    savedOrder.getId(),
    savedOrder.getTotalAmount().toString()
);
```

The async methods receive plain `String` and `Long` values — no JPA entities, no lazy fields. Strings are plain Java objects — no Hibernate session needed.

---

## Methods

### `sendOrderPlaced`
```java
@Async
public void sendOrderPlaced(String email, String username, Long orderId, String total) {
    send(email,
         "Order Placed – #" + orderId,
         "Hi " + username + ",\n\nYour order #" + orderId + " has been placed successfully.\n" +
         "Total: Rs. " + total + "\n\n...");
}
```
Called from: `OrderService.createOrderFromCart()`

---

### `sendOrderConfirmed`
```java
@Async
public void sendOrderConfirmed(String email, String username, Long orderId) { ... }
```
Called from: `OrderService.updateOrderStatus()` when status → CONFIRMED

---

### `sendOrderShipped`
```java
@Async
public void sendOrderShipped(String email, String username, Long orderId, String address) { ... }
```
Called from: `OrderService.updateOrderStatus()` when status → SHIPPED. Includes shipping address.

---

### `sendOrderDelivered`
```java
@Async
public void sendOrderDelivered(String email, String username, Long orderId) { ... }
```
Called from: `OrderService.updateOrderStatus()` when status → DELIVERED

---

### `sendOrderCancelled`
```java
@Async
public void sendOrderCancelled(String email, String username, Long orderId) { ... }
```
Called from: `OrderService.cancelOrder()`

---

### `send` (private helper)
```java
private void send(String to, String subject, String body) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        System.out.println("[EMAIL] Sent '" + subject + "' to " + to);
    } catch (Exception e) {
        System.err.println("[EMAIL ERROR] Failed to send '" + subject + "' to " + to + ": " + e.getMessage());
    }
}
```

`SimpleMailMessage` — basic plain-text email. No HTML, no attachments.  
The `try-catch` ensures an email failure NEVER crashes the order flow. Errors are logged to console but don't throw exceptions to the caller.

---

## SMTP Configuration (in application.properties)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=465
spring.mail.username=${SMTP_USER}
spring.mail.password=${SMTP_PASS}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
```

Gmail requires an **App Password** (not your regular password) when 2FA is enabled. App passwords are 16-character codes generated in Google account security settings.

---

## Forward Linkage

| File | Methods Called |
|------|---------------|
| `OrderService` | `sendOrderPlaced`, `sendOrderCancelled`, `sendOrderConfirmed`, `sendOrderShipped`, `sendOrderDelivered` |
