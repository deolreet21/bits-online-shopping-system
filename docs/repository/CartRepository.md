# CartRepository.java

**File:** `src/main/java/com/shopping/system/repository/CartRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** Aliya  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `Cart` entities. The primary use case is looking up a user's cart by user ID, or creating one if it doesn't exist (via `CartService.getOrCreateCart()`).

---

## Interface Declaration

```java
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> { ... }
```

---

## Custom Methods

### `findByUser`
```java
Optional<Cart> findByUser(User user);
```
**Generated SQL:** `SELECT * FROM carts WHERE user_id = ?` (using the User object's id)  
**Returns:** `Optional<Cart>` — `Optional.empty()` if the user has no cart yet.  
**Less commonly used** than `findByUserId` because it requires loading the User object first.

---

### `findByUserId`
```java
Optional<Cart> findByUserId(Long userId);
```
**Generated SQL:** `SELECT * FROM carts WHERE user_id = ?`  
**Why prefer this over `findByUser`?**  
In most places we already have the user's `id` from the session — there's no need to load the full User object just to find the cart. This avoids one unnecessary DB query.  
**Used by:** Almost all `CartService` methods.

---

## How `getOrCreateCart` Works

```java
// In CartService
public Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId).orElseGet(() -> {
        User user = userRepository.findById(userId)...;
        Cart cart = new Cart(user);
        return cartRepository.save(cart);     // creates new cart if none exists
    });
}
```

`orElseGet()` only runs the lambda if the Optional is empty — so the User is only fetched from DB when a new cart actually needs to be created.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `CartService` | `findByUserId`, `save`, `findById` |
