# Template: orders/confirmation.html

**File:** `src/main/resources/templates/orders/confirmation.html`  
**Owner:** HeenuReet  
**Controller:** `OrderController.java` → `GET /orders/confirmation/{id}`  
**Purpose:** Post-checkout confirmation page showing order ID, status badge, order date, shipping address, all items with quantities and prices, and order total.

---

## Status Badge with Dynamic Class

```html
<span th:class="${'badge fs-6 status-' + order.status.name().toLowerCase()}"
      th:text="${order.status}"></span>
```

**`th:class`** (not `th:classappend`) — replaces the entire class attribute.

`order.status.name()` → `"PENDING"`, `.toLowerCase()` → `"pending"`, concatenated → `"badge fs-6 status-pending"`.

In `main.css`:
```css
.status-pending { background-color: #ffc107; color: #000; }
```

The dynamic class pattern handles all 5 statuses without `th:if` branching.

---

## Date Formatting with #temporals

```html
<span th:text="${#temporals.format(order.orderDate, 'dd MMM yyyy, HH:mm')}"></span>
```

`order.orderDate` is a `LocalDateTime`. `#temporals.format()` formats it using Java date patterns:
- `dd` — 2-digit day (01, 15, 31)
- `MMM` — 3-letter month abbreviation (Jan, Feb, Mar)
- `yyyy` — 4-digit year
- `HH:mm` — 24-hour time

Result: `"03 Jun 2026, 14:35"`.

**Why `#temporals`?** Thymeleaf's standard dialect handles Java `Date`, but for `LocalDateTime` (Java 8+), the `thymeleaf-extras-java8time` module is needed. It's included automatically with `spring-boot-starter-thymeleaf`.

---

## Order Items Table

```html
<tr th:each="item : ${order.orderItems}">
    <td th:text="${item.product.name}"></td>
    <td class="text-center" th:text="${item.quantity}"></td>
    <td class="text-end"
        th:text="'$' + ${#numbers.formatDecimal(item.price, 1, 2)}"></td>
    <td class="text-end fw-semibold"
        th:text="'$' + ${#numbers.formatDecimal(item.subtotal, 1, 2)}"></td>
</tr>
```

`order.orderItems` — loaded by `orderService.getOrderWithDetails()` using `findByIdWithDetails()` which JOIN FETCHes orderItems + product in one query (avoids N+1 on this page).

---

## Why This Page Gets the Order from DB (Not Flash)

After `POST /orders/place`, the controller redirects:
```java
return "redirect:/orders/confirmation/" + order.getId();
```

The confirmation page fetches the order fresh from the DB:
```java
Order order = orderService.getOrderWithDetails(id);
```

**Why not pass via flash attribute?** Flash attributes can hold objects, but `Order` + its eager associations would be serialized into the session — potentially megabytes. Re-fetching from DB is cleaner and ensures the data is consistent.

---

## Navigation After Confirmation

```html
<a th:href="@{/orders}" class="btn btn-outline-primary">View All Orders</a>
<a th:href="@{/products}" class="btn btn-primary">Continue Shopping</a>
```

Two buttons: view order history or go back to shopping.

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `order` | `Order` (with orderItems + products) | `orderService.getOrderWithDetails()` |
| `currentUser` | `User` | Session |
