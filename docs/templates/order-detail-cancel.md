# Templates: Order Detail Pages

**Files:**  
- `src/main/resources/templates/orders/details.html`  
- `src/main/resources/templates/orders/cancel-confirm.html`  

**Owner:** HeenuReet  
**Controller:** `OrderController.java` → `GET /orders/{id}`, `GET /orders/{id}/cancel-confirm`

---

## orders/details.html — Order Detail Page

Shows full order information: status, date, shipping address, items table, total, and cancel button (if PENDING).

### Status Badge

```html
<span th:class="${'badge fs-5 status-' + order.status.name().toLowerCase()}"
      th:text="${order.status.name()}"></span>
```

Same dynamic class pattern as other templates.

### Items Table

```html
<tr th:each="item : ${order.orderItems}">
    <td>
        <a th:href="@{/products/{id}(id=${item.product.id})}"
           th:text="${item.product.name}"></a>
    </td>
    <td class="text-center" th:text="${item.quantity}"></td>
    <td class="text-end"
        th:text="'₹' + ${#numbers.formatDecimal(item.price, 1, 2)}"></td>
    <td class="text-end fw-bold"
        th:text="'₹' + ${#numbers.formatDecimal(item.subtotal, 1, 2)}"></td>
</tr>
```

Product names are clickable links to the product detail page (`/products/{id}`). `item.product` is available because `orderService.getOrderWithDetails()` uses JOIN FETCH.

### Cancel Button

```html
<form th:if="${order.status.name() == 'PENDING' and currentUser.role.name() == 'CUSTOMER'}"
      th:action="@{/orders/{id}/cancel(id=${order.id})}" method="post">
    <button type="submit" class="btn btn-outline-danger"
            onclick="return confirm('Are you sure you want to cancel this order?')">
        Cancel Order
    </button>
</form>
```

Two conditions with `and`:
1. Order must be PENDING
2. User must be a CUSTOMER (admin manages status via the status dropdown in history.html)

**`return confirm(...)`** — JavaScript confirmation dialog. Stops form submission if user clicks Cancel.

### Admin Status Dropdown (in details.html)

```html
<form th:if="${currentUser.role.name() == 'ADMIN'}"
      th:action="@{/orders/{id}/status(id=${order.id})}" method="post">
    <select name="status">
        <option th:each="s : ${orderStatuses}"
                th:value="${s.name()}"
                th:text="${s.name()}"
                th:selected="${s == order.status}"></option>
    </select>
    <button type="submit">Update Status</button>
</form>
```

Admin sees the status dropdown on the detail page too (in addition to history.html).

---

## orders/cancel-confirm.html — Cancel Confirmation Page

A standalone confirmation page before the actual cancel.

```html
<div class="card text-center">
    <h4>Cancel Order #<span th:text="${order.id}"></span>?</h4>
    <p class="text-muted">
        This order for ₹<span th:text="${order.totalAmount}"></span> will be cancelled.
        This cannot be undone.
    </p>
    <form th:action="@{/orders/{id}/cancel(id=${order.id})}" method="post">
        <button type="submit" class="btn btn-danger">Yes, Cancel</button>
    </form>
    <a th:href="@{/orders/{id}(id=${order.id})}" class="btn btn-secondary">Go Back</a>
</div>
```

Some flows use `GET /orders/{id}/cancel-confirm` → shows this page → user clicks confirm → `POST /orders/{id}/cancel`. The `onclick="return confirm(...)"` inline dialog in other templates is an alternative UI pattern — both lead to the same POST endpoint.

---

## Model Attributes Expected

| Template | Attribute | Source |
|----------|-----------|--------|
| `details.html` | `order` (with items), `orderStatuses`, `currentUser` | OrderService, OrderStatus.values(), Session |
| `cancel-confirm.html` | `order` | OrderService.getOrderById() |
