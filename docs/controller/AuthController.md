# AuthController.java

**File:** `src/main/java/com/shopping/system/controller/AuthController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring MVC Controller  
**Purpose:** Handles all authentication-related HTTP requests: showing the login and registration forms, processing credentials, creating/destroying the HTTP session, and redirecting to the appropriate dashboard based on role.

---

## Class-Level Annotation

```java
@Controller
public class AuthController { ... }
```

`@Controller` — Returns view names (template names) to be rendered by Thymeleaf. Contrast with `@RestController` which returns JSON. No `@RequestMapping` — each method has its own full path.

---

## Dependency

```java
@Autowired private UserService userService;
```

---

## Endpoints

### `GET /` — Root redirect
```java
@GetMapping("/")
public String home(HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user == null) return "redirect:/login";
    if (user.getRole() == UserRole.ADMIN) return "redirect:/admin/dashboard";
    return "redirect:/customer/dashboard";
}
```

The root URL is a redirect hub. No template is rendered. Checks session and routes to the right dashboard. First page loaded when user visits the site.

---

### `GET /login` — Show login form
```java
@GetMapping("/login")
public String loginForm(HttpSession session, Model model) {
    if (session.getAttribute("loggedInUser") != null) {
        User user = (User) session.getAttribute("loggedInUser");
        return user.getRole() == UserRole.ADMIN ? "redirect:/admin/dashboard" : "redirect:/customer/dashboard";
    }
    return "login";
}
```

If user is already logged in, redirects to their dashboard (prevents double login). Otherwise renders `templates/login.html`.

---

### `POST /login` — Process login
```java
@PostMapping("/login")
public String login(@RequestParam String username,
                    @RequestParam String password,
                    HttpSession session,
                    RedirectAttributes redirectAttributes) {
    Optional<User> userOpt = userService.loginUser(username, password);
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        session.setAttribute("loggedInUser", user);       // store user in session
        session.setMaxInactiveInterval(30 * 60);           // expire after 30 min idle
        return user.getRole() == UserRole.ADMIN ? "redirect:/admin/dashboard" : "redirect:/customer/dashboard";
    }
    redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
    return "redirect:/login";
}
```

**Session management:**
- `session.setAttribute("loggedInUser", user)` — stores the entire `User` object in the server-side session. All subsequent requests can access it with `session.getAttribute("loggedInUser")`.
- `session.setMaxInactiveInterval(30 * 60)` — session expires after 30 minutes of inactivity. The session is then invalidated by the servlet container.

**Flash attribute:** `redirectAttributes.addFlashAttribute("error", ...)` — stores the error message for ONE redirect. It disappears after being displayed once. This is the correct pattern for POST → redirect → GET to avoid re-submission on browser refresh.

---

### `GET /register` — Show registration form
```java
@GetMapping("/register")
public String registerForm(HttpSession session) {
    if (session.getAttribute("loggedInUser") != null) {
        return "redirect:/";
    }
    return "register";
}
```

Renders `templates/register.html`. Redirects already-logged-in users away.

---

### `POST /register` — Process registration
```java
@PostMapping("/register")
public String register(@RequestParam String username,
                       @RequestParam String email,
                       @RequestParam String password,
                       @RequestParam String confirmPassword,
                       @RequestParam(defaultValue = "CUSTOMER") UserRole role,
                       RedirectAttributes redirectAttributes) {
    if (!password.equals(confirmPassword)) { ... error ... }
    if (password.length() < 6) { ... error ... }
    try {
        userService.registerUser(username, email, password, role);
        redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
        return "redirect:/login";
    } catch (IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/register";
    }
}
```

**Validation (client + server):**
- Passwords must match (checked before calling service)
- Password min length 6 chars
- Username/email uniqueness checked in `UserService.registerUser()` — throws `IllegalArgumentException` caught here

`@RequestParam(defaultValue = "CUSTOMER")` — if the role field is missing from the form, defaults to CUSTOMER. Prevents server error if the form omits the field.

---

### `GET /logout`
```java
@GetMapping("/logout")
public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
    session.invalidate();
    redirectAttributes.addFlashAttribute("success", "You have been logged out successfully.");
    return "redirect:/login";
}
```

`session.invalidate()` — destroys the session and all its attributes. The browser's session cookie becomes invalid. On next request, `SessionInterceptor` finds no session and redirects to `/login`.

---

## Flash Attribute Pattern

```
POST /login                     POST /login (wrong password)
     │                               │
     ▼                               ▼
session.setAttribute(user)    redirectAttributes.addFlashAttribute("error", msg)
     │                               │
     ▼                               ▼
redirect:/customer/dashboard   redirect:/login
     │                               │
     ▼                               ▼
GET /customer/dashboard        GET /login
     │                               │
     ▼                               ▼
Renders dashboard              ${error} shown once, then gone
```

---

## Backward Linkage

| Dependency | Reason |
|------------|--------|
| `UserService` | `loginUser`, `registerUser` |
| `HttpSession` | Read/write session attributes |
| `RedirectAttributes` | Flash messages across redirects |
| `UserRole` | Role-based routing after login |
