// Owner: Mehwish | Database | Runs on startup to seed default users and sample products if DB is empty
package com.shopping.system;

import com.shopping.system.entity.*;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserService userService;
    @Autowired private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (!userService.existsByUsername("admin")) {
            userService.registerUser("admin", "admin@shop.com", "password123", UserRole.ADMIN);
        }
        if (!userService.existsByUsername("heenureet")) {
            userService.registerUser("heenureet", "heenu@example.com", "password123", UserRole.CUSTOMER);
        }
        if (!userService.existsByUsername("aliya")) {
            userService.registerUser("aliya", "aliya@example.com", "password123", UserRole.CUSTOMER);
        }
        if (!userService.existsByUsername("mehwish")) {
            userService.registerUser("mehwish", "mehwish@example.com", "password123", UserRole.CUSTOMER);
        }
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        productRepository.saveAll(List.of(
            // Electronics
            product("Samsung 55\" Smart TV",         "4K UHD Smart TV with HDR and built-in streaming apps",          45999, 12, Category.ELECTRONICS),
            product("Apple iPhone 14",               "128GB, Midnight Black, A15 Bionic chip, 12MP camera",           79999,  8, Category.ELECTRONICS),
            product("Sony WH-1000XM5 Headphones",   "Industry-leading noise cancellation wireless headphones",        24999, 20, Category.ELECTRONICS),
            product("Lenovo IdeaPad Laptop",         "Core i5, 8GB RAM, 512GB SSD, Windows 11",                       55000,  6, Category.ELECTRONICS),
            product("Canon EOS 200D Camera",         "24.1MP DSLR camera with 18-55mm lens kit",                      38500,  4, Category.ELECTRONICS),
            product("JBL Flip 6 Speaker",            "Portable waterproof Bluetooth speaker, 12hr battery",            8999, 30, Category.ELECTRONICS),
            // Electrical
            product("Havells Ceiling Fan",           "1200mm 3-blade energy-efficient ceiling fan",                    3200, 25, Category.ELECTRICAL),
            product("Philips LED Bulb Pack",         "Pack of 10 x 9W LED bulbs, warm white",                          650, 50, Category.ELECTRICAL),
            product("Bajaj Mixer Grinder",           "750W, 3 jars, stainless steel blades",                           3499, 15, Category.ELECTRICAL),
            product("Havells Iron",                  "1250W dry iron with non-stick soleplate",                        1299, 20, Category.ELECTRICAL),
            product("V-Guard Voltage Stabilizer",    "5KVA stabilizer for AC units",                                   4500,  3, Category.ELECTRICAL),
            // Furniture
            product("Wooden Study Table",            "Solid wood study table with drawer, 120x60cm",                   8500,  8, Category.FURNITURE),
            product("Office Chair",                  "Ergonomic mesh back chair with lumbar support",                  6999, 10, Category.FURNITURE),
            product("3-Seater Sofa",                 "Fabric upholstered sofa, grey color",                          22000,  4, Category.FURNITURE),
            product("Queen Bed Frame",               "Solid sheesham wood queen size bed with storage",               18500,  3, Category.FURNITURE),
            product("Bookshelf 5-Tier",              "Engineered wood open bookshelf, walnut finish",                  4200, 12, Category.FURNITURE),
            // Cosmetics
            product("Lakme Foundation",              "Invisible finish foundation SPF 8, 30ml",                         550, 40, Category.COSMETICS),
            product("Biotique Sunscreen SPF 50",     "Bio sandalwood 50+ SPF sunscreen lotion 120ml",                   350, 35, Category.COSMETICS),
            product("Mamaearth Face Wash",           "Ubtan face wash with turmeric & saffron 100ml",                   299, 60, Category.COSMETICS),
            product("Loreal Shampoo",                "Total Repair 5 shampoo 640ml",                                    699, 45, Category.COSMETICS),
            product("Nivea Body Lotion",             "Soft moisturising cream with Jojoba oil 200ml",                   220, 55, Category.COSMETICS),
            // Toys
            product("LEGO City Police Set",          "376 pieces police station building set, age 6+",                 3999, 15, Category.TOYS),
            product("Remote Control Car",            "1:16 scale RC car, 25km/h, rechargeable battery",               1499, 20, Category.TOYS),
            product("Barbie Dream House",            "3-storey dollhouse with furniture and accessories",              4500,  8, Category.TOYS),
            product("Funskool Scrabble",             "Classic word board game for 2-4 players, age 10+",               999, 25, Category.TOYS),
            product("Nerf Elite Blaster",            "Fires up to 27 meters, includes 20 darts",                      1799, 18, Category.TOYS),
            // Books
            product("Clean Code",                   "A handbook of agile software craftsmanship - Robert Martin",      699, 30, Category.BOOKS),
            product("Atomic Habits",                "Build good habits and break bad ones - James Clear",              499, 40, Category.BOOKS),
            product("The Alchemist",                "A magical story about following your dreams - Paulo Coelho",      299, 50, Category.BOOKS),
            product("Rich Dad Poor Dad",            "Personal finance classic by Robert Kiyosaki",                    349, 35, Category.BOOKS)
        ));
    }

    private Product product(String name, String desc, double price, int qty, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(BigDecimal.valueOf(price));
        p.setQuantityOnHand(qty);
        p.setCategory(category);
        return p;
    }
}
