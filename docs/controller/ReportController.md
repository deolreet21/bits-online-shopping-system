# ReportController.java

**File:** `src/main/java/com/shopping/system/controller/ReportController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** Mehwish  
**Type:** Spring MVC Controller  
**Purpose:** Admin-only reporting. Provides date-range sales reports, product inventory reports, customer summaries (with order counts), and feedback reports. The most complex controller — injects 6 dependencies.

---

## Dependencies

```java
@Autowired private DashboardService dashboardService;
@Autowired private ProductService productService;
@Autowired private OrderService orderService;
@Autowired private FeedbackService feedbackService;
@Autowired private SalesAnalysisService salesAnalysisService;
@Autowired private UserRepository userRepository;
```

---

## Endpoints

### `GET /admin/reports/sales` — Date-Range Sales Report

```java
@GetMapping("/sales")
public String salesReport(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Model model, HttpSession session) {

    LocalDateTime start = (from != null) ? from.atStartOfDay() : LocalDateTime.now().minusMonths(1);
    LocalDateTime end   = (to   != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();

    var orders = salesAnalysisService.getOrdersBetween(start, end);
    var totalRevenue = orders.stream().map(Order::getTotalAmount)
                             .reduce(BigDecimal.ZERO, BigDecimal::add);
    var avgOrderValue = orders.isEmpty() ? BigDecimal.ZERO
                         : totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    ...
}
```

**`@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`** — tells Spring to parse the URL parameter `from=2024-01-01` as a `LocalDate` using ISO 8601 format.

**Default dates:** If not provided, defaults to last 30 days (start = 30 days ago, end = today). `to.atTime(23, 59, 59)` ensures the end date includes the entire last day.

**`RoundingMode.HALF_UP`** — standard rounding for financial calculations (5 rounds up: ₹12.345 → ₹12.35).

---

### `GET /admin/reports/customers` — Customer Summary

```java
@GetMapping("/customers")
public String customersReport(Model model, HttpSession session) {
    List<User> customers = userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.CUSTOMER)
            .collect(Collectors.toList());

    List<CustomerSummary> summaries = customers.stream()
            .map(u -> new CustomerSummary(u, orderService.getUserOrders(u.getId()).size()))
            .collect(Collectors.toList());

    long activeCustomers = summaries.stream().filter(s -> s.orderCount() > 0).count();
    ...
}
```

**N+1 note:** `orderService.getUserOrders(u.getId())` is called once per customer. If there are 100 customers, that's 100 DB queries. Acceptable for a university project; at scale, a single JOIN query would be used.

**`CustomerSummary` inner record:**
```java
public record CustomerSummary(User user, int orderCount) {}
```
Java `record` is a concise immutable data class. Generates constructor, getters (`user()`, `orderCount()`), `equals`, `hashCode`, and `toString` automatically. Used instead of a full class just to pass (user + count) to the template.

---

### Other Report Endpoints

| URL | Template | What It Shows |
|-----|----------|---------------|
| `/admin/reports` | `admin/reports/dashboard` | Links to all report types |
| `/admin/reports/products` | `admin/reports/inventory` | All products (delegates to `/inventory`) |
| `/admin/reports/inventory` | `admin/reports/inventory` | Product list with stock levels |
| `/admin/reports/feedback` | `admin/feedback` | All feedback list |

---

## Model Attributes for Sales Report

| Key | Type | Template Use |
|-----|------|-------------|
| `orders` | `List<Order>` | Order rows in report table |
| `totalOrderCount` | `int` | Summary stat |
| `totalRevenue` | `BigDecimal` | Summary stat (₹ formatted) |
| `avgOrderValue` | `BigDecimal` | Summary stat |
| `fromDate` | `LocalDate` | Pre-fills date filter input |
| `toDate` | `LocalDate` | Pre-fills date filter input |
