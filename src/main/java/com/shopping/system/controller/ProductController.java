// Owner: HeenuReet | Product Search & Browse | Handles product listing, search, category filter, and detail view
package com.shopping.system.controller;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import com.shopping.system.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Category category,
                               Model model,
                               HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<Product> products;
        if (keyword != null && !keyword.isBlank() && category != null) {
            products = productService.searchWithCategory(keyword, category);
        } else if (keyword != null && !keyword.isBlank()) {
            products = productService.search(keyword);
        } else if (category != null) {
            products = productService.getByCategory(category);
        } else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUser", user);
        return "products/list";
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Category category,
                                 Model model,
                                 HttpSession session) {
        return listProducts(keyword, category, model, session);
    }

    @GetMapping("/category/{category}")
    public String byCategory(@PathVariable Category category,
                             Model model,
                             HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<Product> products = productService.getByCategory(category);
        model.addAttribute("products", products);
        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("currentUser", user);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Product product = productService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        model.addAttribute("currentUser", user);
        return "products/detail";
    }
}
