# ProductController.java

**File:** `src/main/java/com/shopping/system/controller/ProductController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring MVC Controller  
**Purpose:** Handles customer-facing product browsing: listing all products, searching by name, filtering by category, and viewing a single product detail page.

---

## Class-Level Annotations

```java
@Controller
@RequestMapping("/products")
```

---

## Endpoint: `GET /products` — Product List with Search/Filter

```java
@GetMapping
public String listProducts(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Category category,
                           Model model, HttpSession session) {
    List<Product> products;
    if (keyword != null && !keyword.isBlank() && category != null) {
        products = productService.searchWithCategory(keyword, category);
    } else if (keyword != null && !keyword.isBlank()) {
        products = productService.search(keyword);
    } else if (category != null) {
        products = productService.getByCategory(category);
    } else {
        products = productService.getAllProducts();
    }

    model.addAttribute("products", products);
    model.addAttribute("categories", Category.values());
    model.addAttribute("selectedCategory", category);
    model.addAttribute("keyword", keyword);
    model.addAttribute("currentUser", user);
    return "products/list";
}
```

**Decision tree** for which service method to call based on which query params are present.

**`Category.values()`** — passes all category enum values to the template for the filter dropdown. The template uses `${categories}` to render the category pills.

**`selectedCategory`** — sent back to template so the currently selected category pill stays highlighted.

---

## Endpoint: `GET /products/search`

```java
@GetMapping("/search")
public String searchProducts(...) {
    return listProducts(keyword, category, model, session);
}
```

Delegates entirely to `listProducts()`. Allows both `/products?keyword=phone` and `/products/search?keyword=phone` to work — the search bar form can use either URL.

---

## Endpoint: `GET /products/category/{category}`

```java
@GetMapping("/category/{category}")
public String byCategory(@PathVariable Category category, ...) {
    List<Product> products = productService.getByCategory(category);
    ...
    return "products/list";
}
```

`@PathVariable Category category` — Spring automatically converts the URL segment (`"ELECTRONICS"`) to the `Category` enum value using its built-in enum conversion. If an invalid category string is in the URL, Spring throws a `MethodArgumentTypeMismatchException`.

---

## Endpoint: `GET /products/{id}` — Product Detail

```java
@GetMapping("/{id}")
public String productDetail(@PathVariable Long id, Model model, HttpSession session) {
    Product product = productService.getById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    model.addAttribute("product", product);
    model.addAttribute("currentUser", user);
    return "products/detail";
}
```

Renders the product detail page with full product info and the "Add to Cart" form.

**`orElseThrow()`** — if no product exists with the given ID, throws `IllegalArgumentException` → `GlobalExceptionHandler` returns a 400 error page.

---

## Template → Controller Flow

```
products/list.html
  Search form:  GET /products?keyword=laptop&category=ELECTRONICS
  Category pill: GET /products/category/ELECTRONICS
  Product card link: GET /products/{id}

products/detail.html
  "Add to Cart" form: POST /cart/add (goes to CartController)
```

---

## Model Attributes Summary

| Attribute | Type | Template Use |
|-----------|------|--------------|
| `products` | `List<Product>` | Renders product cards in grid |
| `categories` | `Category[]` | Category filter pills |
| `selectedCategory` | `Category` | Highlights active filter |
| `keyword` | `String` | Pre-fills search box |
| `currentUser` | `User` | Navbar, role check |
