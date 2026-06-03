# SessionInterceptor.java

**File:** `src/main/java/com/shopping/system/controller/SessionInterceptor.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring Component (HandlerInterceptor + WebMvcConfigurer)  
**Purpose:** Acts as a global authentication guard. Runs before every HTTP request is processed by a controller. If the user is not logged in (no session or no `loggedInUser` attribute), redirects to `/login`. Whitelists public paths (login, register, static files) so they are always accessible.

---

## Class-Level Annotations

```java
@Component
public class SessionInterceptor implements HandlerInterceptor, WebMvcConfigurer { ... }
```

| Annotation/Interface | Reason |
|---------------------|--------|
| `@Component` | Registers this as a Spring bean so it can be detected and injected |
| `implements HandlerInterceptor` | Gives it the `preHandle()` method called before every request |
| `implements WebMvcConfigurer` | Gives it `addInterceptors()` to register itself with Spring MVC |

By implementing both interfaces in one class, this component both defines the logic AND registers itself — no separate config class needed.

---

## `preHandle` Method

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String uri = request.getRequestURI();

    // Allow static resources and public pages
    if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
            || uri.startsWith("/static/") || uri.equals("/login") || uri.equals("/register")
            || uri.equals("/") || uri.startsWith("/webjars/") || uri.startsWith("/favicon")) {
        return true;
    }

    HttpSession session = request.getSession(false);  // false = don't create new session
    if (session == null || session.getAttribute("loggedInUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
    return true;
}
```

**Return value:**
- `return true` → proceed to controller
- `return false` → stop processing; redirect already sent

**`request.getSession(false)`** — the `false` parameter means "don't create a new session if one doesn't exist." Using `getSession()` or `getSession(true)` would create an empty session, which would then pass the null check and incorrectly allow access.

**Whitelist paths** — these never hit controller code, so they're allowed through:
- `/css/**`, `/js/**`, `/images/**`, `/static/**` — static resources (stylesheets, scripts)
- `/webjars/**` — Bootstrap and other library resources
- `/login`, `/register` — pre-auth pages
- `/` — root (AuthController handles the redirect logic there)
- `/favicon` — browser favicon request

---

## `addInterceptors` Method

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this)
            .excludePathPatterns("/login", "/register", "/css/**", "/js/**", "/images/**",
                    "/static/**", "/webjars/**", "/favicon.ico", "/");
}
```

This registers `this` interceptor with Spring MVC. `excludePathPatterns` mirrors the whitelist in `preHandle`. The redundancy is intentional — `addInterceptors` handles Spring's pattern-matching exclusion at the framework level, while the `preHandle` check is a safety fallback.

---

## Why Not Use Spring Security?

Full Spring Security would require:
- Security configuration class
- UserDetailsService implementation
- Password encoder in the security chain
- Role-based access rules using `HttpSecurity`
- CSRF tokens on all POST forms

For this project scope, `SessionInterceptor` provides session authentication with much less complexity, while `BCryptPasswordEncoder` (imported from `spring-security-crypto`) provides password hashing without the full security framework.

**Trade-off:** No built-in CSRF protection, no remember-me, no account lockout. Acceptable for a university project but not production-ready.

---

## Interaction with Other Controllers

Every controller starts with:
```java
User user = (User) session.getAttribute("loggedInUser");
if (user == null) return "redirect:/login";
```

This is **redundant** (SessionInterceptor already ensures user is present) but acts as a defensive check. If the interceptor is bypassed or misconfigured, the controller still handles it safely.

---

## Flow Diagram

```
Browser: GET /orders
          │
          ▼
SessionInterceptor.preHandle()
  uri = "/orders" → not in whitelist
  session = request.getSession(false)
          │
          ├── session null or no "loggedInUser"
          │     → response.sendRedirect("/login")
          │     → return false (stop)
          │
          └── session has "loggedInUser"
                → return true (proceed)
                      │
                      ▼
              OrderController.orderHistory()
```
