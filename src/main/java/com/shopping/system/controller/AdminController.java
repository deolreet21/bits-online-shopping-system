// Owner: Mehwish | Admin Dashboard | Displays admin dashboard metrics using DashboardService
package com.shopping.system.controller;

import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";

        model.addAttribute("currentUser", user);
        model.addAttribute("totalProducts", dashboardService.getTotalProducts());
        model.addAttribute("totalOrders", dashboardService.getTotalOrders());
        model.addAttribute("totalCustomers", dashboardService.getTotalCustomers());
        model.addAttribute("todaysSales", dashboardService.getTodaysSales());
        model.addAttribute("lowStockProducts", dashboardService.getLowStockProducts());
        model.addAttribute("recentOrders", dashboardService.getRecentOrders());
        model.addAttribute("recentFeedback", dashboardService.getRecentFeedback());
        return "admin/dashboard";
    }
}
