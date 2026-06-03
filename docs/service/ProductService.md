# ProductService.java

**File:** `src/main/java/com/shopping/system/service/ProductService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** Aliya  
**Type:** Spring Service  
**Purpose:** Business logic layer for all product operations: full CRUD (create, read, update, delete), search by name, filter by category, and utility queries (total count, low-stock products). Called by both customer-facing and admin controllers.

---

## Class-Level Annotation

```java
@Service
public class ProductService { ... }
```

---

## Dependencies

```java
@Autowired private ProductRepository productRepository;
```

---

## Methods

### `getAllProducts`
```java
public List<Product> getAllProducts() {
    return productRepository.findAll();
}
```
Returns all products. Used as the default product list when no search/filter is active.

---

### `getById`
```java
public Optional<Product> getById(Long id) {
    return productRepository.findById(id);
}
```
Returns `Optional<Product>`. Controllers call `.orElseThrow()` to get the product or throw `IllegalArgumentException` if not found (handled by `GlobalExceptionHandler`).

---

### `save`
```java
public Product save(Product product) {
    return productRepository.save(product);
}
```
Used for creating new products. Hibernate's `@PrePersist` auto-sets `createdDate` and `updatedDate`.

---

### `update`
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

**Why not just `save(updatedProduct)` directly?**  
If we save the object from `@ModelAttribute` directly, it may have `id` set but `createdDate` = null (form fields don't include timestamps). This would overwrite `createdDate` with null in the DB. By loading `existing` first and copying only the editable fields, `createdDate` is preserved. `@PreUpdate` then refreshes `updatedDate` automatically.

---

### `delete`
```java
public void delete(Long id) {
    productRepository.deleteById(id);
}
```

**Note:** Deleting a product that has associated CartItems or OrderItems could cause a FK constraint violation if the DB enforces it. Currently, this risk exists — a more robust implementation would check for associations first. This is a known design limitation.

---

### `search` and `searchWithCategory`
```java
public List<Product> search(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return productRepository.findAll();
    }
    return productRepository.findByNameContainingIgnoreCase(keyword);
}

public List<Product> searchWithCategory(String keyword, Category category) {
    if (keyword == null || keyword.isBlank()) {
        return productRepository.findByCategory(category);
    }
    return productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category);
}
```

Both methods handle null/blank keyword gracefully — returning all products or category-filtered products rather than an empty list or error.

---

### `getLowStockProducts`
```java
public List<Product> getLowStockProducts(int threshold) {
    return productRepository.findAll().stream()
            .filter(p -> p.getQuantityOnHand() < threshold)
            .toList();
}
```
Used by `DashboardService.getLowStockProducts()` with threshold = 5. Products with fewer than 5 units are flagged in the admin dashboard.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `ProductController` | `getAllProducts`, `getById`, `search`, `searchWithCategory`, `getByCategory` |
| `AdminProductController` | `getAllProducts`, `getById`, `save`, `update`, `delete` |
| `CartController` | `getById` (to validate product before adding to cart) |
| `DashboardService` | `getTotalProducts`, `getLowStockProducts` |
| `FeedbackController` | `getAllProducts` (for feedback form dropdown) |
| `SalesAnalysisService` | (uses ProductRepository directly for slow-moving products) |
