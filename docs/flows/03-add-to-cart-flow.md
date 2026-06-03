# Flow 03: Add to Cart Flow

**End-to-End Trace: Customer clicks "Add to Cart" → item appears in cart → quantity updated**

---

## ASCII Flow Diagram

```
Browser                  Controller           Service          Repository            DB
  │                          │                   │                 │                 │
  │  POST /cart/add           │                   │                 │                 │
  │  {productId=5, qty=1}     │                   │                 │                 │
  │──────────────────────────>│                   │                 │                 │
  │                    CartController             │                 │                 │
  │                    .addToCart()               │                 │                 │
  │                          │                   │                 │                 │
  │                          │  getProductById(5) │                 │                 │
  │                          │──────────────────>│                 │                 │
  │                          │                   │ findById(5)─────>│                 │
  │                          │                   │                 │──SELECT products─>
  │                          │                   │<────────────────│                 │
  │                          │<──────────────────│                 │                 │
  │                          │                   │                 │                 │
  │                    Stock check: qty > 0?      │                 │                 │
  │                    Qty requested ≤ QOH?       │                 │                 │
  │                          │                   │                 │                 │
  │                          │  addToCart(user,   │                 │                 │
  │                          │  product, qty)    │                 │                 │
  │                          │──────────────────>│                 │                 │
  │                          │                   │  findByUserId───>│                 │
  │                          │                   │  (get/create cart)│                │
  │                          │                   │                 │                 │
  │                          │                   │  findByCartId    │                 │
  │                          │                   │  AndProductId    │                 │
  │                          │                   │  (existing item?)│                │
  │                          │                   │                 │                 │
  │                          │                   │  If exists: update qty+subtotal   │
  │                          │                   │  If new: create CartItem          │
  │                          │                   │                 │                 │
  │                          │                   │  cartRepository.save()────────────>
  │                          │                   │                 │                 │
  │  redirect:/products (success flash)           │                 │                 │
  │<──────────────────────────│                   │                 │                 │
```

---

## Step-by-Step Walkthrough

### Step 1: Form Submission

**Template:** `products/list.html` (also `products/detail.html`)
```html
<form th:action="@{/cart/add}" method="post">
    <input type="hidden" name="productId" th:value="${product.id}"/>
    <input type="hidden" name="quantity" value="1"/>
    <button type="submit" th:disabled="${product.quantityOnHand <= 0}">
        Add to Cart
    </button>
</form>
```

The form POSTs `productId` and `quantity` to `/cart/add`.

### Step 2: Controller Receives Request

**File:** `CartController.java`
```java
@PostMapping("/cart/add")
public String addToCart(@RequestParam Long productId,
                        @RequestParam(defaultValue = "1") int quantity, ...) {
    User user = (User) session.getAttribute("loggedInUser");
```

`@RequestParam(defaultValue = "1")` — if `quantity` is missing from the form, default to 1.

**Stock validation (first check):**
```java
Product product = productService.getById(productId).orElseThrow(...);
if (product.getQuantityOnHand() <= 0) {
    redirectAttributes.addFlashAttribute("error", "This product is out of stock.");
    return "redirect:/products";
}
if (quantity > product.getQuantityOnHand()) {
    redirectAttributes.addFlashAttribute("error", "Only " + product.getQuantityOnHand() + " units available.");
    return "redirect:/products";
}
```

Two separate checks: out of stock entirely, vs requested quantity exceeds available.

### Step 3: CartService.addToCart()

**File:** `CartService.java`

**3a: Get or create cart**
```java
Cart cart = cartRepository.findByUserId(userId)
        .orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(user);
            return cartRepository.save(c);
        });
```

`orElseGet(() -> ...)` — the lambda only executes if `findByUserId` returns empty. Lazy creation: cart is only created when needed. `orElse(new Cart())` would always call `new Cart()` even when a cart exists.

**3b: Check for existing cart item (deduplication)**
```java
Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
```

SQL: `SELECT * FROM cart_items WHERE cart_id = ? AND product_id = ?`

**3c: Update or create**
```java
if (existing.isPresent()) {
    CartItem item = existing.get();
    item.setQuantity(item.getQuantity() + quantity);
    // setQuantity triggers recalculation: subtotal = price × quantity
} else {
    CartItem item = new CartItem();
    item.setCart(cart);
    item.setProduct(product);
    item.setPrice(product.getPrice());    // price captured at add time
    item.setQuantity(quantity);
    cart.getCartItems().add(item);
}
cartRepository.save(cart);
```

**Why capture price at add time?** If the admin changes the product price tomorrow, items already in the cart should reflect the price when added. This is standard e-commerce behavior.

**Why `cartRepository.save(cart)` not `cartItemRepository.save(item)`?**  
`Cart` has `cascade = CascadeType.ALL` on its `cartItems`. Saving the cart cascades to save/update all its items.

### Step 4: `@PrePersist`/`@PreUpdate` on CartItem

**File:** `CartItem.java`
```java
@PrePersist
@PreUpdate
public void calculateSubtotal() {
    this.subtotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
}
```

Before every insert/update, Hibernate calls this method. The `subtotal` is always price × quantity — no manual calculation needed elsewhere.

### Step 5: Flash Message and Redirect

```java
redirectAttributes.addFlashAttribute("success", "Product added to cart!");
return "redirect:/products";
```

Post-Redirect-Get: browser follows the redirect and sees the success message on the products page. If the user refreshes, it's a GET `/products` — no re-POSTing to `/cart/add`.

---

## Files Involved

| File | Role |
|------|------|
| `products/list.html` | Add-to-Cart form per product card |
| `CartController.java` | Receives POST, validates stock, calls service |
| `CartService.java` | `getOrCreateCart`, deduplication, price capture |
| `CartItemRepository.java` | `findByCartIdAndProductId` for dedup |
| `CartRepository.java` | `findByUserId`, save cascade |
| `CartItem.java` | `@PrePersist calculateSubtotal()` |
| `ProductRepository.java` | `findById` for stock check |
