# SalesController.java

**File:** `src/main/java/com/shopping/system/controller/SalesController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Aliya  
**Type:** Spring MVC Controller  
**Purpose:** Admin-only sales analysis dashboard. Provides multiple time-period views (weekly, monthly, quarterly, yearly) and product movement analysis (fast/slow-moving products). All views are admin-protected.

---

## Class-Level Annotations

```java
@Controller
@RequestMapping("/admin/sales")
```

---

## Authorization Helper

```java
private boolean isAdmin(HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    return user != null && user.getRole() == UserRole.ADMIN;
}
```

---

## Endpoints

### `GET /admin/sales` — Sales Dashboard (Main)
```java
@GetMapping({"", "/"})   // handles both /admin/sales and /admin/sales/
public String salesDashboard(Model model, HttpSession session) {
    model.addAttribute("weeklySales",   salesAnalysisService.getWeeklySales());
    model.addAttribute("monthlySales",  salesAnalysisService.getMonthlySales());
    model.addAttribute("quarterlySales", salesAnalysisService.getQuarterlySales());
    model.addAttribute("yearlySales",   salesAnalysisService.getYearlySales());
    model.addAttribute("fastMovingProducts", salesAnalysisService.getFastMovingProducts());
    model.addAttribute("slowMovingProducts", salesAnalysisService.getSlowMovingProducts());
    model.addAttribute("salesByCategory",    salesAnalysisService.getSalesByCategory());
    return "admin/sales/dashboard";
}
```

All four period summaries + product movement + category breakdown in one view.

---

### Period-Specific Endpoints
```java
@GetMapping("/weekly")    → loads only weeklySales + sets period="Weekly"
@GetMapping("/monthly")   → loads only monthlySales + sets period="Monthly"
@GetMapping("/quarterly") → loads only quarterlySales + sets period="Quarterly"
@GetMapping("/yearly")    → loads only yearlySales + sets period="Yearly"
```

All reuse `admin/sales/dashboard` template with a `period` model attribute that the template uses to show/hide sections.

---

### `GET /admin/sales/products` — Product Movement
```java
@GetMapping("/products")
public String productSales(Model model, HttpSession session) {
    model.addAttribute("fastMovingProducts", salesAnalysisService.getFastMovingProducts());
    model.addAttribute("slowMovingProducts", salesAnalysisService.getSlowMovingProducts());
    return "admin/sales/dashboard";
}
```

Focused product sales view — top sellers and slow-movers side by side.

---

## Template: `admin/sales/dashboard.html`

Template variables used:
- `${weeklySales}`, `${monthlySales}`, `${quarterlySales}`, `${yearlySales}` — BigDecimal formatted as currency
- `${fastMovingProducts}` — `List<Map<String, Object>>` where each map has `"product"` and `"totalSold"` keys
- `${salesByCategory}` — `Map<String, BigDecimal>` iterated with `th:each="entry : ${salesByCategory}"`
