// Owner: Aliya | Product Management | Admin CRUD for products: list, add, edit, delete
package com.shopping.system.controller;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import com.shopping.system.entity.UserRole;
import com.shopping.system.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @GetMapping
    public String listProducts(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        return "admin/products/list";
    }

    @GetMapping("/add")
    public String addProductForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        model.addAttribute("product", new Product());
        model.addAttribute("categories", Category.values());
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        return "admin/products/add";
    }

    @PostMapping("/add")
    public String addProduct(@ModelAttribute Product product,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        productService.save(product);
        redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' added successfully.");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        Product product = productService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", Category.values());
        model.addAttribute("currentUser", session.getAttribute("loggedInUser"));
        return "admin/products/edit";
    }

    @PostMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id,
                              @ModelAttribute Product product,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        productService.update(id, product);
        redirectAttributes.addFlashAttribute("success", "Product updated successfully.");
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/customer/dashboard";
        Product product = productService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        productService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Product '" + product.getName() + "' deleted.");
        return "redirect:/admin/products";
    }
}
