// Owner: Mehwish | Reports | Admin reports for sales, products, customers, feedback, and inventory
package com.shopping.system.controller;

import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.repository.UserRepository;
import com.shopping.system.service.DashboardService;
import com.shopping.system.service.FeedbackService;
import com.shopping.system.service.OrderService;
import com.shopping.system.service.ProductService;
import com.shopping.system.service.SalesAnalysisService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    @Autowired private DashboardService dashboardService;
    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private FeedbackService feedbackService;
    @Autowired private SalesAnalysisService salesAnalysisService;
    @Autowired private UserRepository userRepository;

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @GetMapping({"", "/"})
    public String reportsDashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        return "admin/reports/dashboard";
    }

    @GetMapping("/sales")
    public String salesReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                              Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";

        LocalDateTime start = (from != null) ? from.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end   = (to   != null) ? to.atTime(23, 59, 59) : LocalDateTime.now();

        var orders = salesAnalysisService.getOrdersBetween(start, end);
        var totalRevenue = orders.stream()
                .map(o -> o.getTotalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var avgOrderValue = orders.isEmpty() ? java.math.BigDecimal.ZERO
                : totalRevenue.divide(java.math.BigDecimal.valueOf(orders.size()), 2, java.math.RoundingMode.HALF_UP);

        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrderCount", orders.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("avgOrderValue", avgOrderValue);
        model.addAttribute("fromDate", start.toLocalDate());
        model.addAttribute("toDate", end.toLocalDate());
        return "admin/reports/sales";
    }

    @GetMapping("/products")
    public String productsReport(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("products", productService.getAllProducts());
        return "admin/reports/inventory";
    }

    @GetMapping("/customers")
    public String customersReport(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";

        // Build a list of CustomerSummary records (user + orderCount)
        List<User> customers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .collect(Collectors.toList());

        List<CustomerSummary> summaries = customers.stream()
                .map(u -> new CustomerSummary(u, orderService.getUserOrders(u.getId()).size()))
                .collect(Collectors.toList());

        long activeCustomers = summaries.stream().filter(s -> s.orderCount() > 0).count();

        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("customers", summaries);
        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("activeCustomers", activeCustomers);
        return "admin/reports/customers";
    }

    @GetMapping("/feedback")
    public String feedbackReport(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("feedbackList", feedbackService.getAllFeedback());
        return "admin/feedback";
    }

    @GetMapping("/inventory")
    public String inventoryReport(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("products", productService.getAllProducts());
        return "admin/reports/inventory";
    }

    // Inner record to pass customer + order count to template
    public record CustomerSummary(User user, int orderCount) {}
}
