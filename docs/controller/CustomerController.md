# CustomerController.java

**File:** `src/main/java/com/shopping/system/controller/CustomerController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Mehwish  
**Type:** Spring MVC Controller  
**Purpose:** Handles the customer-specific pages: the customer home dashboard (showing cart count, order stats, recent orders) and the profile page (viewing and updating username, email, password).

---

## Dependencies

```java
@Autowired private CartService cartService;
@Autowired private OrderService orderService;
@Autowired private FeedbackService feedbackService;
@Autowired private UserRepository userRepository;      // direct repo — bypasses UserService for updates
@Autowired private BCryptPasswordEncoder passwordEncoder;
```

**Direct `UserRepository` injection** (bypassing `UserService`) — used in `updateProfile()` for a simpler save operation. Also needs `BCryptPasswordEncoder` to hash the new password.

---

## Endpoints

### `GET /customer/dashboard`
```java
@GetMapping("/customer/dashboard")
public String customerDashboard(Model model, HttpSession session) {
    if (user.getRole() == UserRole.ADMIN) return "redirect:/admin/dashboard";

    int cartCount = cartService.getCartItemCount(user.getId());
    List<Order> recentOrders = orderService.getUserOrders(user.getId()).stream().limit(5).toList();
    long totalOrders = orderService.getUserOrders(user.getId()).size();
    ...
}
```

**Note:** `getUserOrders()` is called twice — once for `recentOrders` (limit 5) and once for `totalOrders` (count). This could be optimized by calling once and deriving both. Trade-off: simplicity over performance.

**Admin guard:** Even though `SessionInterceptor` checks login, it doesn't check roles. If an admin somehow hits `/customer/dashboard`, they're redirected to admin dashboard.

---

### `GET /customer/profile`
```java
@GetMapping("/customer/profile")
public String profilePage(Model model, HttpSession session) {
    model.addAttribute("currentUser", user);
    model.addAttribute("orderCount", orderService.getUserOrders(user.getId()).size());
    model.addAttribute("feedbackCount", feedbackService.getFeedbackByUser(user.getId()).size());
    model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
    return "customer/profile";
}
```

Displays the profile form pre-filled with current user data, plus stat counts.

---

### `POST /customer/profile` — Update Profile

```java
@PostMapping("/customer/profile")
public String updateProfile(@RequestParam String username,
                            @RequestParam String email,
                            @RequestParam(required = false) String newPassword,
                            HttpSession session, RedirectAttributes redirectAttributes) {

    // Uniqueness check — skip if unchanged
    if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
        redirectAttributes.addFlashAttribute("errorMessage", "Username already taken.");
        return "redirect:/customer/profile";
    }
    if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
        redirectAttributes.addFlashAttribute("errorMessage", "Email already in use.");
        return "redirect:/customer/profile";
    }

    user.setUsername(username);
    user.setEmail(email);
    if (newPassword != null && !newPassword.isBlank()) {
        if (newPassword.length() < 6) { ... return error ... }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    userRepository.save(user);
    session.setAttribute("loggedInUser", user);  // update session with new data
    redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
    return "redirect:/customer/profile";
}
```

**Conditional uniqueness check:**  
`!user.getUsername().equals(username)` — if the username hasn't changed, we skip the uniqueness check. Otherwise, `existsByUsername(newUsername)` would return `true` for the current user's own username, incorrectly blocking the update.

**`session.setAttribute("loggedInUser", user)`** after save — the session still holds the old User object in memory. This updates the session with the modified User so the navbar shows the new username immediately.

**`newPassword` is `required = false`** — password is optional. If the field is empty/blank, the password is left unchanged.

---

## Model Attributes Summary

| Attribute | Type | Template Use |
|-----------|------|-------------|
| `currentUser` | `User` | Welcome message, form pre-fill |
| `cartCount` | `int` | Stat card on dashboard |
| `totalOrders` | `long` | Stat card on dashboard |
| `recentOrders` | `List<Order>` | Recent orders table |
| `orderCount` | `int` | Profile page stats |
| `feedbackCount` | `int` | Profile page stats |
| `successMessage` | Flash | Success alert |
| `errorMessage` | Flash | Error alert |
