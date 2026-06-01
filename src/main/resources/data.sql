# Owner: Mehwish | Database | Sample data for testing — 1 admin, 3 customers, 30 products, sample orders and feedback
# Passwords are BCrypt-encoded: "password123" for all users
# Spring Boot auto-runs this file on startup (spring.sql.init.mode=always in application.properties)

-- Users (BCrypt of "password123")
INSERT IGNORE INTO users (username, email, password, role, created_date) VALUES
('admin',    'admin@shop.com',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e', 'ADMIN',    NOW()),
('heenureet','heenu@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e', 'CUSTOMER', NOW()),
('aliya',    'aliya@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e', 'CUSTOMER', NOW()),
('mehwish',  'mehwish@example.com','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKnHd3tRXEGZ0Zjc9OvLKPKlQ/7e','CUSTOMER', NOW());

-- Products — Electronics (6)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('Samsung 55" Smart TV',    '4K UHD Smart TV with HDR and built-in streaming apps', 45999.00, 12, 'ELECTRONICS', NOW(), NOW()),
('Apple iPhone 14',         '128GB, Midnight Black, A15 Bionic chip, 12MP camera',  79999.00,  8, 'ELECTRONICS', NOW(), NOW()),
('Sony WH-1000XM5 Headphones','Industry-leading noise cancellation wireless headphones', 24999.00, 20, 'ELECTRONICS', NOW(), NOW()),
('Lenovo IdeaPad Laptop',   'Core i5, 8GB RAM, 512GB SSD, Windows 11',              55000.00,  6, 'ELECTRONICS', NOW(), NOW()),
('Canon EOS 200D Camera',   '24.1MP DSLR camera with 18-55mm lens kit',              38500.00,  4, 'ELECTRONICS', NOW(), NOW()),
('JBL Flip 6 Speaker',      'Portable waterproof Bluetooth speaker, 12hr battery',   8999.00, 30, 'ELECTRONICS', NOW(), NOW());

-- Products — Electrical (5)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('Havells Ceiling Fan',     '1200mm 3-blade energy-efficient ceiling fan',            3200.00, 25, 'ELECTRICAL', NOW(), NOW()),
('Philips LED Bulb Pack',   'Pack of 10 x 9W LED bulbs, warm white',                  650.00, 50, 'ELECTRICAL', NOW(), NOW()),
('Bajaj Mixer Grinder',     '750W, 3 jars, stainless steel blades',                  3499.00, 15, 'ELECTRICAL', NOW(), NOW()),
('Havells Iron',            '1250W dry iron with non-stick soleplate',               1299.00, 20, 'ELECTRICAL', NOW(), NOW()),
('V-Guard Voltage Stabilizer','5KVA stabilizer for AC units',                        4500.00,  3, 'ELECTRICAL', NOW(), NOW());

-- Products — Furniture (5)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('Wooden Study Table',      'Solid wood study table with drawer, 120x60cm',          8500.00,  8, 'FURNITURE', NOW(), NOW()),
('Office Chair',            'Ergonomic mesh back chair with lumbar support',         6999.00, 10, 'FURNITURE', NOW(), NOW()),
('3-Seater Sofa',           'Fabric upholstered sofa, grey color',                  22000.00,  4, 'FURNITURE', NOW(), NOW()),
('Queen Bed Frame',         'Solid sheesham wood queen size bed with storage',      18500.00,  3, 'FURNITURE', NOW(), NOW()),
('Bookshelf 5-Tier',        'Engineered wood open bookshelf, walnut finish',         4200.00, 12, 'FURNITURE', NOW(), NOW());

-- Products — Cosmetics (5)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('Lakme Foundation',        'Invisible finish foundation SPF 8, 30ml',               550.00, 40, 'COSMETICS', NOW(), NOW()),
('Biotique Sunscreen SPF 50','Bio sandalwood 50+ SPF sunscreen lotion 120ml',        350.00, 35, 'COSMETICS', NOW(), NOW()),
('Mamaearth Face Wash',     'Ubtan face wash with turmeric & saffron 100ml',         299.00, 60, 'COSMETICS', NOW(), NOW()),
('Loreal Shampoo',          'LOreal Paris Total Repair 5 shampoo 640ml',            699.00, 45, 'COSMETICS', NOW(), NOW()),
('Nivea Body Lotion',       'Soft moisturising cream with Jojoba oil 200ml',         220.00, 55, 'COSMETICS', NOW(), NOW());

-- Products — Toys (5)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('LEGO City Police Set',    '376 pieces police station building set, age 6+',       3999.00, 15, 'TOYS', NOW(), NOW()),
('Remote Control Car',      '1:16 scale RC car, 25km/h, rechargeable battery',      1499.00, 20, 'TOYS', NOW(), NOW()),
('Barbie Dream House',      '3-storey dollhouse with furniture and accessories',     4500.00,  8, 'TOYS', NOW(), NOW()),
('Funskool Scrabble',       'Classic word board game for 2-4 players, age 10+',      999.00, 25, 'TOYS', NOW(), NOW()),
('Nerf Elite Blaster',      'Fires up to 27 meters, includes 20 darts',             1799.00, 18, 'TOYS', NOW(), NOW());

-- Products — Books (4)
INSERT IGNORE INTO products (name, description, price, quantity_on_hand, category, created_date, updated_date) VALUES
('Clean Code - Robert Martin', 'A handbook of agile software craftsmanship',          699.00, 30, 'BOOKS', NOW(), NOW()),
('Atomic Habits - James Clear', 'Build good habits and break bad ones',              499.00, 40, 'BOOKS', NOW(), NOW()),
('The Alchemist - Paulo Coelho','A magical story about following your dreams',        299.00, 50, 'BOOKS', NOW(), NOW()),
('Rich Dad Poor Dad',           'Personal finance classic by Robert Kiyosaki',       349.00, 35, 'BOOKS', NOW(), NOW());
