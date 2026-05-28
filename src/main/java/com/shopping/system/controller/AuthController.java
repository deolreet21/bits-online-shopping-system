// Owner: HeenuReet | Login & Registration | Handles GET/POST login, register, logout with session management
package com.shopping.system.controller;

import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() == UserRole.ADMIN) return "redirect:/admin/dashboard";
        return "redirect:/customer/dashboard";
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            User user = (User) session.getAttribute("loggedInUser");
            return user.getRole() == UserRole.ADMIN ? "redirect:/admin/dashboard" : "redirect:/customer/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.loginUser(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute("loggedInUser", user);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes
            if (user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/customer/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(defaultValue = "CUSTOMER") UserRole role,
                           RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/register";
        }
        try {
            userService.registerUser(username, email, password, role);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "You have been logged out successfully.");
        return "redirect:/login";
    }
}
