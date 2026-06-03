# Flow 02: Browse Products Flow

**End-to-End Trace: Customer views products, searches, filters by category**

---

## ASCII Flow Diagram

```
Browser                  Controller              Service            Repository          DB
  │                          │                      │                   │               │
  │  GET /products            │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                    ProductController             │                   │               │
  │                    .listProducts()               │                   │               │
  │                          │  getAllProducts()     │                   │               │
  │                          │─────────────────────>│                   │               │
  │                          │                      │  findAll()────────>│               │
  │                          │                      │                   │──SELECT * FROM │
  │                          │                      │                   │  products──────>
  │                          │                      │<──────────────────│               │
  │                          │<─────────────────────│                   │               │
  │                          │                      │                   │               │
  │  products/list.html       │                      │                   │               │
  │<──────────────────────────│                      │                   │               │
  │                          │                      │                   │               │
  │  GET /products?keyword=TV&category=ELECTRONICS   │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                    (4-case search logic)         │                   │               │
  │                          │ findByNameContaining  │                   │               │
  │                          │ IgnoreCaseAndCategory │                   │               │
  │                          │─────────────────────────────────────────>│               │
  │                          │                      │      SELECT...WHERE│               │
  │                          │                      │      LOWER(name)   │               │
  │                          │                      │      LIKE '%tv%'   │               │
  │                          │                      │      AND category='ELECTRONICS'───>│
  │<──────────────────────────│                      │                   │<──────────────│
  │  filtered products/list.html                     │                   │               │
```

---

## Step-by-Step Walkthrough

### Step 1: No Search — `GET /products`

**File:** `ProductController.java`
```java
@GetMapping("/products")
public String listProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        Model model, HttpSession session) {
```

**4-case decision tree:**

```
keyword=null, category=null  →  getAllProducts()      (all 30 products)
keyword=null, category="TV"  →  getByCategory(cat)   (filter by category only)
keyword="TV", category=null  →  searchByName(kw)      (search by name only)
keyword="TV", category="EL"  →  searchByNameAndCategory(kw, cat)  (both filters)
```

All four paths end up in `ProductService`:
- `getAllProducts()` → `productRepository.findAll()`
- `getByCategory(category)` → `productRepository.findByCategory(category)`
- `searchByName(keyword)` → `productRepository.findByNameContainingIgnoreCase(keyword)`
- `searchByNameAndCategory(keyword, category)` → `productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category)`

### Step 2: Model Attributes Added

```java
model.addAttribute("products", products);
model.addAttribute("categories", Category.values());
model.addAttribute("keyword", keyword);
model.addAttribute("selectedCategory", selectedCategory);
model.addAttribute("currentUser", user);
model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
```

- `Category.values()` — the enum array `[ELECTRONICS, ELECTRICAL, FURNITURE, ...]` used to render category pills and dropdown
- `keyword` and `selectedCategory` — pre-fill the search form so the user sees their active filter
- `cartCount` — badge on the Cart nav link showing item count

### Step 3: Template Renders

**File:** `products/list.html`

Key Thymeleaf patterns:

```html
<!-- Category pills: clicking navigates to /products/category/ELECTRONICS -->
<a th:each="cat : ${categories}"
   th:href="@{/products/category/{c}(c=${cat.name()})}"
   th:classappend="${cat == selectedCategory} ? ' active' : ''"
   th:text="${cat.displayName}"></a>
```

- `th:each` iterates the `Category[]` array
- `@{/products/category/{c}(c=${cat.name()})}` → URL with path variable: `/products/category/ELECTRONICS`
- `th:classappend` adds `active` class to the currently selected category pill

```html
<!-- Product cards -->
<div class="col" th:each="product : ${products}">
    <span th:text="'$' + ${#numbers.formatDecimal(product.price, 1, 2)}"></span>
    <span th:class="${product.quantityOnHand > 0 ? 'badge bg-success' : 'badge bg-danger'}"
          th:text="${product.quantityOnHand > 0 ? 'In Stock' : 'Out of Stock'}"></span>
```

`#numbers.formatDecimal(product.price, 1, 2)` — Thymeleaf utility: format BigDecimal with min 1 integer digit, 2 decimal places. `45999.00` becomes `45999.00`.

```html
<!-- Add to Cart — only for CUSTOMER role -->
<form th:if="${currentUser.role.name() == 'CUSTOMER'}"
      th:action="@{/cart/add}" method="post">
    <input type="hidden" name="productId" th:value="${product.id}"/>
    <button th:disabled="${product.quantityOnHand <= 0}">Add to Cart</button>
</form>
```

Admin users see no "Add to Cart" button. Out-of-stock products have the button disabled.

---

### Step 4: Category URL Path — `GET /products/category/ELECTRONICS`

**File:** `ProductController.java`
```java
@GetMapping("/products/category/{category}")
public String byCategory(@PathVariable Category category, ...) {
```

`@PathVariable Category category` — Spring automatically converts the URL string `"ELECTRONICS"` to the `Category.ELECTRONICS` enum constant. This works because Spring has a built-in `StringToEnumConverterFactory`.

Then calls `productService.getByCategory(category)` and renders the same list template.

---

### Step 5: Product Detail — `GET /products/{id}`

**File:** `ProductController.java`
```java
@GetMapping("/products/{id}")
public String productDetail(@PathVariable Long id, ...) {
    Product product = productService.getById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
```

`getById()` → `productRepository.findById(id)` → `SELECT * FROM products WHERE id = ?`

`orElseThrow()` — if no product with that ID, throws `RuntimeException`. `GlobalExceptionHandler` catches it and shows the error page.

---

## Files Involved

| File | Role |
|------|------|
| `products/list.html` | Product grid, search form, category pills |
| `products/detail.html` | Single product detail page |
| `ProductController.java` | 4-case search logic, path variable enum conversion |
| `ProductService.java` | Delegates to repository |
| `ProductRepository.java` | `findByNameContainingIgnoreCase`, `findByCategory`, etc. |
| `fragments/navbar.html` | Cart count badge |
| `CartService.java` | `getCartItemCount()` for navbar badge |
