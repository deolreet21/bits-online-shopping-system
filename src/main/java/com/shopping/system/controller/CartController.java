// Owner: Aliya | Shopping Cart | Handles cart view, add/remove/update items, and clear cart actions
package com.shopping.system.controller;

import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import com.shopping.system.service.CartService;
import com.shopping.system.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        var cart = cartService.getOrCreateCart(user.getId());
        model.addAttribute("cart", cart);
        model.addAttribute("currentUser", user);
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Product product = productService.getById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getQuantityOnHand() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Product is out of stock.");
            return "redirect:/products/" + productId;
        }
        if (quantity > product.getQuantityOnHand()) {
            redirectAttributes.addFlashAttribute("error", "Requested quantity exceeds available stock.");
            return "redirect:/products/" + productId;
        }

        cartService.addToCart(user.getId(), product, quantity);
        redirectAttributes.addFlashAttribute("success", "'" + product.getName() + "' added to cart.");
        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        cartService.removeFromCart(itemId);
        redirectAttributes.addFlashAttribute("success", "Item removed from cart.");
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long cartItemId,
                                 @RequestParam int quantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        cartService.updateQuantity(cartItemId, quantity);
        redirectAttributes.addFlashAttribute("success", "Cart updated.");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        cartService.clearCart(user.getId());
        redirectAttributes.addFlashAttribute("success", "Cart cleared.");
        return "redirect:/cart";
    }
}
