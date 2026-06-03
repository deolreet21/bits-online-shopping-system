# Template: orders/history.html

**File:** `src/main/resources/templates/orders/history.html`  
**Owner:** HeenuReet  
**Controller:** `OrderController.java` → `GET /orders`  
**Purpose:** Order history table — serves both customers (own orders) and admins (all orders). Dynamic heading, conditional "Customer" column for admins, status badges, and view detail links.

---

## Dynamic Heading Based on Role

```html
<span th:text="${currentUser.role.name() == 'ADMIN'} ? 'All Orders' : 'My Orders'">Orders</span>
```

Ternary in Thymeleaf: `condition ? 'valueIfTrue' : 'valueIfFalse'`. Same template renders "All Orders" for admin, "My Orders" for customer.

---

## Conditional "Customer" Column

```html
<th th:if="${currentUser.role.name() == 'ADMIN'}">Customer</th>
...
<td th:if="${currentUser.role.name() == 'ADMIN'}"
    th:text="${order.user.username}"></td>
```

The Customer column (header + cell) only appears for admin. The same `th:if` on both `<th>` and `<td>` keeps columns aligned.

`order.user.username` — works because `OrderRepository.findAllOrdersWithItems()` JOIN FETCHes `o.user`. No lazy loading issue.

---

## `#lists.size()` for Item Count

```html
<td th:text="${#lists.size(order.orderItems)} + ' item(s)'"></td>
```

`#lists.size()` is a Thymeleaf built-in that calls `.size()` on a `List`. The `+ ' item(s)'` concatenation is done server-side in the expression.

---

## Status Badge

```html
<span th:class="${'badge status-' + order.status.name().toLowerCase()}"
      th:text="${order.status}"></span>
```

Dynamic class → CSS in `main.css` colors it: green for DELIVERED, orange for PENDING, etc.  
`th:text="${order.status}"` — calls `.toString()` on the `OrderStatus` enum, which defaults to the constant name (`"PENDING"`, `"DELIVERED"`, etc.).

---

## URL Expression with Path Variable

```html
<a th:href="@{/orders/{id}(id=${order.id})}">View</a>
```

`@{/orders/{id}(id=${order.id})}` generates `/orders/42`. The syntax: path variable in `{curly}` braces, value provided as `(varName=expression)`.

---

## Why No Cancel Button Here

The cancel functionality is in `orders/details.html` (single order detail page) and `orders/cancel-confirm.html`. The history table is read-only — users click "View" to see details and cancel from there.

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `orders` | `List<Order>` | `OrderService` (all or user's only) |
| `currentUser` | `User` | Session |
| `success` | `String` | Flash (e.g., after cancel) |
