# Template: admin/dashboard.html

**File:** `src/main/resources/templates/admin/dashboard.html`  
**Owner:** Mehwish  
**Controller:** `AdminController.java` → `GET /admin/dashboard`  
**Purpose:** Admin home dashboard — 4 metric stat cards (products, orders, customers, today's sales), low-stock alert table, recent orders table, and recent feedback widget.

---

## Stat Cards

```html
<div class="card text-white bg-primary h-100">
    <div class="card-body">
        <h6 class="card-title">Total Products</h6>
        <h2 th:text="${totalProducts}">0</h2>
    </div>
</div>
```

4 cards in a Bootstrap row: `bg-primary` (blue), `bg-success` (green), `bg-info` (teal), `bg-warning` (yellow).

`th:text="${totalProducts}"` — replaced by the integer from `DashboardService.getTotalProducts()`. The `0` fallback shows in IDE/preview only.

---

## Low-Stock Alert Table

```html
<div th:if="${#lists.isEmpty(lowStockProducts)}" class="text-success">
    <i class="bi bi-check-circle"></i> All products are well-stocked.
</div>

<table th:if="${!#lists.isEmpty(lowStockProducts)}" class="table table-sm">
    <tr th:each="product : ${lowStockProducts}">
        <td th:text="${product.name}"></td>
        <td>
            <span class="badge bg-danger"
                  th:text="${product.quantityOnHand} + ' left'"></span>
        </td>
    </tr>
</table>
```

`lowStockProducts` = products with `quantityOnHand < 5` (from `DashboardService.getLowStockProducts()`). Shows a green "all good" message or a table of products needing restocking.

---

## Recent Orders Table

```html
<tr th:each="order : ${recentOrders}">
    <td th:text="'#' + ${order.id}"></td>
    <td th:text="${order.user.username}"></td>
    <td th:text="${#temporals.format(order.orderDate, 'dd MMM')}"></td>
    <td th:text="'₹' + ${#numbers.formatDecimal(order.totalAmount, 1, 2)}"></td>
    <td>
        <span th:class="${'badge status-' + order.status.name().toLowerCase()}"
              th:text="${order.status}"></span>
    </td>
</tr>
```

`recentOrders` = last 10 orders from `DashboardService.getRecentOrders()`. Shows order ID, customer name, date, amount, and status badge.

---

## Recent Feedback Widget

```html
<div th:each="feedback : ${recentFeedback}" class="mb-3 pb-2 border-bottom">
    <div class="d-flex justify-content-between">
        <strong th:text="${feedback.user.username}"></strong>
        <span class="text-warning">
            <span th:each="i : ${#numbers.sequence(1, feedback.rating)}">★</span>
        </span>
    </div>
    <p class="mb-0 small text-muted" th:text="${feedback.comment}"></p>
</div>
```

**`#numbers.sequence(1, feedback.rating)`** — generates a sequence `[1, 2, 3]` for a 3-star rating. `th:each` iterates it to render ★★★. A 5-star feedback renders ★★★★★.

---

## `container-fluid` vs `container`

```html
<div class="container-fluid mt-4 px-4">
```

`container-fluid` = full width of the viewport. Dashboard uses full width for the metric cards to span the entire screen. Most other pages use `container` (fixed max-width with auto margins).

---

## Model Attributes Expected

| Attribute | Type | Value Source |
|-----------|------|-------------|
| `totalProducts` | `long` | `productRepository.count()` |
| `totalOrders` | `long` | `orderRepository.count()` |
| `totalCustomers` | `long` | users with CUSTOMER role |
| `todaysSales` | `BigDecimal` | SUM of today's DELIVERED orders |
| `lowStockProducts` | `List<Product>` | QOH < 5 |
| `recentOrders` | `List<Order>` | Last 10 orders |
| `recentFeedback` | `List<Feedback>` | Last 5 feedback items |
| `currentUser` | `User` | Session |
