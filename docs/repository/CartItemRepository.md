# CartItemRepository.java

**File:** `src/main/java/com/shopping/system/repository/CartItemRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** Aliya  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `CartItem` entities. Key function: detect whether a product is already in the cart before adding (to update quantity rather than create a duplicate row).

---

## Interface Declaration

```java
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> { ... }
```

---

## Custom Methods

### `findByCartId`
```java
List<CartItem> findByCartId(Long cartId);
```
**Generated SQL:** `SELECT * FROM cart_items WHERE cart_id = ?`  
**Used by:** Not called directly in current code — cart items are accessed via `cart.getCartItems()` through the `@OneToMany` relationship. Kept for flexibility.

---

### `findByCartIdAndProductId`
```java
Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
```
**Generated SQL:** `SELECT * FROM cart_items WHERE cart_id = ? AND product_id = ?`

**Critical use case** — the "add to cart" deduplication logic in `CartService.addToCart()`:
```java
Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
if (existingItem.isPresent()) {
    CartItem item = existingItem.get();
    item.setQuantity(item.getQuantity() + quantity);  // update existing
    cartItemRepository.save(item);
} else {
    CartItem newItem = new CartItem(cart, product, quantity, product.getPrice());
    cartItemRepository.save(newItem);                 // create new
}
```

Without this, adding "Samsung TV" twice would create two separate rows. With it, the quantity is incremented on the existing row.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `CartService` | `findByCartIdAndProductId`, `save`, `deleteById`, `findById`, `delete` |
