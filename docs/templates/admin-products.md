# Template: admin/products/list.html, add.html, edit.html

**Files:**  
- `src/main/resources/templates/admin/products/list.html`  
- `src/main/resources/templates/admin/products/add.html`  
- `src/main/resources/templates/admin/products/edit.html`  

**Owner:** Aliya  
**Controller:** `AdminProductController.java` → `GET/POST /admin/products`, `/admin/products/add`, `/admin/products/edit/{id}`, `/admin/products/delete/{id}`

---

## list.html — Product Management Table

Displays all products with name, category, price, stock, and action buttons (Edit/Delete).

### Delete as POST Form

```html
<form th:action="@{/admin/products/delete/{id}(id=${product.id})}" method="post">
    <button type="submit" class="btn btn-sm btn-outline-danger"
            onclick="return confirm('Delete product: ' + '[[${product.name}]]' + '?')">
        Delete
    </button>
</form>
```

**Why POST not GET for delete?** Using `<a href="/delete/42">` for deletion is dangerous — search engine crawlers and browser pre-fetch can follow GET links and accidentally delete data. POST requires a form submit (intentional user action).

**`[[${product.name}]]`** — Thymeleaf inline expression inside JavaScript string. Renders as the product name in the `confirm()` dialog.

---

## add.html — New Product Form

```html
<form th:action="@{/admin/products/add}" method="post">
    <input type="text" name="name" required/>
    <textarea name="description" rows="3" required></textarea>
    <input type="number" name="price" step="0.01" min="0" required/>
    <input type="number" name="quantityOnHand" min="0" required/>
    <select name="category" required>
        <option th:each="cat : ${categories}"
                th:value="${cat.name()}"
                th:text="${cat.displayName}"></option>
    </select>
    <button type="submit">Add Product</button>
</form>
```

**`step="0.01"`** — allows decimal inputs for price (e.g., `99.99`).

**`name="quantityOnHand"`** — the form field name matches the Java field name. Spring's `@ModelAttribute` binds `name="quantityOnHand"` → `product.setQuantityOnHand(value)`.

**`th:value="${cat.name()}`** — sends the enum constant name (e.g., `"ELECTRONICS"`), which Spring auto-converts to `Category.ELECTRONICS` in the `Product.category` field via `@ModelAttribute`.

---

## edit.html — Pre-Filled Edit Form

```html
<form th:action="@{/admin/products/edit/{id}(id=${product.id})}" method="post"
      th:object="${product}">
    <input type="text" th:field="*{name}" required/>
    <textarea th:field="*{description}" rows="3" required></textarea>
    <input type="number" th:field="*{price}" step="0.01" min="0" required/>
    <input type="number" th:field="*{quantityOnHand}" min="0" required/>
    <select th:field="*{category}">
        <option th:each="cat : ${categories}"
                th:value="${cat.name()}"
                th:text="${cat.displayName}"
                th:selected="${cat == product.category}"></option>
    </select>
</form>
```

**`th:object="${product}"`** — declares the form's bound object. All `*{...}` expressions are relative to this object.

**`th:field="*{name}"`** — shorthand for `name="name" th:value="${product.name}"`. Generates both the `name` attribute and the pre-filled `value`.

**`th:selected="${cat == product.category}"`** — pre-selects the product's current category in the dropdown.

---

## `@ModelAttribute` Binding in Controller

```java
@PostMapping("/add")
public String addProduct(@ModelAttribute Product product, ...) {
```

Spring reads all form fields, matches them to `Product` field names, and calls setters:
- `name="name"` → `product.setName(value)`
- `name="price"` → `product.setPrice(value)` (auto-converted String→BigDecimal)
- `name="category"` → `product.setCategory(value)` (auto-converted String→Category enum)

---

## Model Attributes Expected

| Page | Attribute | Source |
|------|-----------|--------|
| list.html | `products` | `productService.getAllProducts()` |
| add.html | `product` (empty), `categories` | `new Product()`, `Category.values()` |
| edit.html | `product` (filled), `categories` | `productService.getById(id)`, `Category.values()` |
| all | `currentUser` | Session |
