# OrderItem.java

**File:** `src/main/java/com/shopping/system/entity/OrderItem.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** JPA Entity  
**Purpose:** A single line item in an order — which product was ordered, how many units, at what price. Price and subtotal are captured at order time and never change, even if the product's price changes later. This is an immutable snapshot of the transaction.

---

## Why Is Price Stored on OrderItem (Not Just Read from Product)?

This is a critical design decision. If we only stored `product_id` and `quantity`, then:
- When the admin changes the iPhone price from ₹79,999 to ₹74,999
- All historical orders would suddenly show the new price
- Order totals would be wrong
- This would be illegal for receipts/invoices

By storing `price` in `order_items`, each order is a **permanent snapshot** of prices at the time of purchase.

---

## Fields

### `order`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private Order order;
```
FK to the parent order. Many items belong to one order. LAZY loading prevents cascading DB hits.

---

### `product`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = false)
private Product product;
```
Reference to the product. LAZY — loaded only when explicitly accessed (e.g. to display product name in a template). For admin order history, `JOIN FETCH oi.product` in the repository query pre-loads it.

---

### `quantity`, `price`, `subtotal`
```java
@Column(nullable = false)
private Integer quantity;

@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal price;

@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal subtotal;
```
Same pattern as `CartItem`. Subtotal = price × quantity, auto-calculated.

---

## Lifecycle Method

```java
@PrePersist
@PreUpdate
public void calculateSubtotal() {
    if (price != null && quantity != null) {
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

Ensures subtotal is always consistent in the DB. Called before INSERT and UPDATE.

---

## Constructor

```java
public OrderItem(Order order, Product product, Integer quantity, BigDecimal price) {
    this.order = order;
    this.product = product;
    this.quantity = quantity;
    this.price = price;
    this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
}
```

Used in `OrderService.createOrderFromCart()`:
```java
OrderItem orderItem = new OrderItem(order, product, qty, cartItem.getPrice());
order.getOrderItems().add(orderItem);
```

Note: `cartItem.getPrice()` (not `product.getPrice()`) is used — the price that was in the cart when added, not the current product price.

---

## Difference from CartItem

| Feature | CartItem | OrderItem |
|---------|----------|-----------|
| Purpose | Temporary shopping list | Permanent transaction record |
| Can be changed? | Yes (quantity, removal) | No (order is immutable) |
| Cleared after? | After order placed | Never deleted |
| Smart setters? | Yes (recalculate subtotal) | No (no modification expected) |

---

## Forward Linkage

| File | How |
|------|-----|
| `OrderItemRepository` | `findByOrderId()` |
| `OrderService` | Creates OrderItems from CartItems; restores product stock on cancel |
| `Order.orderItems` | OneToMany collection |
| `SalesAnalysisService` | Iterates items for category/product sales analysis |
| Templates | `orders/details.html` renders the items table |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| order | order_id | BIGINT | FK → orders.id, NOT NULL |
| product | product_id | BIGINT | FK → products.id, NOT NULL |
| quantity | quantity | INT | NOT NULL |
| price | price | DECIMAL(10,2) | NOT NULL |
| subtotal | subtotal | DECIMAL(10,2) | NOT NULL |
