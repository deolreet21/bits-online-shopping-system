# CartService.java

**File:** `src/main/java/com/shopping/system/service/CartService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** Aliya  
**Type:** Spring Service  
**Purpose:** All business logic for shopping cart operations: create/retrieve a cart, add items (with deduplication), remove items, update quantity, clear cart, and calculate totals. All mutating operations are `@Transactional`.

---

## Class-Level Annotation

```java
@Service
public class CartService { ... }
```

---

## Dependencies

```java
@Autowired private CartRepository cartRepository;
@Autowired private CartItemRepository cartItemRepository;
@Autowired private UserRepository userRepository;
```

`UserRepository` is needed only when creating a new Cart — we need the full User object to set `cart.setUser(user)`.

---

## Methods

### `getOrCreateCart`
```java
@Transactional
public Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId).orElseGet(() -> {
        User user = userRepository.findById(userId)...;
        Cart cart = new Cart(user);
        return cartRepository.save(cart);
    });
}
```

`orElseGet()` is lazy — the lambda only executes if the Optional is empty (no cart exists). On first login, user has no cart → one is created automatically. On subsequent visits, the existing cart is returned.

---

### `addToCart`
```java
@Transactional
public Cart addToCart(Long userId, Product product, int quantity) {
    Cart cart = getOrCreateCart(userId);
    Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
    if (existingItem.isPresent()) {
        CartItem item = existingItem.get();
        item.setQuantity(item.getQuantity() + quantity);  // increment
        cartItemRepository.save(item);
    } else {
        CartItem newItem = new CartItem(cart, product, quantity, product.getPrice());
        cart.getCartItems().add(newItem);
        cartItemRepository.save(newItem);
    }
    return cartRepository.findById(cart.getId()).orElse(cart);
}
```

**Deduplication:** Uses `findByCartIdAndProductId` to check if product is already in cart.  
- If yes → increment quantity on existing item  
- If no → create new CartItem with current product price (price captured at add time)

The final `cartRepository.findById()` reloads the cart from DB to ensure the returned object reflects the updated state.

---

### `removeFromCart`
```java
@Transactional
public void removeFromCart(Long cartItemId) {
    cartItemRepository.deleteById(cartItemId);
}
```

Directly deletes the CartItem by its ID. Simple and focused.

---

### `updateQuantity`
```java
@Transactional
public Cart updateQuantity(Long cartItemId, int quantity) {
    CartItem item = cartItemRepository.findById(cartItemId)...;
    if (quantity <= 0) {
        Long cartId = item.getCart().getId();
        cartItemRepository.delete(item);           // quantity 0 = remove item
        return cartRepository.findById(cartId).orElseThrow();
    }
    item.setQuantity(quantity);                    // smart setter recalculates subtotal
    cartItemRepository.save(item);
    return cartRepository.findById(item.getCart().getId()).orElseThrow();
}
```

If quantity drops to 0 or below, the item is deleted rather than kept at 0. This prevents "ghost" items cluttering the cart.

---

### `clearCart`
```java
@Transactional
public void clearCart(Long userId) {
    Cart cart = getOrCreateCart(userId);
    cart.getCartItems().clear();    // removes from Java list
    cartRepository.save(cart);      // triggers orphanRemoval → DELETE in DB
}
```

**`orphanRemoval = true` in `Cart.cartItems`** is what makes this work. Clearing the Java list and saving the Cart tells Hibernate "these items are orphans — delete them from DB."

Called by `OrderService.createOrderFromCart()` after the order is successfully saved.

---

### `getCartTotal` and `getCartItemCount`
```java
public BigDecimal getCartTotal(Long userId) {
    return cartRepository.findByUserId(userId)
            .map(Cart::getTotal)      // calls Cart.getTotal() stream sum
            .orElse(BigDecimal.ZERO);
}

public int getCartItemCount(Long userId) {
    return cartRepository.findByUserId(userId)
            .map(c -> c.getCartItems().size())
            .orElse(0);
}
```

Both use `Optional.map()` — if no cart exists, returns the default value without throwing. Used in `CustomerController.customerDashboard()` to display the cart badge count.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `CartController` | `getOrCreateCart`, `addToCart`, `removeFromCart`, `updateQuantity`, `clearCart` |
| `OrderService` | `getOrCreateCart`, `clearCart` |
| `CustomerController` | `getCartItemCount` |
| `AdminController` | (none directly) |
