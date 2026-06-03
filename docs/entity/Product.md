# Product.java

**File:** `src/main/java/com/shopping/system/entity/Product.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** Aliya  
**Type:** JPA Entity  
**Purpose:** Represents a product in the store catalogue. Stored in the `products` table. Referenced by CartItem and OrderItem. Managed by admins (CRUD), browsed by customers.

---

## Class-Level Annotations

```java
@Entity
@Table(name = "products")
```

| Annotation | Reason |
|-----------|--------|
| `@Entity` | Marks this as a JPA-managed class mapped to a DB table |
| `@Table(name = "products")` | Explicitly names the table. Without this, Hibernate would use the class name `product` which could clash with SQL reserved words in some dialects. |

---

## Fields

### `id`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
Auto-incrementing primary key. `GenerationType.IDENTITY` delegates to MySQL's `AUTO_INCREMENT`.

---

### `name`
```java
@Column(nullable = false, length = 100)
private String name;
```
Product name is mandatory (`nullable = false`) and capped at 100 characters. Used in search queries: `findByNameContainingIgnoreCase`.

---

### `description`
```java
@Column(columnDefinition = "TEXT")
private String description;
```
`columnDefinition = "TEXT"` creates a MySQL `TEXT` column instead of `VARCHAR`. `TEXT` can hold up to 65,535 bytes — necessary for long product descriptions. A regular `@Column` defaults to `VARCHAR(255)` which would truncate long descriptions.

---

### `price`
```java
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal price;
```
| Attribute | Reason |
|-----------|--------|
| `precision = 10` | Total digits: up to 99,999,999.99 (₹10 crore max price) |
| `scale = 2` | Always two decimal places for currency (paise) |

**Why `BigDecimal` not `double`?**  
`double` has floating-point precision errors (e.g. `0.1 + 0.2 = 0.30000000000000004`). For financial calculations, `BigDecimal` is exact. A price of ₹45,999.00 must never become ₹45,998.99999.

---

### `quantityOnHand`
```java
@Column(name = "quantity_on_hand", nullable = false)
private Integer quantityOnHand;
```
`name = "quantity_on_hand"` — maps to snake_case DB column. Tracks available stock. Decremented in `OrderService.createOrderFromCart()` and restored in `cancelOrder()`.

---

### `category`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Category category;
```
Stored as a string (`"ELECTRONICS"`, `"BOOKS"`, etc.) for the same reasons as `UserRole` — stable across refactoring, human-readable in the DB.

---

### `createdDate` and `updatedDate`
```java
@Column(name = "created_date")
private LocalDateTime createdDate;

@Column(name = "updated_date")
private LocalDateTime updatedDate;
```
Both auto-managed via lifecycle methods. `createdDate` is set once; `updatedDate` is refreshed on every update. Useful for admin reporting and audit trails.

---

## Lifecycle Methods

```java
@PrePersist
public void prePersist() {
    this.createdDate = LocalDateTime.now();
    this.updatedDate = LocalDateTime.now();
}

@PreUpdate
public void preUpdate() {
    this.updatedDate = LocalDateTime.now();
}
```

| Annotation | When Triggered | What It Does |
|-----------|---------------|--------------|
| `@PrePersist` | Before first INSERT | Sets both timestamps |
| `@PreUpdate` | Before each UPDATE | Refreshes only `updatedDate` |

This pattern ensures timestamps are always accurate without the service layer needing to handle them.

---

## Relationships (Declared on Other Entities)

| Entity | Relationship | FK Column |
|--------|-------------|-----------|
| `CartItem` | ManyToOne to Product | `product_id` in `cart_items` |
| `OrderItem` | ManyToOne to Product | `product_id` in `order_items` |
| `Feedback` | ManyToOne to Product (nullable) | `product_id` in `feedback` |

Product is the **referenced** side — it doesn't hold any `@OneToMany` collections to avoid loading all orders/carts every time a product is fetched.

---

## Forward Linkage

| File | How It Uses Product |
|------|---------------------|
| `ProductRepository` | CRUD + search queries |
| `ProductService` | Business logic layer wrapping repository |
| `ProductController` | Browse/search for customers |
| `AdminProductController` | Create/edit/delete for admin |
| `CartService` | `addToCart(userId, product, qty)` |
| `OrderService` | Deducts `quantityOnHand` on order, restores on cancel |
| `DataInitializer` | Creates 30 seed products via `productRepository.saveAll()` |
| `SalesAnalysisService` | `findTopSellingProducts()` returns Product objects |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| name | name | VARCHAR(100) | NOT NULL |
| description | description | TEXT | nullable |
| price | price | DECIMAL(10,2) | NOT NULL |
| quantityOnHand | quantity_on_hand | INT | NOT NULL |
| category | category | VARCHAR(255) | NOT NULL |
| createdDate | created_date | DATETIME | nullable |
| updatedDate | updated_date | DATETIME | nullable |
