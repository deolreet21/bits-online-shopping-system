# Template: fragments/navbar.html

**File:** `src/main/resources/templates/fragments/navbar.html`  
**Owner:** HeenuReet  
**Purpose:** Reusable navbar fragment included in every authenticated page. Shows different nav links for CUSTOMER vs ADMIN roles, cart badge, user dropdown, and logout link.

---

## Thymeleaf Fragment Definition

```html
<nav th:fragment="navbar" class="navbar navbar-expand-lg navbar-dark bg-primary">
```

**`th:fragment="navbar"`** — declares this `<nav>` element as a named fragment. Other templates include it with:
```html
<div th:replace="~{fragments/navbar :: navbar}"></div>
```

`th:replace` removes the `<div>` and replaces it with the fragment. The result is the `<nav>` element directly in the page.

`~{fragments/navbar :: navbar}` — syntax: `~{template-name :: fragment-name}`. Template name resolves to `templates/fragments/navbar.html`.

---

## Role-Based Navigation

```html
<!-- Customer Nav -->
<ul th:if="${currentUser != null and currentUser.role.name() == 'CUSTOMER'}"
    class="navbar-nav me-auto">
    ...
</ul>

<!-- Admin Nav -->
<ul th:if="${currentUser != null and currentUser.role.name() == 'ADMIN'}"
    class="navbar-nav me-auto">
    ...
</ul>
```

`th:if` — only one `<ul>` renders. `currentUser.role.name()` calls `.name()` on the `UserRole` enum, returning `"CUSTOMER"` or `"ADMIN"`.

**Why `.name()` not `.toString()`?** Enum's `.name()` always returns the constant name exactly as declared (`"CUSTOMER"`). `.toString()` can be overridden to return something else (though it isn't here). `.name()` is safer.

---

## Cart Badge

```html
<span th:if="${cartCount != null and cartCount > 0}"
      class="badge bg-warning text-dark"
      th:text="${cartCount}">0</span>
```

`cartCount` is added to the model by `ProductController` and `CartController`:
```java
model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
```

The badge only appears if `cartCount > 0`. Customer sees a yellow number on the Cart nav link.

---

## User Dropdown

```html
<a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">
    <span th:text="${currentUser.username}">User</span>
    <span class="badge bg-light text-primary"
          th:text="${currentUser.role.name()}">ROLE</span>
</a>
<ul class="dropdown-menu dropdown-menu-end">
    <li><a class="dropdown-item" th:href="@{/logout}">Logout</a></li>
</ul>
```

Shows the username and role badge. `data-bs-toggle="dropdown"` is Bootstrap 5 JavaScript that opens the dropdown. `dropdown-menu-end` aligns the menu to the right edge.

---

## What `currentUser` Comes From

Every controller that renders an authenticated page adds:
```java
model.addAttribute("currentUser", user);
```

Where `user = (User) session.getAttribute("loggedInUser")`. The navbar fragment reads `${currentUser}` from this model attribute.

---

## Admin Nav Links

| Link | URL | Icon |
|------|-----|------|
| Dashboard | `/admin/dashboard` | `bi-speedometer2` |
| Products | `/admin/products` | `bi-box-seam` |
| Orders | `/orders` | `bi-clipboard-data` |
| Sales | `/admin/sales` | `bi-graph-up-arrow` |
| Reports | `/admin/reports` | `bi-file-earmark-bar-graph` |
| Feedback | `/admin/feedback` | `bi-chat-square-text` |

## Customer Nav Links

| Link | URL | Icon |
|------|-----|------|
| Dashboard | `/customer/dashboard` | `bi-speedometer2` |
| Products | `/products` | `bi-grid` |
| Cart | `/cart` | `bi-cart3` (+ badge) |
| My Orders | `/orders` | `bi-bag-check` |
| Feedback | `/feedback` | `bi-chat-square-dots` |
