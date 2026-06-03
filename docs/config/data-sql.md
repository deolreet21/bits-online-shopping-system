# data.sql

**File:** `src/main/resources/data.sql`  
**Owner:** Mehwish  
**Type:** SQL seed script  
**Purpose:** Provides sample data for testing — 4 users (1 admin + 3 customers) and 30 products across 6 categories. **Currently not auto-run** — `DataInitializer.java` handles seeding instead. This file is kept as a reference/manual-use fallback.

---

## Why This File Exists

Spring Boot can auto-run `data.sql` on startup if configured. The file was used early in development before `DataInitializer.java` was written. It's kept for:
- Manual import via MySQL Workbench or CLI
- Reference for what seed data looks like in raw SQL

---

## How to Enable (Currently Disabled)

```properties
# In application.properties:
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

**`defer-datasource-initialization=true`** is critical: by default `data.sql` runs before Hibernate creates tables. This property delays it until after `ddl-auto=update` finishes, so the tables exist.

**Why it's disabled now:** `DataInitializer.java` (`CommandLineRunner`) runs after full Spring context initialization — after Hibernate, after all beans are wired. It's safer and more Java-idiomatic than raw SQL.

---

## `INSERT IGNORE` Explained

```sql
INSERT IGNORE INTO users (...) VALUES (...)
```

`IGNORE` = if the row already exists (duplicate primary key or unique key violation), skip it silently instead of throwing an error. This makes the script idempotent — safe to run multiple times.

Without `IGNORE`, re-running the script would throw:
```
ERROR 1062 (23000): Duplicate entry 'admin' for key 'users.UK_username'
```

---

## Password Encoding

```sql
'$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e'
```

This is the BCrypt hash of `"password123"`. All four users share the same hash because they all have the same password.

**BCrypt hash anatomy:**
```
$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e
│  │  │                                                         │
│  │  └─ 22-char salt (random per hash)                         │
│  └──── cost factor (10 = 2^10 = 1024 iterations)              │
└─────── BCrypt version                          24-char hash ──┘
```

**Why same hash for same password?** Normally BCrypt produces different hashes for the same password because the salt is random per call. These 4 entries happen to share the same hash because the same BCrypt call result was copy-pasted. This is safe — BCrypt verification reads the salt embedded in the hash string.

---

## Product Data

30 products across 6 categories:

| Category | Count | Price Range |
|----------|-------|-------------|
| ELECTRONICS | 6 | ₹8,999 – ₹79,999 |
| ELECTRICAL | 5 | ₹650 – ₹4,500 |
| FURNITURE | 5 | ₹4,200 – ₹22,000 |
| COSMETICS | 5 | ₹220 – ₹699 |
| TOYS | 5 | ₹999 – ₹4,500 |
| BOOKS | 4 | ₹299 – ₹699 |

**`quantity_on_hand`** values are intentionally varied:
- Some have low stock (3–4) to demonstrate the low-stock alert in the admin dashboard
- V-Guard Stabilizer and Canon Camera have qty 3–4, below the `< 5` threshold

---

## Column Name Mapping

| SQL Column | Java Field | Entity |
|------------|-----------|--------|
| `quantity_on_hand` | `quantityOnHand` | `Product` |
| `created_date` | `createdDate` | `User`, `Product` |
| `updated_date` | `updatedDate` | `Product` |

Hibernate converts `camelCase` to `snake_case` by default (`spring.jpa.hibernate.naming.physical-strategy`).

---

## Relationship to DataInitializer.java

`DataInitializer.java` contains the same data as this file but in Java:

| data.sql | DataInitializer.java |
|----------|----------------------|
| Raw SQL INSERT | `productRepository.saveAll(List.of(...))` |
| `INSERT IGNORE` for idempotence | `if (productRepository.count() > 0) return` |
| BCrypt hash hardcoded | `userService.registerUser(...)` calls `BCryptPasswordEncoder.encode()` |
| No guards | Checks `existsByUsername()` before inserting users |

`DataInitializer.java` is the active seeder. This file is dormant.
