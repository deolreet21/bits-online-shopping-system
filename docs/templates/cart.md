# Template: cart/cart.html

**File:** `src/main/resources/templates/cart/cart.html`  
**Owner:** Aliya  
**Controller:** `CartController.java` → `GET /cart`  
**Purpose:** Shopping cart page — cart items table with quantity update, item removal, cart total summary, shipping address input, and Place Order button.

---

## Empty vs Non-Empty Cart

```html
<!-- Empty state -->
<div th:if="${cart == null or #lists.isEmpty(cart.cartItems)}" class="text-center py-5">
    <i class="bi bi-cart-x display-1 text-muted"></i>
    <h4>Your cart is empty</h4>
</div>

<!-- Cart with items -->
<div th:if="${cart != null and !#lists.isEmpty(cart.cartItems)}" class="row g-4">
    ...
</div>
```

Compound `th:if` with `or`/`and` operators. Handles both null cart and cart with empty items list.

---

## Cart Items Table

```html
<tr th:each="item : ${cart.cartItems}">
    <td th:text="${item.product.name}"></td>
    <td th:text="'$' + ${#numbers.formatDecimal(item.price, 1, 2)}"></td>
    ...
```

**`item.product.name`** — accesses through the `CartItem → Product` association. Hibernate loads product lazily (it's within `@Transactional` in CartService, so the session is open when the template renders in the same thread).

**`item.price`** — the price captured when the item was added, not the current product price.

---

## Quantity Update Form (auto-submit on change)

```html
<form th:action="@{/cart/update}" method="post">
    <input type="hidden" name="cartItemId" th:value="${item.id}"/>
    <input type="number" name="quantity" th:value="${item.quantity}"
           min="1" th:max="${item.product.quantityOnHand}"
           onchange="this.form.submit()"/>
</form>
```

**`onchange="this.form.submit()"`** — submits the form immediately when the user changes the quantity value (no separate "Update" button needed). Browser auto-submits, controller updates the DB, page redirects back with new totals.

**`th:max`** — dynamically sets the HTML `max` attribute to the product's available stock. Prevents the user from adding more than available.

---

## Remove Item Form

```html
<form th:action="@{/cart/remove/{id}(id=${item.id})}" method="post">
    <button type="submit" class="btn btn-sm btn-outline-danger">
        <i class="bi bi-x-circle"></i>
    </button>
</form>
```

**`@{/cart/remove/{id}(id=${item.id})}`** — Thymeleaf URL expression with path variable. Generates `/cart/remove/42` where `42` is the `CartItem.id`.

**Why separate forms per row?** Each row needs a different `cartItemId`. A single form can't handle multiple rows. Each `<form>` is a self-contained POST for one item.

---

## Order Summary (right column)

```html
<div class="d-flex justify-content-between mb-2">
    <span>Subtotal:</span>
    <span th:text="'$' + ${#numbers.formatDecimal(cart.total, 1, 2)}"></span>
</div>
```

`cart.total` calls `Cart.getTotal()`:
```java
public BigDecimal getTotal() {
    return cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

The template doesn't compute the total — it reads the pre-computed value from the entity method.

---

## Checkout Form

```html
<form th:action="@{/orders/place}" method="post" id="checkoutForm">
    <textarea name="shippingAddress" rows="3" required></textarea>
    <button type="submit" class="btn btn-success btn-lg">Place Order</button>
</form>
```

POSTs to `OrderController.placeOrder()`. Only input from the user: `shippingAddress`. All order data (items, prices) comes from the cart in the DB.

`required` — browser validation prevents submission with empty address.

---

## `sticky-top` Order Summary

```html
<div class="card sticky-top" style="top: 80px;">
```

`sticky-top` is a Bootstrap utility: the card stays visible as the user scrolls the cart item table. `top: 80px` offsets it below the fixed navbar (80px height).

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `cart` | `Cart` (with `cartItems` loaded) | CartService |
| `currentUser` | `User` | Session |
| `cartCount` | `int` | CartService |
| `success` / `error` | `String` | Flash attributes |
