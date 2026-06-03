# Template: register.html

**File:** `src/main/resources/templates/register.html`  
**Owner:** HeenuReet  
**Controller:** `AuthController.java` → `GET /register`  
**Purpose:** New user registration form — username, email, password, confirm password, and account type (role) selection.

---

## Fields

| Field | HTML Input | Validation |
|-------|-----------|------------|
| username | `text`, `minlength="3"`, `required` | Min 3 chars, unique check on server |
| email | `type="email"`, `required` | Browser email format + regex in validation.js |
| password | `type="password"`, `minlength="6"`, `required` | Min 6 chars |
| confirmPassword | `type="password"`, `required` | Server checks password == confirmPassword |
| role | `<select>` with CUSTOMER/ADMIN options | Required |

---

## Role Select Dropdown

```html
<select class="form-select" id="role" name="role" required>
    <option value="CUSTOMER" selected>Customer</option>
    <option value="ADMIN">Admin</option>
</select>
```

The `value` is the enum string (`"CUSTOMER"`, `"ADMIN"`) that gets sent to the server. `@RequestParam String role` in `AuthController.register()` receives it and calls `UserRole.valueOf(role)`.

**Note:** In production, allowing self-registration as ADMIN would be a security issue. For this university project, it's acceptable.

---

## `autocomplete="new-password"`

```html
<input type="password" name="password" autocomplete="new-password"/>
```

Tells the browser this is a **new** password (registration), not a login password. The browser's password manager offers to save it and generate a strong password, rather than autofilling an existing one.

---

## Server-Side Validation (in AuthController)

Even though HTML has `minlength="3"` and `required`, the server validates:
1. `username.length() < 3` → error flash
2. `userService.existsByUsername(username)` → error flash
3. `!password.equals(confirmPassword)` → error flash
4. `password.length() < 6` → error flash

Client-side validation can be bypassed; server-side is authoritative.

---

## After Successful Registration

```java
redirectAttributes.addFlashAttribute("success", "Account created! Please log in.");
return "redirect:/login";
```

The success flash appears on the `/login` page (`th:if="${success}"`).

---

## Model Attributes Expected

| Attribute | Source | Purpose |
|-----------|--------|---------|
| `error` | `redirectAttributes.addFlashAttribute("error", ...)` | Show red alert on validation failure |
