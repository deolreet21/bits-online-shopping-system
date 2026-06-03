# CartController.java

**File:** `src/main/java/com/shopping/system/controller/CartController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Aliya  
**Type:** Spring MVC Controller  
**Purpose:** Handles all cart-related HTTP requests: displaying the cart, adding products, removing items, updating quantities, and clearing the cart. Validates stock availability before adding to cart.

---

## Class-Level Annotations

```java
@Controller
@RequestMapping("/cart")
public class CartController { ... }
```

---

## Dependencies

```java
@Autowired private CartService cartService;
@Autowired private ProductService productService;
```

`ProductService` is needed to load the full Product object (for stock validation) before adding to cart.

---

## Endpoints

### `GET /cart`
```java
@GetMapping
public String viewCart(Model model, HttpSession session) {
    var cart = cartService.getOrCreateCart(user.getId());
    model.addAttribute("cart", cart);
    model.addAttribute("currentUser", user);
    return "cart/cart";
}
```

`getOrCreateCart` — creates an empty cart if none exists. The template renders an "empty cart" message if `cart.cartItems` is empty.

---

### `POST /cart/add`
```java
@PostMapping("/add")
public String addToCart(@RequestParam Long productId,
                        @RequestParam(defaultValue = "1") int quantity, ...) {
    Product product = productService.getById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

    if (product.getQuantityOnHand() <= 0) {
        redirectAttributes.addFlashAttribute("error", "Product is out of stock.");
        return "redirect:/products/" + productId;
    }
    if (quantity > product.getQuantityOnHand()) {
        redirectAttributes.addFlashAttribute("error", "Requested quantity exceeds available stock.");
        return "redirect:/products/" + productId;
    }

    cartService.addToCart(user.getId(), product, quantity);
    redirectAttributes.addFlashAttribute("success", "'" + product.getName() + "' added to cart.");
    return "redirect:/cart";
}
```

**Stock validation — two checks:**
1. Is the product in stock at all? (`quantityOnHand <= 0`)
2. Is the requested quantity available? (`quantity > quantityOnHand`)

**`defaultValue = "1"`** — if the form submits without a quantity field, defaults to 1.

On success, redirects to `/cart` so the user sees their updated cart.

---

### `POST /cart/remove/{itemId}`
```java
@PostMapping("/remove/{itemId}")
public String removeFromCart(@PathVariable Long itemId, ...) {
    cartService.removeFromCart(itemId);
    redirectAttributes.addFlashAttribute("success", "Item removed from cart.");
    return "redirect:/cart";
}
```

`@PathVariable Long itemId` — the cart item's ID (not the product ID). This is important: the same product could theoretically be in carts of different users; we delete by the specific cart item row ID.

---

### `POST /cart/update`
```java
@PostMapping("/update")
public String updateQuantity(@RequestParam Long cartItemId,
                             @RequestParam int quantity, ...) {
    cartService.updateQuantity(cartItemId, quantity);
    redirectAttributes.addFlashAttribute("success", "Cart updated.");
    return "redirect:/cart";
}
```

If quantity ≤ 0, `CartService.updateQuantity()` deletes the item instead of updating.

---

### `POST /cart/clear`
```java
@PostMapping("/clear")
public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
    cartService.clearCart(user.getId());
    redirectAttributes.addFlashAttribute("success", "Cart cleared.");
    return "redirect:/cart";
}
```

Removes all items from the cart. Used by the "Clear Cart" button in the cart template.

---

## How the Cart Template Links to This Controller

In `cart/cart.html`:
```html
<!-- Remove item form -->
<form th:action="@{/cart/remove/{id}(id=${item.id})}" method="POST">
    <button type="submit">Remove</button>
</form>

<!-- Update quantity form -->
<form th:action="@{/cart/update}" method="POST">
    <input type="hidden" name="cartItemId" th:value="${item.id}">
    <input type="number" name="quantity" th:value="${item.quantity}">
    <button type="submit">Update</button>
</form>

<!-- Place order form (goes to OrderController) -->
<form th:action="@{/orders/place}" method="POST">
    <textarea name="shippingAddress"></textarea>
    <button type="submit">Place Order</button>
</form>
```

`@{/cart/remove/{id}(id=${item.id})}` — Thymeleaf URL expression that substitutes `{id}` with the item's actual ID.
