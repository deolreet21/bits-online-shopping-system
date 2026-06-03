# User.java

**File:** `src/main/java/com/shopping/system/entity/User.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** JPA Entity  
**Purpose:** Represents a registered user. Stored in the `users` table. Used for login, session management, and associating orders, cart, and feedback with a person.

---

## Class-Level Annotations

| Annotation | Reason |
|-----------|--------|
| `@Entity` | Tells Hibernate this class maps to a database table. Without this, JPA ignores the class. |
| `@Table(name = "users")` | Explicitly names the table `users`. Without this, Hibernate would default to the class name `user`, which is a reserved keyword in MySQL — this would cause a SQL error. |

---

## Fields

### `id`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
| Annotation | Reason |
|-----------|--------|
| `@Id` | Marks this as the primary key column |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Tells MySQL to use AUTO_INCREMENT. Each new user gets the next available ID. We don't set this manually. |

**Type choice:** `Long` (not `int`) — Long supports up to 9.2 quintillion values; avoids overflow in production systems.

---

### `username`
```java
@Column(nullable = false, unique = true, length = 50)
private String username;
```
| Attribute | Reason |
|-----------|--------|
| `nullable = false` | Generates `NOT NULL` constraint in DB — a user must have a username |
| `unique = true` | Generates a `UNIQUE INDEX` — prevents two users with the same username |
| `length = 50` | Sets `VARCHAR(50)` — limits input size; prevents abuse |

---

### `email`
```java
@Column(nullable = false, unique = true, length = 100)
private String email;
```
Same constraints as username. `length = 100` because email addresses can be longer than usernames (domain part).

---

### `password`
```java
@Column(nullable = false)
private String password;
```
No `length` specified → defaults to `VARCHAR(255)`. This is intentional: BCrypt hashes are always 60 characters, but no length cap prevents future algorithm changes. **Never stored as plain text — always BCrypt encoded before saving.**

---

### `role`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private UserRole role;
```
| Annotation | Reason |
|-----------|--------|
| `@Enumerated(EnumType.STRING)` | Stores `"CUSTOMER"` or `"ADMIN"` as a string in the DB. The alternative `EnumType.ORDINAL` stores `0` or `1` — fragile because adding a new enum value in the middle shifts all ordinals. |

---

### `createdDate`
```java
@Column(name = "created_date")
private LocalDateTime createdDate;
```
`@Column(name = "created_date")` — maps to snake_case column name as per DB convention. **Not set manually** — handled automatically by `@PrePersist`.

---

## Lifecycle Method

```java
@PrePersist
public void prePersist() {
    this.createdDate = LocalDateTime.now();
}
```
`@PrePersist` — JPA calls this method automatically **just before** the entity is inserted into the database (`INSERT` statement). This ensures `createdDate` is always set without requiring the caller to set it manually. If this were done in a service, it could be forgotten; `@PrePersist` makes it guaranteed.

---

## Constructors

```java
public User() {}  // Required by JPA — Hibernate creates instances via reflection
public User(String username, String email, String password, UserRole role) { ... }
```
JPA **requires** a no-arg constructor to instantiate entities when loading from DB. The 4-arg constructor is used when programmatically creating users (in `UserService.registerUser()`).

---

## Relationships (Implied — not declared in this class)

| Related Entity | Type | Where Declared |
|---------------|------|----------------|
| Cart | OneToOne (1 user → 1 cart in practice) | `Cart.user` field |
| Order | OneToMany (1 user → many orders) | `Order.user` field |
| Feedback | OneToMany (1 user → many feedback) | `Feedback.user` field |

All are declared on the **owning side** (Cart, Order, Feedback hold the FK column `user_id`), not in User. This is why User has no `@OneToMany` collection fields — keeping User simple avoids loading entire order history every time a user object is accessed.

---

## Forward Linkage (Who Uses This Class)

| File | How |
|------|-----|
| `UserService` | Creates, saves, authenticates User |
| `CartService` | Loads user to create/find cart |
| `OrderService` | Sets `order.setUser(user)` |
| `AuthController` | Stores User in session: `session.setAttribute("loggedInUser", user)` |
| `CustomerController` | Reads user from session |
| All controllers | Read `(User) session.getAttribute("loggedInUser")` |
| `DataInitializer` | Calls `userService.registerUser(...)` to create seed users |

---

## Backward Linkage (What This Class Depends On)

| Dependency | Reason |
|------------|--------|
| `UserRole` (enum) | Type of the `role` field |
| `jakarta.persistence.*` | All JPA annotations |
| `java.time.LocalDateTime` | Type of `createdDate` |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| username | username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | email | VARCHAR(100) | NOT NULL, UNIQUE |
| password | password | VARCHAR(255) | NOT NULL |
| role | role | VARCHAR(255) (ENUM stored as string) | NOT NULL |
| createdDate | created_date | DATETIME | nullable |
