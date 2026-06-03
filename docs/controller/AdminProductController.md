# AdminProductController.java

**File:** `src/main/java/com/shopping/system/controller/AdminProductController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Aliya  
**Type:** Spring MVC Controller  
**Purpose:** Provides admin-only CRUD operations for products: list all, add new, edit existing, and delete. All endpoints check admin role before proceeding.

---

## Class-Level Annotations

```java
@Controller
@RequestMapping("/admin/products")
```

---

## Authorization Helper

```java
private boolean isAdmin(HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    return user != null && user.getRole() == UserRole.ADMIN;
}
```

Called at the start of every method. Returns `true` if the logged-in user is an ADMIN. If not: `return "redirect:/customer/dashboard"`.

---

## Endpoints

### `GET /admin/products` — List Products
```java
@GetMapping
public String listProducts(Model model, HttpSession session) {
    if (!isAdmin(session)) return "redirect:/customer/dashboard";
    model.addAttribute("products", productService.getAllProducts());
    ...
    return "admin/products/list";
}
```

---

### `GET /admin/products/add` — Show Add Form
```java
@GetMapping("/add")
public String addProductForm(Model model, HttpSession session) {
    model.addAttribute("product", new Product());       // empty product for form binding
    model.addAttribute("categories", Category.values()); // all categories for dropdown
    return "admin/products/add";
}
```

An empty `new Product()` is added to the model for Thymeleaf's `th:object` binding. The form fields bind to this object's properties.

---

### `POST /admin/products/add` — Save New Product
```java
@PostMapping("/add")
public String addProduct(@ModelAttribute Product product, ...) {
    productService.save(product);
    redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' added successfully.");
    return "redirect:/admin/products";
}
```

`@ModelAttribute Product product` — Spring automatically binds all form fields (name, description, price, quantityOnHand, category) to the `Product` object fields by matching field names. `@PrePersist` in `Product` sets the timestamps on save.

---

### `GET /admin/products/edit/{id}` — Show Edit Form
```java
@GetMapping("/edit/{id}")
public String editProductForm(@PathVariable Long id, Model model, HttpSession session) {
    Product product = productService.getById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    model.addAttribute("product", product);   // pre-filled with existing data
    model.addAttribute("categories", Category.values());
    return "admin/products/edit";
}
```

The existing `product` object is passed — Thymeleaf pre-fills the form fields with current values.

---

### `POST /admin/products/edit/{id}` — Save Edit
```java
@PostMapping("/edit/{id}")
public String editProduct(@PathVariable Long id, @ModelAttribute Product product, ...) {
    productService.update(id, product);
    redirectAttributes.addFlashAttribute("success", "Product updated successfully.");
    return "redirect:/admin/products";
}
```

`productService.update()` loads the existing entity and copies field values — this preserves `createdDate` which isn't in the form.

---

### `POST /admin/products/delete/{id}` — Delete Product
```java
@PostMapping("/delete/{id}")
public String deleteProduct(@PathVariable Long id, ...) {
    Product product = productService.getById(id)...;
    productService.delete(id);
    redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' deleted.");
    return "redirect:/admin/products";
}
```

`GET` is not used for delete — a `POST` is required so that a browser pre-fetching links or a search engine crawling doesn't accidentally delete products.

---

## `@ModelAttribute` vs `@RequestParam`

`@RequestParam` — binds a single named request parameter.  
`@ModelAttribute` — binds ALL matching request parameters to an object's fields at once. Name matching is by field name: form field `name="price"` → `product.setPrice(...)`.

For complex objects like `Product` (5+ fields), `@ModelAttribute` is much cleaner than 5 separate `@RequestParam` arguments.
