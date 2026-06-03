# DataInitializer.java

**File:** `src/main/java/com/shopping/system/DataInitializer.java`  
**Owner:** Mehwish  
**Type:** Spring Component / CommandLineRunner  
**Purpose:** Seeds the database with default users and sample products on application startup, but only if the data does not already exist. Runs once after all Spring beans and Hibernate are fully initialized.

---

## Class-Level Annotations

```java
@Component
public class DataInitializer implements CommandLineRunner {
```

### `@Component`
Makes this class a Spring-managed bean. Spring discovers it via component scan. Without `@Component`, Spring would not know this class exists and `run()` would never be called.

### `CommandLineRunner` interface
A Spring Boot interface with one method:
```java
void run(String... args) throws Exception;
```

Spring calls `run()` automatically after the full application context is loaded — all beans wired, Hibernate schema synced, Tomcat started. This is the safe moment to write to the DB.

**Why `CommandLineRunner` vs `@PostConstruct`?**  
`@PostConstruct` runs inside the bean's constructor phase — before `DataSource`, `EntityManager`, and other beans may be fully ready. `CommandLineRunner` runs after everything is guaranteed to be ready.

---

## Dependencies

```java
@Autowired private UserService userService;
@Autowired private ProductRepository productRepository;
```

- `UserService` — used to register users (handles BCrypt hashing)
- `ProductRepository` — saves products directly via `saveAll()`

`UserService` is used (not `UserRepository` directly) because `registerUser()` already encodes the password. If `UserRepository.save()` were called directly, the password would be stored in plain text.

---

## `run()` Method

```java
@Override
public void run(String... args) {
    seedUsers();
    seedProducts();
}
```

Order matters: users must exist before products (no FK dependency here, but it's logical order).

---

## `seedUsers()`

```java
private void seedUsers() {
    if (!userService.existsByUsername("admin")) {
        userService.registerUser("admin", "admin@shop.com", "password123", UserRole.ADMIN);
    }
    // ... same for heenureet, aliya, mehwish
}
```

**Idempotency:** `existsByUsername()` checks before inserting. Running the app a second time does not create duplicate users.

**`UserService.registerUser()`** internally calls:
```java
user.setPassword(passwordEncoder.encode(rawPassword));
userRepository.save(user);
```

So the stored password is always BCrypt hash of `"password123"`.

---

## `seedProducts()`

```java
private void seedProducts() {
    if (productRepository.count() > 0) return;
    productRepository.saveAll(List.of(...));
}
```

**Guard:** `count() > 0` — if any products exist, skip entirely. This is coarser than the user check (which checks per username) but acceptable since products are never partially seeded.

**`saveAll(List.of(...))`** — batch insert. One call → one transaction → all 30 products inserted atomically. If any product fails, all fail (rollback).

---

## `product()` Helper Method

```java
private Product product(String name, String desc, double price, int qty, Category category) {
    Product p = new Product();
    p.setName(name);
    p.setDescription(desc);
    p.setPrice(BigDecimal.valueOf(price));
    p.setQuantityOnHand(qty);
    p.setCategory(category);
    return p;
}
```

A private builder helper to avoid repeating `new Product()` / setter boilerplate 30 times.

`BigDecimal.valueOf(double)` — converts `double` literal to `BigDecimal` correctly. Uses `double`→`String`→`BigDecimal` internally (avoids floating-point precision issues of `new BigDecimal(0.3)` which gives `0.2999999...`).

**What's NOT set:** `id` (auto-generated), `createdDate`, `updatedDate` (set by `@PrePersist` in `Product`).

---

## Relationship to data.sql

| | DataInitializer.java | data.sql |
|--|--|--|
| When it runs | After full context init (CommandLineRunner) | Before Hibernate (unless deferred) |
| Idempotency | `existsByUsername()` + `count() > 0` guards | `INSERT IGNORE` |
| Password hashing | BCryptPasswordEncoder.encode() | Hardcoded hash |
| Active? | Yes | No (disabled in application.properties) |
| Language | Java | SQL |

---

## Flow Diagram

```
Spring Boot starts
  │
  ├── Hibernate: ddl-auto=update → tables created/updated
  ├── All @Service, @Repository beans initialized
  │
  └── DataInitializer.run() called
        │
        ├── seedUsers()
        │     ├── existsByUsername("admin") → false → registerUser() → INSERT
        │     ├── existsByUsername("heenureet") → false → registerUser() → INSERT
        │     └── ... (3 more)
        │
        └── seedProducts()
              ├── count() → 0 → proceed
              └── saveAll(30 products) → 30 INSERTs in one transaction
```
