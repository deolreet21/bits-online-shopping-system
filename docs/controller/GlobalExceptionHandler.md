# GlobalExceptionHandler.java

**File:** `src/main/java/com/shopping/system/controller/GlobalExceptionHandler.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring Controller Advice  
**Purpose:** Centralized exception handler for the entire application. Instead of every controller having try-catch blocks for every exception, this class intercepts exceptions thrown from any controller and renders appropriate error pages with meaningful messages.

---

## Class-Level Annotation

```java
@ControllerAdvice
public class GlobalExceptionHandler { ... }
```

`@ControllerAdvice` — applied globally to all `@Controller` classes. Spring AOP intercepts unhandled exceptions from any controller and routes them to the matching `@ExceptionHandler` method here.

**Without `@ControllerAdvice`:** Every controller would need try-catch blocks, and the user would see a raw 500 error page on any unhandled exception.

---

## Exception Handlers

### `IllegalArgumentException` → 400 Bad Request

```java
@ExceptionHandler(IllegalArgumentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
    model.addAttribute("errorTitle", "Invalid Request");
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
}
```

**Thrown when:** Invalid input or resource not found.  
**Examples:**
- `ProductService.getById()` → "Product not found: 99"
- `OrderController.orderDetails()` → "You are not authorized to view this order"
- `UserService.registerUser()` → "Username already taken"

**Returns:** Renders `templates/error.html` with a user-friendly message and HTTP 400 status.

---

### `IllegalStateException` → 409 Conflict

```java
@ExceptionHandler(IllegalStateException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public String handleIllegalState(IllegalStateException ex, Model model) {
    model.addAttribute("errorTitle", "Operation Not Allowed");
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
}
```

**Thrown when:** A business rule is violated.  
**Examples:**
- `OrderService.createOrderFromCart()` → "Cart is empty. Cannot place order."
- `OrderService.cancelOrder()` → "Only PENDING orders can be cancelled."
- `OrderService.createOrderFromCart()` → "Insufficient stock for product: iPhone"

**HTTP 409 Conflict** is the appropriate status — the request was valid, but it conflicts with the current state of the resource.

---

### `NoHandlerFoundException` → 404 Not Found

```java
@ExceptionHandler(NoHandlerFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public String handleNotFound(NoHandlerFoundException ex, Model model) {
    model.addAttribute("errorTitle", "Page Not Found");
    model.addAttribute("errorMessage", "The page you're looking for does not exist.");
    return "error/404";
}
```

**Thrown when:** No controller mapping matches the URL (e.g., `/admin/typo`).  
Renders a custom 404 page instead of the generic Tomcat white-label error.

---

### `Exception` → 500 Internal Server Error (catch-all)

```java
@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public String handleGeneral(Exception ex, Model model) {
    model.addAttribute("errorTitle", "Unexpected Error");
    model.addAttribute("errorMessage", "Something went wrong. Please try again later.");
    return "error";
}
```

**Catch-all** — any unhandled exception that isn't one of the specific types above. The user sees a friendly message; the exception stack trace goes to the server log.

**Note:** The error message is intentionally vague — showing stack traces to users is a security risk.

---

## @ResponseStatus Annotation

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
```

Sets the HTTP response status code. Without this, Spring would default to 200 OK even when rendering an error page — which is incorrect and would confuse browsers and monitoring tools.

---

## How Exceptions Flow to Here

```
OrderController.orderDetails()
    │
    ├── orderService.getOrderById(id)
    │      └── returns Optional.empty()
    │              │
    │              └── .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id))
    │
    └── IllegalArgumentException propagates up (not caught in controller)
                │
                ▼
    GlobalExceptionHandler.handleIllegalArgument()
                │
                ▼
    Renders templates/error.html with errorTitle + errorMessage
                │
                ▼
    HTTP 400 returned to browser
```

---

## Template Used: `error.html` and `error/404.html`

Variables provided to templates:
- `${errorTitle}` — shown as the page heading
- `${errorMessage}` — shown as the description

The `error.html` template is a generic error page used for 400, 409, and 500.  
The `error/404.html` has custom copy specifically for "page not found."
