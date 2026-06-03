# Template: products/detail.html

**File:** `src/main/resources/templates/products/detail.html`  
**Owner:** HeenuReet  
**Controller:** `ProductController.java` → `GET /products/{id}`  
**Purpose:** Single product detail page — full description, price, category, stock status, and Add-to-Cart form with quantity selector.

---

## Product Information Display

```html
<div class="col-md-8">
    <h2 class="fw-bold" th:text="${product.name}">Product Name</h2>
    <span class="badge bg-primary-subtle text-primary mb-2"
          th:text="${product.category.displayName}"></span>
    <p class="text-muted" th:text="${product.description}">Description</p>
    <h3 class="text-primary fw-bold"
        th:text="'₹' + ${#numbers.formatDecimal(product.price, 1, 2)}"></h3>
</div>
```

`product.category.displayName` — calls the `displayName` field on the `Category` enum (e.g., `"Electronics"` not `"ELECTRONICS"`).

---

## Stock Status Badge

```html
<span th:if="${product.quantityOnHand > 0}"
      class="badge bg-success">In Stock</span>
<span th:if="${product.quantityOnHand <= 0}"
      class="badge bg-danger">Out of Stock</span>
<p class="text-muted small"
   th:text="${product.quantityOnHand} + ' units available'"></p>
```

---

## Add to Cart Form with Quantity

```html
<form th:if="${product.quantityOnHand > 0 and currentUser.role.name() == 'CUSTOMER'}"
      th:action="@{/cart/add}" method="post" class="d-flex gap-2 align-items-center">
    <input type="hidden" name="productId" th:value="${product.id}"/>
    <input type="number" name="quantity" value="1" min="1"
           th:max="${product.quantityOnHand}" class="form-control" style="width: 90px;"/>
    <button type="submit" class="btn btn-primary btn-lg">
        <i class="bi bi-cart-plus me-2"></i>Add to Cart
    </button>
</form>
```

Unlike the list page (which always adds quantity 1), the detail page allows specifying a quantity. `th:max` prevents entering more than available stock.

**Form hidden only if** `quantityOnHand > 0 AND role == CUSTOMER` — two conditions. Out-of-stock products or admin users don't see the form.

---

## Back to Products Link

```html
<a th:href="@{/products}" class="btn btn-outline-secondary">
    <i class="bi bi-arrow-left me-2"></i>Back to Products
</a>
```

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `product` | `Product` | `productService.getById(id)` |
| `currentUser` | `User` | Session |
| `cartCount` | `int` | CartService |
| `success` / `error` | `String` | Flash attributes |
