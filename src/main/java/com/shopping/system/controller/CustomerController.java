// Owner: Mehwish | Customer Dashboard | Displays customer dashboard with cart/order stats, and profile management
package com.shopping.system.controller;

import com.shopping.system.entity.Order;
import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.repository.UserRepository;
import com.shopping.system.service.CartService;
import com.shopping.system.service.FeedbackService;
import com.shopping.system.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CustomerController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/customer/dashboard")
    public String customerDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() == UserRole.ADMIN) return "redirect:/admin/dashboard";

        int cartCount = cartService.getCartItemCount(user.getId());
        List<Order> recentOrders = orderService.getUserOrders(user.getId()).stream().limit(5).toList();
        long totalOrders = orderService.getUserOrders(user.getId()).size();

        model.addAttribute("currentUser", user);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("recentOrders", recentOrders);
        return "customer/dashboard";
    }

    @GetMapping("/customer/profile")
    public String profilePage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("currentUser", user);
        model.addAttribute("orderCount", orderService.getUserOrders(user.getId()).size());
        model.addAttribute("feedbackCount", feedbackService.getFeedbackByUser(user.getId()).size());
        model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        return "customer/profile";
    }

    @PostMapping("/customer/profile")
    public String updateProfile(@RequestParam String username,
                                @RequestParam String email,
                                @RequestParam(required = false) String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        // Check uniqueness only if changed
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
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters.");
                return "redirect:/customer/profile";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        session.setAttribute("loggedInUser", user);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/customer/profile";
    }
}
