# Category.java

**File:** `src/main/java/com/shopping/system/entity/Category.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** Aliya  
**Type:** Java Enum  
**Purpose:** Defines the six product categories in the system. Each product must belong to exactly one category. Used for filtering products in the browse page and grouping in sales analysis.

---

## Enum Definition

```java
public enum Category {
    ELECTRONICS("Electronics"),
    ELECTRICAL("Electrical"),
    FURNITURE("Furniture"),
    COSMETICS("Cosmetics"),
    TOYS("Toys"),
    BOOKS("Books");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## The `displayName` Field — Why It Exists

Enum constants in Java must be valid identifiers (uppercase by convention). `ELECTRONICS` is fine internally, but in the UI we want to show `"Electronics"` (title case, readable).

Without `displayName`, templates would call `category.name()` and get `"ELECTRONICS"` — ugly on the page.

With `displayName`, templates call `category.getDisplayName()` and get `"Electronics"`.

**In Thymeleaf templates:**
```html
<span th:text="${category.displayName}">Electronics</span>
```

---

## Categories and Their Products

| Category | Sample Products |
|----------|----------------|
| ELECTRONICS | Samsung TV, iPhone, Sony Headphones, Lenovo Laptop |
| ELECTRICAL | Ceiling Fan, LED Bulbs, Mixer Grinder, Iron |
| FURNITURE | Study Table, Office Chair, Sofa, Bed Frame |
| COSMETICS | Foundation, Sunscreen, Face Wash, Shampoo |
| TOYS | LEGO, RC Car, Barbie, Scrabble |
| BOOKS | Clean Code, Atomic Habits, The Alchemist |

---

## How Stored in DB

In `Product.java`:
```java
@Enumerated(EnumType.STRING)
private Category category;
```
Stored as `"ELECTRONICS"`, `"BOOKS"`, etc. — the enum constant name, not the `displayName`. This keeps DB values consistent even if we change the display name later.

---

## Forward Linkage

| File | Usage |
|------|-------|
| `Product.java` | Type of the `category` field |
| `ProductRepository` | `findByCategory(Category)`, `findByNameContainingIgnoreCaseAndCategory()` |
| `ProductService` | `getByCategory(Category)`, `searchWithCategory()` |
| `ProductController` | `@RequestParam Category category` from URL query param |
| `AdminProductController` | `model.addAttribute("categories", Category.values())` for dropdown |
| `SalesAnalysisService` | `getSalesByCategory()` iterates `Category.values()` |
| `DataInitializer` | Each product is assigned a `Category` constant |
