# Flow 01: Login Flow

**End-to-End Trace: User submits login form → session created → dashboard redirect**

---

## ASCII Flow Diagram

```
Browser                  Controller              Service            Repository          DB
  │                          │                      │                   │               │
  │  GET /login               │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │  (renders login.html)     │                      │                   │               │
  │<──────────────────────────│                      │                   │               │
  │                          │                      │                   │               │
  │  POST /login              │                      │                   │               │
  │  {username, password}     │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                     AuthController               │                   │               │
  │                    .login()                      │                   │               │
  │                          │  findByUsername()     │                   │               │
  │                          │─────────────────────────────────────────>│               │
  │                          │                      │     SELECT * FROM users           │
  │                          │                      │     WHERE username = ?────────────>│
  │                          │                      │                   │<──────────────│
  │                          │<─────────────────────────────────────────│               │
  │                          │                      │                   │               │
  │                    BCrypt.matches()              │                   │               │
  │                    (compares raw vs hash)        │                   │               │
  │                          │                      │                   │               │
  │                    session.setAttribute          │                   │               │
  │                    ("loggedInUser", user)        │                   │               │
  │                          │                      │                   │               │
  │  redirect:/admin/dashboard                       │                   │               │
  │  (or /customer/dashboard)                        │                   │               │
  │<──────────────────────────│                      │                   │               │
  │                          │                      │                   │               │
  │  GET /admin/dashboard     │                      │                   │               │
  │──────────────────────────>│ SessionInterceptor   │                   │               │
  │                    checks session exists         │                   │               │
  │                    AdminController checks role   │                   │               │
  │<──────────────────────────│                      │                   │               │
  │  admin/dashboard.html     │                      │                   │               │
```

---

## Step-by-Step Walkthrough

### Step 1: `GET /login` — Show Login Form

**File:** `AuthController.java`
```java
@GetMapping("/login")
public String loginPage(HttpSession session, Model model) {
    if (session.getAttribute("loggedInUser") != null)
        return "redirect:/customer/dashboard";   // already logged in
    return "login";
}
```

- If user already has a session, skip the form and redirect
- Returns view name `"login"` → Thymeleaf renders `templates/login.html`

**Template:** `login.html`
- `th:action="@{/login}"` → form posts to `/login`
- `th:if="${error}"` → shows flash error from a previous failed login attempt
- `th:if="${success}"` → shows "Registered successfully" flash from registration

---

### Step 2: `POST /login` — Process Credentials

**File:** `AuthController.java`
```java
@PostMapping("/login")
public String login(@RequestParam String username,
                    @RequestParam String password,
                    HttpSession session,
                    RedirectAttributes redirectAttributes) {
```

**Step 2a: Look up user in DB**
```java
Optional<User> userOpt = userService.findByUsername(username);
```

Calls: `UserService.findByUsername()` → `UserRepository.findByUsername(username)` →  
SQL: `SELECT * FROM users WHERE username = ?`

**Step 2b: User not found**
```java
if (userOpt.isEmpty()) {
    redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
    return "redirect:/login";
}
```

Intentionally same error message as wrong password (prevents username enumeration — attacker can't tell whether username exists).

**Step 2c: Verify password**
```java
User user = userOpt.get();
if (!userService.checkPassword(rawPassword, user.getPassword())) {
    redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
    return "redirect:/login";
}
```

`userService.checkPassword()` calls `BCryptPasswordEncoder.matches(rawPassword, storedHash)`.

BCrypt re-hashes the raw password with the salt embedded in `storedHash` and compares.

**Step 2d: Create session**
```java
session.setAttribute("loggedInUser", user);
session.setMaxInactiveInterval(30 * 60);  // 30-minute timeout
```

`session` is an `HttpSession` — Spring automatically creates one and sends a `JSESSIONID` cookie to the browser.

**Step 2e: Redirect by role**
```java
if (user.getRole() == UserRole.ADMIN)
    return "redirect:/admin/dashboard";
return "redirect:/customer/dashboard";
```

POST-Redirect-GET pattern: the redirect causes the browser to make a new GET request, preventing form resubmission on refresh.

---

### Step 3: `SessionInterceptor` — Every Subsequent Request

**File:** `SessionInterceptor.java`
```java
@Override
public boolean preHandle(HttpServletRequest request, ...) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("loggedInUser") == null) {
        response.sendRedirect("/login");
        return false;   // stop processing
    }
    return true;        // continue to controller
}
```

`getSession(false)` — does NOT create a new session if none exists (unlike `getSession()` which creates one). Returns `null` if no session.

Whitelist: `/login`, `/register`, `/logout`, `/css/*`, `/js/*` bypass this check.

---

### Step 4: Dashboard Loads

**Admin path:**  
`AdminController.adminDashboard()` reads `dashboardService.getTotalProducts()`, etc. and returns `"admin/dashboard"`.

**Customer path:**  
`CustomerController.customerDashboard()` loads recent orders and returns `"customer/dashboard"`.

---

## What's in the Session

After login, `HttpSession` contains:
```
"loggedInUser" → User object (entity with id, username, email, role, ...)
```

Every controller that needs the current user reads:
```java
User user = (User) session.getAttribute("loggedInUser");
```

---

## Flash Attributes (for error display)

```
POST /login (wrong password)
  │
  └── redirectAttributes.addFlashAttribute("error", "Invalid...")
        │
        └── stored in session temporarily
              │
              redirect → GET /login
                │
                └── model has "error" key → login.html shows red alert
                      │
                      └── flash cleared from session (one-time)
```

Flash attributes survive exactly one redirect and are then gone.

---

## Files Involved

| File | Role |
|------|------|
| `login.html` | Login form (th:action, th:if flash) |
| `AuthController.java` | Handles GET/POST /login |
| `UserService.java` | `findByUsername()`, `checkPassword()` |
| `UserRepository.java` | `findByUsername()` DB query |
| `SessionInterceptor.java` | Guards all subsequent requests |
| `AdminController.java` / `CustomerController.java` | Serves dashboard after redirect |
