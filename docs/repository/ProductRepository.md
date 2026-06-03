# ProductRepository.java

**File:** `src/main/java/com/shopping/system/repository/ProductRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** Aliya  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `Product` entities. Supports browsing (all products, by category), searching by name, and combined search. Used by `ProductService`, `OrderService`, `CartService`, `SalesAnalysisService`, and `DashboardService`.

---

## Interface Declaration

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> { ... }
```

---

## Inherited Methods Used in This Project

| Method | Where Used |
|--------|-----------|
| `save(Product)` | `ProductService.save()`, `ProductService.update()`, `OrderService` (stock update) |
| `findById(Long)` | `ProductService.getById()`, all controllers that fetch a product by ID |
| `findAll()` | `ProductService.getAllProducts()`, `DashboardService.getLowStockProducts()`, `SalesAnalysisService` |
| `deleteById(Long)` | `ProductService.delete()` |
| `count()` | `ProductService.getTotalProducts()` → `DashboardService.getTotalProducts()` |
| `saveAll(List)` | `DataInitializer.seedProducts()` — inserts all 30 products in one call |

---

## Custom Methods

### `findByCategory`
```java
List<Product> findByCategory(Category category);
```
**Generated SQL:** `SELECT * FROM products WHERE category = ?`  
**Used by:** `ProductService.getByCategory()` → `ProductController.byCategory()`, `ProductController.listProducts()` when category filter applied.

---

### `findByNameContainingIgnoreCase`
```java
List<Product> findByNameContainingIgnoreCase(String name);
```
**Generated SQL:** `SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%' + ? + '%')`  
**Breaking down the method name:**
- `findBy` — query trigger
- `Name` — the `name` field of Product
- `Containing` — generates a `LIKE '%value%'` (substring search)
- `IgnoreCase` — wraps both sides in `LOWER()` or equivalent

**Example:** Searching `"phone"` returns iPhone, any other product with "phone" in the name.  
**Used by:** `ProductService.search()` when only a keyword is provided.

---

### `findByNameContainingIgnoreCaseAndCategory`
```java
List<Product> findByNameContainingIgnoreCaseAndCategory(String name, Category category);
```
**Generated SQL:** `SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%?%') AND category = ?`  
Spring Data combines two conditions with `And` in the method name.  
**Used by:** `ProductService.searchWithCategory()` when both keyword and category filter are applied.

---

## Search Logic (in `ProductController`)

```java
if (keyword != null && !keyword.isBlank() && category != null) {
    products = productService.searchWithCategory(keyword, category);  // both
} else if (keyword != null && !keyword.isBlank()) {
    products = productService.search(keyword);                        // keyword only
} else if (category != null) {
    products = productService.getByCategory(category);                // category only
} else {
    products = productService.getAllProducts();                        // none - show all
}
```

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `ProductService` | All methods + `count()`, `findAll()`, `saveAll()` |
| `OrderService` | `findById()` for stock check; `save()` to update stock |
| `CartService` | Not used directly (goes through ProductService) |
| `DataInitializer` | `saveAll()`, `count()` |
| `SalesAnalysisService` | `findAll()` for slow-moving products |
| `DashboardService` | `findAll()` for low-stock alert |
