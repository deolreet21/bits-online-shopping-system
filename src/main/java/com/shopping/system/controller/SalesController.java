// Owner: Aliya | Sales Analysis | Admin sales analysis views: weekly, monthly, quarterly, yearly, products
package com.shopping.system.controller;

import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.SalesAnalysisService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/sales")
public class SalesController {

    @Autowired
    private SalesAnalysisService salesAnalysisService;

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @GetMapping({"", "/"})
    public String salesDashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";

        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("weeklySales", salesAnalysisService.getWeeklySales());
        model.addAttribute("monthlySales", salesAnalysisService.getMonthlySales());
        model.addAttribute("quarterlySales", salesAnalysisService.getQuarterlySales());
        model.addAttribute("yearlySales", salesAnalysisService.getYearlySales());
        model.addAttribute("fastMovingProducts", salesAnalysisService.getFastMovingProducts());
        model.addAttribute("slowMovingProducts", salesAnalysisService.getSlowMovingProducts());
        model.addAttribute("salesByCategory", salesAnalysisService.getSalesByCategory());
        return "admin/sales/dashboard";
    }

    @GetMapping("/weekly")
    public String weeklySales(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("weeklySales", salesAnalysisService.getWeeklySales());
        model.addAttribute("period", "Weekly");
        return "admin/sales/dashboard";
    }

    @GetMapping("/monthly")
    public String monthlySales(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("monthlySales", salesAnalysisService.getMonthlySales());
        model.addAttribute("period", "Monthly");
        return "admin/sales/dashboard";
    }

    @GetMapping("/quarterly")
    public String quarterlySales(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("quarterlySales", salesAnalysisService.getQuarterlySales());
        model.addAttribute("period", "Quarterly");
        return "admin/sales/dashboard";
    }

    @GetMapping("/yearly")
    public String yearlySales(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("yearlySales", salesAnalysisService.getYearlySales());
        model.addAttribute("period", "Yearly");
        return "admin/sales/dashboard";
    }

    @GetMapping("/products")
    public String productSales(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        model.addAttribute("fastMovingProducts", salesAnalysisService.getFastMovingProducts());
        model.addAttribute("slowMovingProducts", salesAnalysisService.getSlowMovingProducts());
        return "admin/sales/dashboard";
    }
}
