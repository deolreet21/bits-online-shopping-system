# UserService.java

**File:** `src/main/java/com/shopping/system/service/UserService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** HeenuReet  
**Type:** Spring Service  
**Purpose:** Handles all user-related business logic: registration (with BCrypt hashing and uniqueness validation), login (with BCrypt password verification), user lookup, and customer counting for admin dashboard.

---

## Class-Level Annotation

```java
@Service
public class UserService { ... }
```

`@Service` — Spring stereotype annotation. Marks this class as a business logic component. Spring registers it as a bean, allowing it to be `@Autowired` in controllers. Functionally identical to `@Component` but semantically communicates "this is a service layer class."

---

## Dependencies

```java
@Autowired private UserRepository userRepository;
@Autowired private BCryptPasswordEncoder passwordEncoder;
```

| Dependency | Why Needed |
|------------|-----------|
| `UserRepository` | To query and persist User entities |
| `BCryptPasswordEncoder` | To hash passwords on registration and verify on login. Defined as a `@Bean` in `OnlineShoppingSystemApplication`. |

---

## Methods

### `registerUser`
```java
public User registerUser(String username, String email, String rawPassword, UserRole role) {
    if (userRepository.existsByUsername(username)) {
        throw new IllegalArgumentException("Username already taken: " + username);
    }
    if (userRepository.existsByEmail(email)) {
        throw new IllegalArgumentException("Email already registered: " + email);
    }
    User user = new User(username, email, passwordEncoder.encode(rawPassword), role);
    return userRepository.save(user);
}
```

**Flow:**
1. Check username uniqueness → throw if taken
2. Check email uniqueness → throw if taken
3. Encode the raw password with BCrypt: `passwordEncoder.encode(rawPassword)`
   - BCrypt generates a different salt each time → same password produces different hashes
   - Hash looks like: `$2a$10$N.zmdr9k...` (60 chars)
4. Create `User` object with encoded password
5. Save to DB via repository

**Why throw `IllegalArgumentException`?**  
This is caught by `GlobalExceptionHandler.handleIllegalArgument()` which returns a 400 Bad Request page. Alternatively, `AuthController` catches it in a try-catch and sets a flash attribute.

---

### `loginUser`
```java
public Optional<User> loginUser(String username, String rawPassword) {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Optional.of(user);
        }
    }
    return Optional.empty();
}
```

**Flow:**
1. Look up user by username
2. If found, use `passwordEncoder.matches(rawPassword, storedHash)` to verify
   - BCrypt re-hashes the raw input with the stored salt and compares
   - Returns true only if they match
3. Return `Optional.of(user)` on success, `Optional.empty()` on failure

**Security note:** The method returns `Optional.empty()` for BOTH "user not found" AND "wrong password." This prevents username enumeration attacks (an attacker can't tell the difference between a non-existent user and a wrong password).

---

### `getTotalCustomers`
```java
public long getTotalCustomers() {
    return userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.CUSTOMER)
            .count();
}
```

Loads all users, filters CUSTOMER role, counts. Used by `DashboardService` for the admin metric card. Simple enough that a stream is appropriate; a dedicated repository query would be more efficient at scale.

---

### Simple Lookups

```java
public Optional<User> findByUsername(String username) { ... }
public Optional<User> findById(Long id) { ... }
public boolean existsByUsername(String username) { ... }
public boolean existsByEmail(String email) { ... }
```

Thin wrappers around repository methods. The service layer provides a clean API for controllers — controllers don't need to know about repositories.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `AuthController` | `registerUser`, `loginUser` |
| `DataInitializer` | `registerUser`, `existsByUsername` |
| `DashboardService` | `getTotalCustomers` |

---

## Backward Linkage

| Dependency | Reason |
|------------|--------|
| `UserRepository` | Data access |
| `BCryptPasswordEncoder` | Password operations |
| `User`, `UserRole` | Operated-on entities |
