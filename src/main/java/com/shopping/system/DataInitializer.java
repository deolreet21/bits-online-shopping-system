// Owner: Mehwish | Database & Config | Seeds default admin/customer users and sample products on startup
package com.shopping.system;

import com.shopping.system.entity.Product;
import com.shopping.system.entity.UserRole;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    @SuppressWarnings("null") // List.of() is guaranteed non-null; false positive from Eclipse null analysis
    private void seedProducts() {
        // Guard: skip entirely if any products already exist
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
            new Product("Wireless Mouse",
                    "Ergonomic wireless mouse with 2.4 GHz connectivity and 12-month battery life",
                    new BigDecimal("799.00"), 50, "Electronics"),

            new Product("Mechanical Keyboard",
                    "Compact TKL keyboard with blue switches and RGB backlight",
                    new BigDecimal("2499.00"), 30, "Electronics"),

            new Product("USB-C Hub 7-in-1",
                    "Multiport adapter with HDMI, USB 3.0 x3, SD card, and PD charging",
                    new BigDecimal("1299.00"), 25, "Electronics"),

            new Product("Noise-Cancelling Earbuds",
                    "True wireless earbuds with active noise cancellation and 24h battery",
                    new BigDecimal("3499.00"), 20, "Electronics"),

            new Product("Laptop Stand Aluminium",
                    "Adjustable aluminium stand compatible with 11–17 inch laptops",
                    new BigDecimal("899.00"), 40, "Accessories"),

            new Product("Microfibre Cleaning Kit",
                    "Screen cleaning kit with spray and microfibre cloth for monitors and phones",
                    new BigDecimal("199.00"), 100, "Accessories"),

            new Product("Webcam 1080p HD",
                    "Full HD webcam with built-in microphone and auto light correction",
                    new BigDecimal("1999.00"), 15, "Electronics"),

            new Product("Desk Organiser Bamboo",
                    "Eco-friendly bamboo desk organiser with 5 compartments",
                    new BigDecimal("649.00"), 3, "Stationery"),   // low stock — tests alert

            new Product("A4 Notebook 200 Pages",
                    "Hardcover ruled notebook, acid-free paper, 200 pages",
                    new BigDecimal("149.00"), 200, "Stationery"),

            new Product("Portable Charger 20000mAh",
                    "High-capacity power bank with dual USB-A and USB-C output, 22.5W fast charge",
                    new BigDecimal("2199.00"), 35, "Electronics")
        );

        productRepository.saveAll(products);
        System.out.println("[DataInitializer] " + products.size() + " sample products seeded.");
    }
}
