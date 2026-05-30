// Owner: Mehwish | Customer Dashboard · Profile | Routes for customer dashboard, profile view and update
package com.shopping.system.controller;

import com.shopping.system.entity.User;
import com.shopping.system.repository.CartRepository;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerController(UserService userService,
                               OrderRepository orderRepository,
                               CartRepository cartRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        // Cart count via CartRepository — HeenuReet refactored CartItemRepository to use Cart-based lookup
        long cartCount = cartRepository.findByUser(user)
                .map(cart -> (long) cart.getCartItems().size())
                .orElse(0L);
        long orderCount = (long) orderRepository.findByUserId(user.getId()).size();

        // stream().limit(5).toList() — takes only 5 most recent without loading all orders into memory
        var recentOrders = orderRepository
                .findByUserIdOrderByOrderDateDesc(user.getId())
                .stream().limit(5).toList();

        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("recentOrders", recentOrders);
        return "customer/dashboard";
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String username,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String newPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        // Check uniqueness only for changed values — same username must not be flagged as taken
        if (!user.getUsername().equals(username) && userService.existsByUsername(username)) {
            ra.addFlashAttribute("error", "Username already taken.");
            return "redirect:/customer/profile";
        }
        if (!user.getEmail().equals(email) && userService.existsByEmail(email)) {
            ra.addFlashAttribute("error", "Email already registered.");
            return "redirect:/customer/profile";
        }

        user.setUsername(username);
        user.setEmail(email);

        // BCrypt re-encoding only triggered when newPassword is non-blank
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        User updatedUser = userService.save(user);
        // Must update session — otherwise navbar still shows the old username
        session.setAttribute("loggedInUser", updatedUser);
        ra.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/customer/profile";
    }
}
