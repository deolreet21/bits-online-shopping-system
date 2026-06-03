# Template: products/list.html

**File:** `src/main/resources/templates/products/list.html`  
**Owner:** HeenuReet  
**Controller:** `ProductController.java` → `GET /products`, `GET /products/category/{category}`  
**Purpose:** Product browsing page — search bar, category filter dropdown, category pills, product grid cards with Add-to-Cart and View Details buttons.

---

## Thymeleaf Patterns Used

### th:replace for Navbar
```html
<div th:replace="~{fragments/navbar :: navbar}"></div>
```
Includes the shared navbar fragment.

### th:value for Pre-Filling Search
```html
<input type="text" name="keyword" th:value="${keyword}"/>
```
After a search, `keyword` is in the model. `th:value` pre-fills the input with the previous search term so the user sees what they searched for.

### th:each for Category Dropdown
```html
<option th:each="cat : ${categories}"
        th:value="${cat.name()}"
        th:text="${cat.displayName}"
        th:selected="${cat == selectedCategory}"></option>
```
- `th:each` iterates `Category.values()` array
- `th:value` = `"ELECTRONICS"` (enum name, sent to server)
- `th:text` = `"Electronics"` (display name from `Category.displayName`)
- `th:selected` = pre-selects the active category

### th:classappend for Active Category Pill
```html
<a th:classappend="${cat == selectedCategory} ? ' active' : ''"
   th:text="${cat.displayName}"></a>
```
`th:classappend` adds to existing classes without replacing them. The pill gets the Bootstrap `active` class when the category matches the current filter.

### th:disabled for Out-of-Stock Button
```html
<button th:disabled="${product.quantityOnHand <= 0}">Add to Cart</button>
```
Generates `disabled` HTML attribute when stock is 0. Button appears greyed out and is not clickable.

### #strings.abbreviate Utility
```html
<p th:text="${#strings.abbreviate(product.description, 80)}">Description</p>
```
Truncates text to 80 chars and appends `"..."`. Prevents long descriptions from breaking the card layout.

### #numbers.formatDecimal Utility
```html
<span th:text="'$' + ${#numbers.formatDecimal(product.price, 1, 2)}"></span>
```
Formats `BigDecimal(45999)` as `"45999.00"`. Parameters: min integer digits (1), fraction digits (2).

### th:if for Empty State
```html
<div th:if="${#lists.isEmpty(products)}" class="text-center py-5">
    <h5>No products found.</h5>
</div>
<div th:if="${!#lists.isEmpty(products)}" class="row row-cols-1 ...">
    ...product cards...
</div>
```
`#lists.isEmpty()` is a Thymeleaf built-in utility. Two mutually exclusive `th:if` blocks.

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `products` | `List<Product>` | ProductController |
| `categories` | `Category[]` | `Category.values()` |
| `keyword` | `String` or null | Request param |
| `selectedCategory` | `Category` or null | Request param |
| `currentUser` | `User` | Session |
| `cartCount` | `int` | CartService |

---

## Add to Cart Form (inside product card)

```html
<form th:if="${currentUser.role.name() == 'CUSTOMER'}"
      th:action="@{/cart/add}" method="post">
    <input type="hidden" name="productId" th:value="${product.id}"/>
    <input type="hidden" name="quantity" value="1"/>
    <button type="submit" th:disabled="${product.quantityOnHand <= 0}">
        Add to Cart
    </button>
</form>
```

- Only rendered for CUSTOMER role
- `productId` is a hidden field — the value comes from the model, not user input
- Always adds quantity 1 (user can adjust in cart)
- Disabled if out of stock (QOH ≤ 0)
