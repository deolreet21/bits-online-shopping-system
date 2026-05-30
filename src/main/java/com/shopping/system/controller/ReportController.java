// Owner: Mehwish | Reports | Admin reports controller — sales, inventory, customer activity
package com.shopping.system.controller;

import com.shopping.system.entity.Order;
import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    // Java records are ideal for read-only data transfer without boilerplate getters
    record CustomerSummary(User user, int orderCount) {}

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReportController(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** Reports hub — links to sales, inventory, and customer reports */
    @GetMapping
    public String reportsDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("loggedInUser", session.getAttribute("loggedInUser"));
        return "admin/reports/dashboard";
    }

    /**
     * Sales report with optional date-range filter.
     * @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) automatically parses ?from=2026-05-01 URL params
     * into Java LocalDate objects — no manual parsing needed.
     * Defaults to last 30 days when no range is provided.
     */
    @GetMapping("/sales")
    public String salesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session, Model model) {

        if (!isAdmin(session)) return "redirect:/login";

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null)   to   = LocalDate.now();

        LocalDateTime start = LocalDateTime.of(from, LocalTime.MIN);
        LocalDateTime end   = LocalDateTime.of(to,   LocalTime.MAX);

        List<Order> orders = orderRepository.findOrdersBetweenDates(start, end);

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // BigDecimal.divide() requires RoundingMode — without it, non-terminating decimals throw ArithmeticException
        BigDecimal avgOrderValue = orders.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        model.addAttribute("orders",        orders);
        model.addAttribute("totalRevenue",  totalRevenue);
        model.addAttribute("avgOrderValue", avgOrderValue);
        model.addAttribute("from",          from);
        model.addAttribute("to",            to);
        model.addAttribute("loggedInUser",  session.getAttribute("loggedInUser"));
        return "admin/reports/sales";
    }

    /** Inventory report — all products with category, price, and stock status */
    @GetMapping("/inventory")
    public String inventoryReport(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("products",     productRepository.findAll());
        model.addAttribute("loggedInUser", session.getAttribute("loggedInUser"));
        return "admin/reports/inventory";
    }

    /**
     * Customer activity report — joins all CUSTOMER-role users with their order count.
     * Uses stream mapping instead of a complex JOIN query to keep logic readable.
     */
    @GetMapping("/customers")
    public String customersReport(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";

        // Build CustomerSummary list: filter CUSTOMER role, then map each user to their order count
        List<CustomerSummary> summaries = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .map(u -> new CustomerSummary(u, orderRepository.findByUserId(u.getId()).size()))
                .toList();

        model.addAttribute("summaries",    summaries);
        model.addAttribute("loggedInUser", session.getAttribute("loggedInUser"));
        return "admin/reports/customers";
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}
