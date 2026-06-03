# Templates: Admin Reports & Sales Dashboards

**Files:**  
- `src/main/resources/templates/admin/reports/dashboard.html` — links to all report types  
- `src/main/resources/templates/admin/reports/sales.html` — date-range sales report  
- `src/main/resources/templates/admin/reports/inventory.html` — product stock levels  
- `src/main/resources/templates/admin/reports/customers.html` — customer order summaries  
- `src/main/resources/templates/admin/sales/dashboard.html` — sales analytics dashboard  

**Owner:** Aliya (sales), Mehwish (reports)  
**Controller:** `SalesController.java`, `ReportController.java`

---

## reports/dashboard.html — Index Page

Simple links page to navigate between report types:
- Sales Report (`/admin/reports/sales`)
- Product Inventory (`/admin/reports/inventory`)
- Customer Report (`/admin/reports/customers`)
- Feedback Report (`/admin/feedback`)

No dynamic model data — pure HTML links.

---

## reports/sales.html — Date-Range Sales Report

### Date Filter Form

```html
<form th:action="@{/admin/reports/sales}" method="get" class="row g-3">
    <div class="col-md-4">
        <input type="date" name="from" class="form-control"
               th:value="${#temporals.format(fromDate, 'yyyy-MM-dd')}"/>
    </div>
    <div class="col-md-4">
        <input type="date" name="to" class="form-control"
               th:value="${#temporals.format(toDate, 'yyyy-MM-dd')}"/>
    </div>
    <button type="submit">Filter</button>
</form>
```

**`type="date"`** — HTML5 date picker. Value format must be `yyyy-MM-dd`.  
**`#temporals.format(fromDate, 'yyyy-MM-dd')`** — formats `LocalDate` to `"2024-01-01"` for pre-filling. The `fromDate` model attribute is the applied filter (or default last-30-days).

**GET not POST** — date filters are query parameters. Using GET means the filtered URL is bookmarkable: `/admin/reports/sales?from=2024-01-01&to=2024-01-31`.

### Summary Cards

```html
<div class="col-md-4">
    <div class="card bg-primary text-white">
        <h2 th:text="${totalOrderCount}">0</h2>
        <small>Total Orders</small>
    </div>
</div>
<div class="col-md-4">
    <div class="card bg-success text-white">
        <h2 th:text="'₹' + ${#numbers.formatDecimal(totalRevenue, 1, 2)}">₹0</h2>
        <small>Total Revenue</small>
    </div>
</div>
```

Three cards: Total Orders, Total Revenue, Average Order Value.

### Orders Table

```html
<tr th:each="order : ${orders}">
    <td th:text="'#' + ${order.id}"></td>
    <td th:text="${order.user.username}"></td>
    <td th:text="${#temporals.format(order.orderDate, 'dd MMM yyyy HH:mm')}"></td>
    <td th:text="'₹' + ${#numbers.formatDecimal(order.totalAmount, 1, 2)}"></td>
    <td><span th:class="${'badge status-' + order.status.name().toLowerCase()}"
              th:text="${order.status}"></span></td>
</tr>
```

---

## reports/inventory.html — Stock Levels

```html
<tr th:each="product : ${products}">
    <td th:text="${product.name}"></td>
    <td th:text="${product.category.displayName}"></td>
    <td th:text="'₹' + ${#numbers.formatDecimal(product.price, 1, 2)}"></td>
    <td>
        <span th:class="${product.quantityOnHand < 5 ? 'badge bg-danger' :
                          (product.quantityOnHand < 20 ? 'badge bg-warning text-dark' :
                           'badge bg-success')}"
              th:text="${product.quantityOnHand}"></span>
    </td>
</tr>
```

Stock badge colors: red < 5, yellow < 20, green ≥ 20.  
Nested ternary in `th:class` for three-state logic.

---

## reports/customers.html — Customer Summary

```html
<tr th:each="summary : ${summaries}">
    <td th:text="${summary.user.username}"></td>
    <td th:text="${summary.user.email}"></td>
    <td th:text="${summary.orderCount}"></td>
    <td>
        <span th:class="${summary.orderCount > 0 ? 'badge bg-success' : 'badge bg-secondary'}"
              th:text="${summary.orderCount > 0 ? 'Active' : 'Inactive'}"></span>
    </td>
</tr>
```

`summaries` is `List<CustomerSummary>` where `CustomerSummary` is a Java record: `record CustomerSummary(User user, int orderCount)`.

Access: `summary.user()` (record getter) — or Thymeleaf accesses it as `summary.user` (handles both getter styles).

Summary stats:
```html
<p>Active: <strong th:text="${activeCustomers}"></strong></p>
<p>Total: <strong th:text="${#lists.size(summaries)}"></strong></p>
```

---

## sales/dashboard.html — Sales Analytics

### Conditional Section Display

```html
<!-- Shows all sections on main dashboard, or only relevant section on period endpoints -->
<div th:if="${weeklySales != null}" class="card mb-4">
    <h5>Weekly Sales</h5>
    <p>₹<span th:text="${#numbers.formatDecimal(weeklySales, 1, 2)}"></span></p>
</div>
```

The `period` model attribute controls which section is highlighted:
```html
<div th:if="${period == null or period == 'Weekly'}">...</div>
```

The main dashboard (`GET /admin/sales`) sets no `period` — all sections show. Period-specific endpoints (`/admin/sales/weekly`) set `period="Weekly"` — only weekly section shows.

### Fast/Slow Moving Products

```html
<tr th:each="entry : ${fastMovingProducts}">
    <td th:text="${entry['product'].name}"></td>
    <td th:text="${entry['totalSold']}"></td>
</tr>
```

`fastMovingProducts` is `List<Map<String, Object>>`. Each map has:
- `"product"` → `Product` entity
- `"totalSold"` → `Long` (total quantity sold)

`entry['product']` — map access via bracket notation in Thymeleaf (same as `entry.get('product')`).

### Sales by Category

```html
<tr th:each="entry : ${salesByCategory}">
    <td th:text="${entry.key}"></td>
    <td th:text="'₹' + ${#numbers.formatDecimal(entry.value, 1, 2)}"></td>
</tr>
```

`salesByCategory` is `Map<String, BigDecimal>`. `th:each` on a Map iterates `Map.Entry` objects — `entry.key` and `entry.value`.

---

## Model Attributes Summary

| Template | Key Attributes |
|----------|---------------|
| `reports/sales.html` | `orders`, `totalOrderCount`, `totalRevenue`, `avgOrderValue`, `fromDate`, `toDate` |
| `reports/inventory.html` | `products` |
| `reports/customers.html` | `summaries`, `activeCustomers` |
| `sales/dashboard.html` | `weeklySales`, `monthlySales`, `quarterlySales`, `yearlySales`, `fastMovingProducts`, `slowMovingProducts`, `salesByCategory`, `period` |
