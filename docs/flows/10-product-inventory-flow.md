# Flow 10: Product & Inventory Flow

**End-to-End Trace: Admin adds/edits/deletes a product → customer browses/searches → low-stock inventory alerts surface on the admin dashboard**

---

## ASCII Flow Diagram

```
                   ── ADMIN SIDE ──────────────────────────────────────────────────────────

Browser                  Controller              Service            Repository          DB
  │                          │                      │                   │               │
  │  POST /admin/products/add │                      │                   │               │
  │  {name, price, qty, cat}  │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                  AdminProductController          │                   │               │
  │                  .addProduct()                   │                   │               │
  │                          │  save(product)        │                   │               │
  │                          │─────────────────────>│                   │               │
  │                          │               ProductService             │               │
  │                          │               .save()                    │               │
  │                          │                      │  productRepo.save()              │
  │                          │                      │─────────────────────────────────>│
  │                          │                      │                   │──INSERT────────>
  │                          │                      │                   │<──────────────│
  │  redirect:/admin/products │                      │                   │               │
  │  (flash: "Product added") │                      │                   │               │
  │<──────────────────────────│                      │                   │               │

                   ── CUSTOMER SIDE ────────────────────────────────────────────────────────

  │  GET /products?keyword=X&category=Y              │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                  ProductController               │                   │               │
  │                  .productList()                  │                   │               │
  │                          │  searchWithCategory() │                   │               │
  │                          │─────────────────────>│                   │               │
  │                          │                      │  findByNameContainingIgnoreCaseAndCategory
  │                          │                      │─────────────────────────────────>│
  │                          │                      │                   │──SELECT────────>
  │                          │                      │                   │<──────────────│
  │  products/list.html       │                      │                   │               │
  │<──────────────────────────│                      │                   │               │

                   ── INVENTORY ALERT ──────────────────────────────────────────────────────

  │  GET /admin/dashboard     │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                  AdminController                 │                   │               │
  │                          │  getLowStockProducts()│                   │               │
  │                          │─────────────────────>│                   │               │
  │                          │               DashboardService           │               │
  │                          │               productService             │               │
  │                          │               .getLowStockProducts(5)    │               │
  │                          │                      │  findAll() + stream filter        │
  │                          │                      │─────────────────────────────────>│
  │                          │                      │                   │<──────────────│
  │                          │               returns products with qty < 5              │
  │  admin/dashboard.html     │                      │                   │               │
  │  (low-stock alert panel)  │                      │                   │               │
  │<──────────────────────────│                      │                   │               │
```

---

## Step-by-Step Walkthrough

### Part A: Admin Product CRUD

#### A1: Add a Product — `POST /admin/products/add`

**File:** `AdminProductController.java`
```java
@PostMapping("/add")
public String addProduct(@ModelAttribute Product product,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
    if (!isAdmin(session)) return "redirect:/customer/dashboard";
    productService.save(product);
    redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' added successfully.");
    return "redirect:/admin/products";
}
```

`@ModelAttribute Product product` — Spring binds the form fields (`name`, `description`, `price`, `quantityOnHand`, `category`) directly to a `Product` object. No DTO needed.

`@PrePersist` on `Product` automatically sets `createdDate` and `updatedDate` before the `INSERT`.

**`GET /admin/products/add`** serves the form with all `Category` enum values for the dropdown:
```java
model.addAttribute("categories", Category.values());
```

#### A2: Edit a Product — `POST /admin/products/edit/{id}`

**File:** `AdminProductController.java`
```java
@PostMapping("/edit/{id}")
public String editProduct(@PathVariable Long id,
                          @ModelAttribute Product product, ...) {
    productService.update(id, product);
    ...
}
```

**File:** `ProductService.java`
```java
public Product update(Long id, Product updatedProduct) {
    Product existing = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    existing.setName(updatedProduct.getName());
    existing.setDescription(updatedProduct.getDescription());
    existing.setPrice(updatedProduct.getPrice());
    existing.setQuantityOnHand(updatedProduct.getQuantityOnHand());
    existing.setCategory(updatedProduct.getCategory());
    return productRepository.save(existing);
}
```

**Why re-fetch and copy fields** instead of saving the bound object directly? The `@ModelAttribute`-bound object has no `id` in its JPA context — saving it directly would attempt an `INSERT` (new entity). Re-fetching the managed entity and copying fields ensures a JPA `UPDATE` on the correct row. `@PreUpdate` then fires to refresh `updatedDate`.

#### A3: Delete a Product — `POST /admin/products/delete/{id}`

```java
@PostMapping("/delete/{id}")
public String deleteProduct(@PathVariable Long id, ...) {
    Product product = productService.getById(id).orElseThrow(...);
    productService.delete(id);
    redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' deleted.");
    return "redirect:/admin/products";
}
```

The product is fetched before deletion so its name can be included in the flash message. `productService.delete(id)` calls `productRepository.deleteById(id)`.

**Cascade consideration:** `Product` has no `CascadeType.REMOVE` to orders or feedback. If a product has existing order items or feedback, the DB foreign key constraint will prevent deletion (MySQL will throw a constraint violation). This is intentional — deleting a product that's part of order history would corrupt sales records.

---

### Part B: Customer Product Browse & Search

#### B1: `GET /products` — List with Optional Filter

**File:** `ProductController.java`
```java
@GetMapping("/products")
public String productList(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category,
                          Model model, HttpSession session) {
```

Three routing branches:
```
keyword + category → searchWithCategory(keyword, categoryEnum)
keyword only       → search(keyword)
neither            → getAllProducts()
```

**File:** `ProductService.java`
```java
public List<Product> search(String keyword) {
    if (keyword == null || keyword.isBlank()) return productRepository.findAll();
    return productRepository.findByNameContainingIgnoreCase(keyword);
}

public List<Product> searchWithCategory(String keyword, Category category) {
    if (keyword == null || keyword.isBlank()) return productRepository.findByCategory(category);
    return productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category);
}
```

**`ContainingIgnoreCase`** — Spring Data derives: `WHERE LOWER(name) LIKE LOWER('%keyword%')`. Case-insensitive substring match, no manual SQL needed.

`keyword.isBlank()` catches empty strings (user submitted the search form without typing anything) — falls back to the full list or category list.

#### B2: `GET /products/{id}` — Product Detail

```java
@GetMapping("/products/{id}")
public String productDetail(@PathVariable Long id, Model model, ...) {
    Product product = productService.getById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    model.addAttribute("product", product);
    return "products/detail";
}
```

Detail page shows description, price, stock status, and the feedback widget (customer can submit a rating directly from this page or just read others').

---

### Part C: Inventory — Low-Stock Alerts

#### C1: How Low-Stock Detection Works

**File:** `ProductService.java`
```java
public List<Product> getLowStockProducts(int threshold) {
    return productRepository.findAll().stream()
            .filter(p -> p.getQuantityOnHand() < threshold)
            .toList();
}
```

**File:** `DashboardService.java`
```java
public List<Product> getLowStockProducts() {
    return productService.getLowStockProducts(5);  // threshold = 5 units
}
```

The threshold of `5` is hardcoded in `DashboardService`. Any product with fewer than 5 units in stock appears in the low-stock panel on the admin dashboard.

**Why `findAll()` + stream filter** instead of a repository query like `findByQuantityOnHandLessThan(5)`? The current volume is small enough that loading all products is fine. A repository-level query would be more efficient at scale.

#### C2: Where Inventory Gets Deducted

Inventory decreases happen in `OrderService.createOrderFromCart()` (see Flow 04), not here. Each time an order is placed:
```java
product.setQuantityOnHand(product.getQuantityOnHand() - cartItem.getQuantity());
productRepository.save(product);
```

`quantityOnHand` is the live count. After enough orders, a product's quantity drops below 5 and the admin dashboard alert appears automatically on the next page load.

#### C3: Admin Dashboard Panel

**File:** `AdminController.java` (via `DashboardService`)
```java
model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts());
```

**Template:** `admin/dashboard.html`
```html
<div th:if="${!lowStockProducts.isEmpty()}" class="alert alert-warning">
    <h5>Low Stock Alert</h5>
    <ul>
        <li th:each="p : ${lowStockProducts}"
            th:text="${p.name + ' — ' + p.quantityOnHand + ' left'}">
        </li>
    </ul>
</div>
```

The alert panel only renders if the list is non-empty (`th:if`). The admin sees product name + remaining quantity. Clicking through to `/admin/products` shows the full list with an edit option to restock.

---

## Product Entity: Key Fields

```java
@Entity
@Table(name = "products")
public class Product {
    private String   name;             // 100 chars, NOT NULL
    private String   description;      // TEXT, nullable
    private BigDecimal price;          // 10,2 precision, NOT NULL
    private Integer  quantityOnHand;   // live stock count, NOT NULL
    private Category category;         // enum stored as STRING
    private LocalDateTime createdDate; // set by @PrePersist
    private LocalDateTime updatedDate; // set by @PrePersist + @PreUpdate
}
```

`Category` is stored as a `VARCHAR` (e.g., `"ELECTRONICS"`) via `@Enumerated(EnumType.STRING)`. This is safer than ordinal storage — adding a new category in the middle of the enum won't corrupt existing DB rows.

---

## Search Routing Summary

| URL | Behaviour |
|-----|-----------|
| `GET /products` | All products |
| `GET /products?keyword=mouse` | Name contains "mouse" (case-insensitive) |
| `GET /products?category=ELECTRONICS` | All Electronics |
| `GET /products?keyword=mouse&category=ELECTRONICS` | Electronics containing "mouse" |
| `GET /products/search?keyword=mouse` | Same as keyword-only (alternate endpoint) |

---

## Files Involved

| File | Role |
|------|------|
| `admin/products/list.html` | Admin product table with edit/delete links |
| `admin/products/add.html` | Add product form |
| `admin/products/edit.html` | Edit product form |
| `products/list.html` | Customer-facing product grid with search bar |
| `products/detail.html` | Individual product page |
| `AdminProductController.java` | Admin CRUD: add, edit, delete |
| `ProductController.java` | Customer browse, search, detail |
| `ProductService.java` | CRUD, search, `getLowStockProducts()` |
| `ProductRepository.java` | `findByCategory`, `findByNameContainingIgnoreCase`, `findByNameContainingIgnoreCaseAndCategory` |
| `DashboardService.java` | Calls `getLowStockProducts(5)` for dashboard alert |
| `Product.java` | Entity with `@PrePersist`/`@PreUpdate` timestamps |
| `Category.java` | Enum with display names for dropdown and grouping |
