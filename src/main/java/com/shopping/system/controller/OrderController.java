// Owner: HeenuReet | Ordering | Handles order placement, history, details, and cancellation
package com.shopping.system.controller;

import com.shopping.system.entity.Order;
import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public String placeOrder(@RequestParam(required = false) String shippingAddress,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (shippingAddress == null || shippingAddress.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Shipping address is required.");
            return "redirect:/cart";
        }

        try {
            Order order = orderService.createOrderFromCart(user, shippingAddress);
            return "redirect:/orders/" + order.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping
    public String orderHistory(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<Order> orders;
        if (user.getRole() == UserRole.ADMIN) {
            orders = orderService.getAllOrders();
        } else {
            orders = orderService.getUserOrders(user.getId());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("currentUser", user);
        return "orders/history";
    }

    @GetMapping("/{id}")
    public String orderDetails(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        // Ensure customers can only see their own orders
        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to view this order.");
        }

        model.addAttribute("order", order);
        model.addAttribute("canCancel", orderService.canCancelOrder(order));
        model.addAttribute("currentUser", user);
        return "orders/details";
    }

    @GetMapping("/{id}/cancel")
    public String cancelConfirmPage(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to view this order.");
        }
        if (!orderService.canCancelOrder(order)) {
            return "redirect:/orders/" + id;
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", user);
        return "orders/cancel-confirm";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (user.getRole() != UserRole.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to cancel this order.");
        }

        try {
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order #" + id + " has been cancelled.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
