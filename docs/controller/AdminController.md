# AdminController.java

**File:** `src/main/java/com/shopping/system/controller/AdminController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Mehwish  
**Type:** Spring MVC Controller  
**Purpose:** Handles the admin home dashboard. Aggregates metrics (total products, orders, customers, today's sales), low-stock alerts, recent orders, and recent feedback via `DashboardService`.

---

## Endpoint: `GET /admin/dashboard`

```java
@GetMapping("/admin/dashboard")
public String adminDashboard(Model model, HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user == null) return "redirect:/login";
    if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";

    model.addAttribute("currentUser", user);
    model.addAttribute("totalProducts",  dashboardService.getTotalProducts());
    model.addAttribute("totalOrders",    dashboardService.getTotalOrders());
    model.addAttribute("totalCustomers", dashboardService.getTotalCustomers());
    model.addAttribute("todaysSales",    dashboardService.getTodaysSales());
    model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts());
    model.addAttribute("recentOrders",   dashboardService.getRecentOrders());
    model.addAttribute("recentFeedback", dashboardService.getRecentFeedback());
    return "admin/dashboard";
}
```

**Role enforcement:**  
If a customer navigates directly to `/admin/dashboard`, they're redirected to the customer dashboard. `SessionInterceptor` only checks if a session exists; role checking is done here.

**`DashboardService` as facade:** This controller has a single dependency and a single method. All complexity is in the service. Controllers stay thin.

---

## Model Attributes → Template Variables

| Model Key | Template Variable | Dashboard Widget |
|-----------|-------------------|-----------------|
| `totalProducts` | `${totalProducts}` | Stat card |
| `totalOrders` | `${totalOrders}` | Stat card |
| `totalCustomers` | `${totalCustomers}` | Stat card |
| `todaysSales` | `${todaysSales}` | Stat card (₹ format) |
| `lowStockProducts` | `${lowStockProducts}` | Alert table (QOH < 5) |
| `recentOrders` | `${recentOrders}` | Recent orders table |
| `recentFeedback` | `${recentFeedback}` | Recent feedback widget |
