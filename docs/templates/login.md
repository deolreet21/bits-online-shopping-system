# Template: login.html

**File:** `src/main/resources/templates/login.html`  
**Owner:** HeenuReet  
**Controller:** `AuthController.java` → `GET /login`  
**Purpose:** Login form — username + password input with flash message display for errors and success notifications.

---

## No Navbar

This template does not include `th:replace="fragments/navbar :: navbar"`. Login and register pages are standalone — no navigation bar shown before the user is authenticated.

---

## Thymeleaf Namespace

```html
<html xmlns:th="http://www.thymeleaf.org" lang="en">
```

`xmlns:th` declares the Thymeleaf namespace. All `th:*` attributes are Thymeleaf directives processed server-side before the HTML is sent to the browser.

---

## CSS Loading

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"/>
<link rel="stylesheet" th:href="@{/css/main.css}"/>
```

**Bootstrap** — loaded from CDN (external URL, no `th:` needed).  
**main.css** — loaded via `th:href="@{/css/main.css}"`. The `@{...}` is a Thymeleaf URL expression that generates the correct path regardless of context root. Produces `href="/css/main.css"`.

---

## Flash Message Display

```html
<div th:if="${error}" class="alert alert-danger ...">
    <span th:text="${error}">Error</span>
</div>

<div th:if="${success}" class="alert alert-success ...">
    <span th:text="${success}">Success</span>
</div>
```

**`th:if="${error}"`** — only renders the `<div>` if the `error` model attribute is present and non-null. The model gets `error` from `redirectAttributes.addFlashAttribute("error", "...")` in `AuthController`.

**`th:text="${error}"`** — replaces the element's text content with the value. The fallback text `"Error"` is shown in IDE previews but never sent to the browser.

---

## Form

```html
<form th:action="@{/login}" method="post" id="loginForm">
    <input type="text" name="username" required autofocus/>
    <input type="password" name="password" autocomplete="current-password" required/>
    <button type="submit">Login</button>
</form>
```

**`th:action="@{/login}"`** — generates `action="/login"`. Use `th:action` not plain `action` because Thymeleaf may add a `_csrf` token if CSRF is enabled (it's not in this project, but the pattern is consistent).

**`required`** — HTML5 attribute. Prevents form submission if the field is empty (browser-side validation).  
**`autofocus`** — cursor lands on username field when page loads.  
**`autocomplete="current-password"`** — tells the browser's password manager this is a login password (vs `"new-password"` for registration).

---

## validation.js

```html
<script th:src="@{/js/validation.js}"></script>
```

Loaded at the bottom so it doesn't block page rendering. `th:src` generates `/js/validation.js`. Adds real-time email/required validation and double-submit prevention.

---

## Model Attributes Expected

| Attribute | Source | Purpose |
|-----------|--------|---------|
| `error` | `redirectAttributes.addFlashAttribute("error", ...)` | Show red alert |
| `success` | `redirectAttributes.addFlashAttribute("success", ...)` | Show green alert (after registration) |
