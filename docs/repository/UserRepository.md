# UserRepository.java

**File:** `src/main/java/com/shopping/system/repository/UserRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** HeenuReet  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `User` entities. Used by `UserService` for authentication, registration, and lookup. Also used directly by `CustomerController` and `ReportController` for user data access.

---

## Interface Declaration

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> { ... }
```

| Part | Explanation |
|------|-------------|
| `@Repository` | Spring stereotype annotation. Marks this as a data access object. Also enables Spring to translate JPA exceptions into Spring's `DataAccessException` hierarchy. |
| `extends JpaRepository<User, Long>` | `User` = the entity type. `Long` = the type of the primary key (`id`). This gives all standard CRUD methods for free. |

---

## Inherited Methods from JpaRepository (Free — No Code Needed)

| Method | SQL Equivalent |
|--------|---------------|
| `save(User user)` | `INSERT` or `UPDATE` depending on whether id is set |
| `findById(Long id)` | `SELECT * FROM users WHERE id = ?` |
| `findAll()` | `SELECT * FROM users` |
| `deleteById(Long id)` | `DELETE FROM users WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM users` |
| `existsById(Long id)` | `SELECT COUNT(*) > 0 WHERE id = ?` |

Spring Data generates the SQL implementation at runtime using proxy classes. No SQL or implementation code is required.

---

## Custom Methods

### `findByUsername`
```java
Optional<User> findByUsername(String username);
```
**Generated SQL:** `SELECT * FROM users WHERE username = ?`  
**How Spring knows:** The method name `findBy` + field name `Username` — Spring Data parses the name and builds the query automatically.  
**Returns:** `Optional<User>` — if no user exists with that username, returns `Optional.empty()` instead of `null`. Forces the caller to handle the absence case.  
**Used by:** `UserService.loginUser()`, `UserService.findByUsername()`

---

### `findByEmail`
```java
Optional<User> findByEmail(String email);
```
**Generated SQL:** `SELECT * FROM users WHERE email = ?`  
**Used by:** `UserService` — checks if email already taken during registration. Also used in password reset if implemented.

---

### `existsByUsername`
```java
boolean existsByUsername(String username);
```
**Generated SQL:** `SELECT COUNT(*) > 0 FROM users WHERE username = ?`  
**Why not just `findByUsername().isPresent()`?** `existsByUsername` only does a COUNT query — much cheaper than loading the full User object just to check existence.  
**Used by:** `UserService.registerUser()` for duplicate username check. `CustomerController.updateProfile()` for username change validation.

---

### `existsByEmail`
```java
boolean existsByEmail(String email);
```
**Generated SQL:** `SELECT COUNT(*) > 0 FROM users WHERE email = ?`  
**Used by:** Same as above for email uniqueness checks.

---

## Forward Linkage (Who Calls This)

| File | Methods Used |
|------|-------------|
| `UserService` | `findByUsername`, `findById`, `existsByUsername`, `existsByEmail`, `save`, `findAll` |
| `CustomerController` | `existsByUsername`, `existsByEmail`, `save` (direct injection — bypasses UserService for profile updates) |
| `ReportController` | `findAll()` — filters for CUSTOMER role to build customer report |
| `DataInitializer` | `userService.registerUser()` → `userRepository.save()` |

---

## Backward Linkage

| Dependency | Reason |
|------------|--------|
| `User` entity | The generic type `JpaRepository<User, Long>` |
| `JpaRepository` | Provides CRUD methods; comes from `spring-boot-starter-data-jpa` |
