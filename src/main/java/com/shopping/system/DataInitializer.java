// Owner: Mehwish | Database & Config | Seeds default admin/customer users and sample products on startup
package com.shopping.system;

import com.shopping.system.entity.Category;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.UserRole;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs after the full Spring context is ready.
 * Guard checks prevent duplicate inserts on every restart.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final ProductRepository productRepository;

    public DataInitializer(UserService userService, ProductRepository productRepository) {
        this.userService = userService;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    // ── Users ────────────────────────────────────────────────────────────────

    private void seedUsers() {
        // Guard: existsByUsername prevents duplicates on every restart
        if (!userService.existsByUsername("admin")) {
            // BCryptPasswordEncoder called through registerUser() — same encoder as normal registration
            userService.registerUser("admin", "admin@ekiosk.com", "Admin@123", UserRole.ADMIN);
            System.out.println("[DataInitializer] Admin user created.");
        }

        if (!userService.existsByUsername("customer1")) {
            userService.registerUser("customer1", "customer1@ekiosk.com", "Customer@123", UserRole.CUSTOMER);
            System.out.println("[DataInitializer] Default customer user created.");
        }
    }

    // ── Products ─────────────────────────────────────────────────────────────

    private void seedProducts() {
        // Guard: skip entirely if any products already exist
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = new ArrayList<>();

        products.add(makeProduct("Wireless Mouse",
                "Ergonomic wireless mouse with 2.4 GHz connectivity and 12-month battery life",
                new BigDecimal("799.00"), 50, Category.ELECTRONICS));

        products.add(makeProduct("Mechanical Keyboard",
                "Compact TKL keyboard with blue switches and RGB backlight",
                new BigDecimal("2499.00"), 30, Category.ELECTRONICS));

        products.add(makeProduct("USB-C Hub 7-in-1",
                "Multiport adapter with HDMI, USB 3.0 x3, SD card, and PD charging",
                new BigDecimal("1299.00"), 25, Category.ELECTRONICS));

        products.add(makeProduct("Noise-Cancelling Earbuds",
                "True wireless earbuds with active noise cancellation and 24h battery",
                new BigDecimal("3499.00"), 20, Category.ELECTRONICS));

        products.add(makeProduct("Laptop Stand Aluminium",
                "Adjustable aluminium stand compatible with 11–17 inch laptops",
                new BigDecimal("899.00"), 40, Category.ELECTRONICS));

        products.add(makeProduct("Microfibre Cleaning Kit",
                "Screen cleaning kit with spray and microfibre cloth for monitors and phones",
                new BigDecimal("199.00"), 100, Category.ELECTRONICS));

        products.add(makeProduct("Webcam 1080p HD",
                "Full HD webcam with built-in microphone and auto light correction",
                new BigDecimal("1999.00"), 15, Category.ELECTRONICS));

        products.add(makeProduct("Desk Organiser Bamboo",
                "Eco-friendly bamboo desk organiser with 5 compartments",
                new BigDecimal("649.00"), 3, Category.FURNITURE));  // low stock — tests alert

        products.add(makeProduct("A4 Notebook 200 Pages",
                "Hardcover ruled notebook, acid-free paper, 200 pages",
                new BigDecimal("149.00"), 200, Category.BOOKS));

        products.add(makeProduct("Portable Charger 20000mAh",
                "High-capacity power bank with dual USB-A and USB-C output, 22.5W fast charge",
                new BigDecimal("2199.00"), 35, Category.ELECTRONICS));

        productRepository.saveAll(products);
        System.out.println("[DataInitializer] " + products.size() + " sample products seeded.");
    }

    private Product makeProduct(String name, String description, BigDecimal price,
                                int quantity, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setQuantityOnHand(quantity);
        p.setCategory(category);
        return p;
    }
}
