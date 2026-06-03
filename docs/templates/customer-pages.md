# Templates: Customer Dashboard & Profile

**Files:**  
- `src/main/resources/templates/customer/dashboard.html`  
- `src/main/resources/templates/customer/profile.html`  

**Owner:** HeenuReet  
**Controller:** `CustomerController.java` → `GET /customer/dashboard`, `GET /customer/profile`

---

## customer/dashboard.html — Customer Home

Shows a welcome card, recent order summary cards, and quick-action buttons.

### Welcome Card

```html
<h2>Welcome back, <span th:text="${currentUser.username}">User</span>!</h2>
```

`currentUser` from session via `model.addAttribute("currentUser", user)`.

### Recent Orders

```html
<div th:if="${recentOrders.isEmpty()}" class="text-center text-muted py-4">
    <p>You haven't placed any orders yet.</p>
    <a th:href="@{/products}" class="btn btn-primary">Start Shopping</a>
</div>

<div th:each="order : ${recentOrders}" class="card mb-2">
    <div class="card-body d-flex justify-content-between">
        <div>
            <strong th:text="'Order #' + ${order.id}"></strong>
            <small class="text-muted ms-2"
                   th:text="${#temporals.format(order.orderDate, 'dd MMM yyyy')}"></small>
        </div>
        <div>
            <span th:class="${'badge status-' + order.status.name().toLowerCase()}"
                  th:text="${order.status}"></span>
            <a th:href="@{/orders/{id}(id=${order.id})}" class="btn btn-sm btn-outline-primary ms-2">
                View
            </a>
        </div>
    </div>
</div>
```

`recentOrders` = last 5 of the user's orders (from `orderService.getUserOrders(userId)` limited to 5).

### Quick Action Buttons

```html
<a th:href="@{/products}" class="btn btn-primary">
    <i class="bi bi-grid me-2"></i>Browse Products
</a>
<a th:href="@{/cart}" class="btn btn-outline-secondary">
    <i class="bi bi-cart3 me-2"></i>View Cart
</a>
<a th:href="@{/orders}" class="btn btn-outline-secondary">
    <i class="bi bi-bag-check me-2"></i>All Orders
</a>
```

---

## customer/profile.html — Edit Profile

### Pre-Filled Form

```html
<form th:action="@{/customer/profile}" method="post">
    <input type="text" name="username" th:value="${currentUser.username}" required/>
    <input type="email" name="email" th:value="${currentUser.email}" required/>
    <input type="password" name="newPassword" placeholder="Leave blank to keep current"/>
    <button type="submit">Save Changes</button>
</form>
```

`th:value="${currentUser.username}"` — pre-fills current username. If user doesn't change it and submits, the same username is sent and the uniqueness check in `CustomerController.updateProfile()` skips it (it checks "is this username already taken by someone OTHER than me?").

**`newPassword` is optional** — `placeholder="Leave blank to keep current"`. No `required`. If blank, `CustomerController.updateProfile()` checks `!newPassword.isBlank()` before updating the password.

### Flash Messages

```html
<div th:if="${success}" class="alert alert-success">
    <span th:text="${success}"></span>
</div>
<div th:if="${error}" class="alert alert-danger">
    <span th:text="${error}"></span>
</div>
```

After profile update, controller redirects back to `GET /customer/profile` with flash attribute:
- Success: `"Profile updated successfully."`
- Error: `"Username already taken."` or `"Password must be at least 6 characters."`

### Session Update After Save

In `CustomerController.updateProfile()`, after saving:
```java
session.setAttribute("loggedInUser", userRepository.findById(user.getId()).get());
```

The session is updated so the navbar shows the new username immediately (without requiring re-login).

---

## Model Attributes Expected

| Template | Attribute | Source |
|----------|-----------|--------|
| `customer/dashboard.html` | `currentUser`, `recentOrders`, `cartCount` | Session, OrderService, CartService |
| `customer/profile.html` | `currentUser` | Session |
| both | `success` / `error` | Flash attributes |
