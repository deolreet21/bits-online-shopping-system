# CartItem.java

**File:** `src/main/java/com/shopping/system/entity/CartItem.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** Aliya  
**Type:** JPA Entity  
**Purpose:** Represents a single line item in a user's cart. Links a product to a cart with a quantity and stores the price (captured at the time the item was added) and the auto-calculated subtotal. If the same product is added twice, the quantity is incremented rather than creating a second row.

---

## Class-Level Annotations

```java
@Entity
@Table(name = "cart_items")
```

---

## Fields

### `cart`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cart_id", nullable = false)
private Cart cart;
```
Many cart items belong to one cart. `cart_id` is the FK column in `cart_items`. LAZY means the Cart is not loaded when CartItem is fetched unless explicitly accessed.

---

### `product`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = false)
private Product product;
```
Many cart items can reference the same product (by different users). LAZY loading is used — we only need product details when displaying the cart.

---

### `quantity`
```java
@Column(nullable = false)
private Integer quantity;
```
Number of units. Modified by `CartService.updateQuantity()`. If set to 0 or negative, the item is deleted.

---

### `price`
```java
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal price;
```
**Price is captured at the time the item is added to the cart.** If the admin changes a product's price later, existing cart items retain the original price. This prevents the cart total from changing unexpectedly while a customer is shopping.

---

### `subtotal`
```java
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal subtotal;
```
Always equal to `price × quantity`. **Never set manually by calling code** — it's calculated automatically.

---

## Lifecycle Method

```java
@PrePersist
@PreUpdate
public void calculateSubtotal() {
    if (price != null && quantity != null) {
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

Both `@PrePersist` (before INSERT) and `@PreUpdate` (before UPDATE) are on the same method. This guarantees `subtotal` is always consistent with `price × quantity` in the DB, even if someone forgets to call `setSubtotal()` manually.

---

## Smart Setters

```java
public void setQuantity(Integer quantity) {
    this.quantity = quantity;
    if (this.price != null) {
        this.subtotal = this.price.multiply(BigDecimal.valueOf(quantity));
    }
}

public void setPrice(BigDecimal price) {
    this.price = price;
    if (this.quantity != null) {
        this.subtotal = price.multiply(BigDecimal.valueOf(this.quantity));
    }
}
```

These setters recalculate `subtotal` immediately in Java memory. Combined with `@PrePersist/@PreUpdate`, this means `subtotal` is always accurate both **before saving** (for template display) and **in the DB** (for persistence).

---

## Constructor

```java
public CartItem(Cart cart, Product product, Integer quantity, BigDecimal price) {
    this.cart = cart;
    this.product = product;
    this.quantity = quantity;
    this.price = price;
    this.subtotal = price.multiply(BigDecimal.valueOf(quantity));  // calculated inline
}
```
Used in `CartService.addToCart()` when creating a new item. Subtotal is calculated immediately so the object is fully valid before being saved.

---

## Forward Linkage

| File | How |
|------|-----|
| `CartItemRepository` | `findByCartId()`, `findByCartIdAndProductId()` |
| `CartService` | Creates, updates, removes CartItems |
| `Cart.getTotal()` | Iterates `cartItems` and sums `getSubtotal()` |
| `OrderService.createOrderFromCart()` | Reads `cart.getCartItems()` to create OrderItems |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| cart | cart_id | BIGINT | FK → carts.id, NOT NULL |
| product | product_id | BIGINT | FK → products.id, NOT NULL |
| quantity | quantity | INT | NOT NULL |
| price | price | DECIMAL(10,2) | NOT NULL |
| subtotal | subtotal | DECIMAL(10,2) | NOT NULL |
