# Cart.java

**File:** `src/main/java/com/shopping/system/entity/Cart.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** Aliya  
**Type:** JPA Entity  
**Purpose:** Represents a user's shopping cart. One cart per user, containing zero or more `CartItem` entries. Persists between sessions — the cart is stored in the DB, not just in memory. When an order is placed, the cart is cleared.

---

## Class-Level Annotations

```java
@Entity
@Table(name = "carts")
```
Standard entity annotations mapping to the `carts` table.

---

## Fields

### `id`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
Auto-incrementing primary key.

---

### `user`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```
| Annotation | Reason |
|-----------|--------|
| `@ManyToOne` | Technically many carts could exist per user (though our logic enforces one); it's the correct JPA multiplicity because Cart is on the "many" side of the FK |
| `fetch = FetchType.LAZY` | The User object is NOT loaded from DB automatically when Cart is loaded. It's only fetched when you actually call `cart.getUser()`. This avoids loading the entire user row on every cart query. |
| `@JoinColumn(name = "user_id")` | Specifies the FK column name in the `carts` table. Without this, Hibernate would name it `user_id` by convention, but explicit is better. |
| `nullable = false` | A cart must belong to a user — orphan carts aren't allowed. |

---

### `cartItems`
```java
@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
private List<CartItem> cartItems = new ArrayList<>();
```
| Attribute | Reason |
|-----------|--------|
| `@OneToMany` | One cart has many cart items |
| `mappedBy = "cart"` | Tells JPA that `CartItem.cart` field owns the FK. The `carts` table does NOT have a column for this; the `cart_items.cart_id` column is the FK. |
| `cascade = CascadeType.ALL` | Any operation on Cart (persist, merge, remove) is cascaded to CartItems. When we save a Cart with new items, the items are saved automatically. When we delete a Cart, all its items are deleted too. |
| `orphanRemoval = true` | If we call `cart.getCartItems().clear()` (remove item from the list), Hibernate automatically deletes those CartItem rows from the DB. This is how `clearCart()` works in `CartService`. |
| Initialized to `new ArrayList<>()` | Prevents `NullPointerException` when checking `cart.getCartItems().isEmpty()` on a new, unsaved cart. |

---

### `createdDate`
```java
@Column(name = "created_date")
private LocalDateTime createdDate;
```
Set by `@PrePersist`. Useful for analytics (how long carts are active before orders are placed).

---

## Lifecycle Method

```java
@PrePersist
public void prePersist() {
    this.createdDate = LocalDateTime.now();
}
```
Called automatically before the cart row is first inserted into the DB.

---

## Business Method

```java
public BigDecimal getTotal() {
    if (cartItems == null || cartItems.isEmpty()) return BigDecimal.ZERO;
    return cartItems.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**What it does:** Sums up the subtotals of all cart items using Java Streams.

- `.stream()` — converts the list to a stream
- `.map(CartItem::getSubtotal)` — extracts the subtotal from each item (method reference)
- `.reduce(BigDecimal.ZERO, BigDecimal::add)` — starts at 0 and adds each subtotal

**Why in the entity and not the service?**  
The total is a derived property of the cart's data — it belongs logically with the data. Any template can call `${cart.total}` without needing a separate service call. This is the "rich domain model" approach.

---

## Constructors

```java
public Cart() {}          // Required by JPA
public Cart(User user) {  // Used in CartService.getOrCreateCart()
    this.user = user;
}
```

---

## Forward Linkage

| File | How |
|------|-----|
| `CartRepository` | `findByUserId()` — looks up a user's cart |
| `CartService` | Creates, updates, clears carts |
| `CartController` | Loads and displays cart via `getOrCreateCart()` |
| `OrderService` | Reads `cart.getCartItems()` to create order, then clears cart |
| Templates | `${cart.total}`, `${cart.cartItems}` in `cart/cart.html` |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| user | user_id | BIGINT | FK → users.id, NOT NULL |
| createdDate | created_date | DATETIME | nullable |

CartItems are in the separate `cart_items` table — not a column in `carts`.
