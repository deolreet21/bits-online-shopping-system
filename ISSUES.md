# ISSUES.md — Setup & Development Issues Log

This file documents setup issues and bugs encountered during the development of the e-Kiosk Online Shopping System, along with their root causes and fixes.

---

## 1. MySQL Connection Failed on Startup

**Symptom:** Application fails to start with `Communications link failure` or `Access denied for user 'root'@'localhost'`.

**Cause:** MySQL 8+/9 requires explicit SSL and timezone flags. Without them the driver throws handshake errors or timezone mismatch warnings.

**Fix:** Add the following flags to the datasource URL in `application.properties`:
```
useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

---

## 2. Spring DevTools Hot-Reload Causes ClassNotFoundException

**Symptom:** After saving a file, the hot-reload restart throws `ClassNotFoundException` for `Category`, `UserService`, or other beans.

**Cause:** Spring DevTools uses two classloaders. If a restart races with a partially compiled class, one classloader sees the old version and the other sees a new one, causing class identity mismatches.

**Fix:** Kill the running process completely and restart fresh:
```bash
lsof -ti:8080 | xargs kill -9 2>/dev/null
source .env && JAVA_HOME=... ./mvnw spring-boot:run
```

---

## 3. Port 8080 Already in Use

**Symptom:** `Web server failed to start. Port 8080 was already in use.`

**Cause:** A previous `mvnw spring-boot:run` process was not terminated cleanly (e.g., killed with Ctrl+C while Maven was still running).

**Fix:**
```bash
lsof -ti:8080 | xargs kill -9 2>/dev/null
```

---

## 4. DataInitializer Fails — Product Constructor Not Found

**Symptom:** `NoSuchMethodException` or compile error on `new Product(name, description, price, quantity, category)`.

**Cause:** The `Product` entity was refactored to use a no-args constructor only, and `category` was changed from `String` to a `Category` enum.

**Fix:** Use the no-args constructor with setters, and pass `Category.ELECTRONICS` enum values instead of strings. See `DataInitializer.java` helper method `makeProduct(...)`.

---

## 5. CartItemRepository.countByUser() Removed — CustomerController Compile Error

**Symptom:** `cannot find symbol: method countByUser(User)` in `CustomerController.java`.

**Cause:** HeenuReet refactored the cart model from user-based to `Cart`-entity-based. `CartItemRepository` no longer has `countByUser()`.

**Fix:** Switch to `CartRepository.findByUser(user).map(c -> c.getCartItems().size()).orElse(0L)`.

---

## 6. OrderRepository Method Names Changed — Multiple Compile Errors

**Symptom:** `cannot find symbol` for `countByUser`, `findByUserOrderByOrderDateDesc`, `findByOrderDateBetween`, `sumTotalAmountByOrderDateBetween`.

**Cause:** HeenuReet renamed all `OrderRepository` methods when adding `OrderItem` and `OrderService`.

**Fix:** Updated callers:

| Old method | New method |
|---|---|
| `countByUser(user)` | `findByUserId(user.getId()).size()` |
| `findByUserOrderByOrderDateDesc(user)` | `findByUserIdOrderByOrderDateDesc(user.getId())` |
| `findByOrderDateBetween(start, end)` | `findOrdersBetweenDates(start, end)` |
| `sumTotalAmountByOrderDateBetween(start, end)` | `findTodaysTotalSales()` |

---

## 7. Eclipse / VS Code Null-Safety Warnings on JPA Repository Calls

**Symptom:** Yellow warning squiggles (code 16778128) on `repository.save(entity)`, `repository.findById(id)` etc. saying the return value may be null.

**Cause:** Spring Data's `@NonNull` annotations conflict with the IDE's null-safety analysis, producing false positives.

**Fix:** Add `@SuppressWarnings("null")` on affected service methods (`UserService.save`, `ProductService.getById`, `CartService.getOrCreateCart`, etc.).

---

## 8. mvnw: Permission Denied

**Symptom:** `zsh: permission denied: ./mvnw` when trying to run the Maven wrapper.

**Cause:** The Maven wrapper script loses its execute permission after a `git clone` on some systems.

**Fix:**
```bash
chmod +x mvnw
```

---

## 9. Favicon Requests Return 500 Instead of 404

**Symptom:** Every page load logs a `500 Internal Server Error` for `/favicon.ico`.

**Cause:** Spring Boot's default error controller returns 500 for missing static resources when Thymeleaf is on the classpath, instead of a clean 404.

**Fix:** Serve an SVG favicon at `/favicon.svg` and link it in all templates:
```html
<link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
```
SVG favicons are the modern approach and require no `.ico` converter.

---

## 10. application.properties Not Found / Thymeleaf Template Location Warning

**Symptom:** `WARNING: Cannot find template location: classpath:/templates/` on startup.

**Cause:** The templates directory was empty or Thymeleaf was checking before the build output was copied.

**Fix:** Add `spring.thymeleaf.check-template-location=false` to `application.properties`. This suppresses the warning in development without affecting functionality.
