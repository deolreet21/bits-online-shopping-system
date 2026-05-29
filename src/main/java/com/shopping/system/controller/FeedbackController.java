// Owner: HeenuReet | Feedback | Handles customer feedback form and admin feedback view
package com.shopping.system.controller;

import com.shopping.system.entity.Feedback;
import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.FeedbackService;
import com.shopping.system.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ProductService productService;

    @GetMapping("/feedback")
    public String feedbackForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("currentUser", user);
        model.addAttribute("myFeedback", feedbackService.getFeedbackByUser(user.getId()));
        return "feedback/form";
    }

    @PostMapping("/feedback")
    public String submitFeedback(@RequestParam(required = false) Long productId,
                                 @RequestParam int rating,
                                 @RequestParam String comment,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5.");
            return "redirect:/feedback";
        }

        feedbackService.submitFeedback(user, productId, rating, comment);
        redirectAttributes.addFlashAttribute("success", "Your feedback has been submitted. Thank you!");
        return "redirect:/feedback";
    }

    @GetMapping("/admin/feedback")
    public String adminFeedback(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";

        List<Feedback> feedbackList = feedbackService.getAllFeedback();
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("currentUser", user);
        return "admin/feedback";
    }
}
