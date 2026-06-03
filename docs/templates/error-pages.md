# Template: error.html and error/404.html

**Files:**  
- `src/main/resources/templates/error.html`  
- `src/main/resources/templates/error/404.html`  

**Owner:** HeenuReet  
**Controller:** `GlobalExceptionHandler.java`  
**Purpose:** User-facing error pages — `error.html` for 500-level errors, `404.html` for not-found errors.

---

## error.html — General Error Page

```html
<div class="text-center py-5">
    <i class="bi bi-exclamation-triangle-fill display-1 text-danger"></i>
    <h2 class="mt-3">Something went wrong</h2>
    <p class="text-muted" th:text="${message}">An unexpected error occurred.</p>
    <a th:href="@{/}" class="btn btn-primary mt-3">Go Home</a>
</div>
```

`${message}` comes from `GlobalExceptionHandler`:
```java
model.addAttribute("message", e.getMessage());
return "error";
```

If `message` is null (no message), `th:text` replaces with empty string. The fallback `"An unexpected error occurred."` shows only in IDE previews.

---

## error/404.html — Not Found Page

```html
<div class="text-center py-5">
    <h1 class="display-1 text-muted fw-bold">404</h1>
    <h3>Page Not Found</h3>
    <p class="text-muted">The page you're looking for doesn't exist.</p>
    <a th:href="@{/}" class="btn btn-primary">Back to Home</a>
</div>
```

**Spring Boot Whitelabel Error Page** is replaced by this template because:
1. `GlobalExceptionHandler` handles `NoResourceFoundException` and returns `"error/404"`
2. Spring Boot also auto-maps `/error` path — if a 404 slips through, Boot renders `templates/error.html`

### `@{/}` — Root URL

`th:href="@{/}"` generates `href="/"`. The `/` mapping redirects to `/login` or `/customer/dashboard` based on session in `AuthController`:
```java
@GetMapping("/")
public String home(HttpSession session) {
    if (session.getAttribute("loggedInUser") != null)
        return "redirect:/customer/dashboard";
    return "redirect:/login";
}
```

---

## No Navbar on Error Pages

Error pages do not include the navbar fragment. This is intentional — if the error occurred during session/user loading, trying to render the navbar (which reads `currentUser`) could cause another error.

---

## How Errors Flow to These Templates

```
Any Controller throws RuntimeException
  │
  GlobalExceptionHandler.handleRuntime()
    → model.addAttribute("message", e.getMessage())
    → @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    → return "error"
  │
  Thymeleaf renders templates/error.html
  (HTTP 500 response sent to browser)
```

```
Request to /products/99999 (not found)
  │
  ProductController throws RuntimeException("Product not found: 99999")
  OR Spring throws NoResourceFoundException (path doesn't match any controller)
  │
  GlobalExceptionHandler
    → RuntimeException → error.html
    → NoResourceFoundException → error/404.html
```
