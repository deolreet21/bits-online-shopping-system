# Git Commands to Fetch Files from `plan` into `main`

Switch to `main` first:
```bash
git checkout main
```

Then use the commands below per commit.

---

## HeenuReet — 12 Commits

| # | Date | Commit Message | Files | Git Command |
|---|------|----------------|-------|-------------|
| 1 | 27 May 10:00 AM | `chore: initialise Spring Boot project with core dependencies` | `pom.xml`, `src/main/java/com/shopping/system/OnlineShoppingSystemApplication.java` | `git checkout origin/plan -- pom.xml src/main/java/com/shopping/system/OnlineShoppingSystemApplication.java` |
| 2 | 27 May 02:00 PM | `feat: add User entity with CUSTOMER and ADMIN roles` | `entity/UserRole.java`, `entity/User.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/entity/UserRole.java src/main/java/com/shopping/system/entity/User.java` |
| 3 | 27 May 05:00 PM | `feat: add UserRepository and UserService with BCrypt password encoding` | `repository/UserRepository.java`, `service/UserService.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/repository/UserRepository.java src/main/java/com/shopping/system/service/UserService.java` |
| 4 | 28 May 09:30 AM | `feat: add AuthController for login, register, and logout` | `controller/AuthController.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/AuthController.java` |
| 5 | 28 May 01:00 PM | `feat: add Thymeleaf login and registration pages` | `templates/login.html`, `templates/register.html` | `git checkout origin/plan -- src/main/resources/templates/login.html src/main/resources/templates/register.html` |
| 6 | 28 May 04:30 PM | `feat: add product search and category browse for customers` | `repository/ProductRepository.java`, `service/ProductService.java`, `controller/ProductController.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/repository/ProductRepository.java src/main/java/com/shopping/system/service/ProductService.java src/main/java/com/shopping/system/controller/ProductController.java` |
| 7 | 29 May 10:00 AM | `feat: add product listing and detail pages with search and category filter` | `templates/products/list.html`, `templates/products/detail.html` | `git checkout origin/plan -- src/main/resources/templates/products/list.html src/main/resources/templates/products/detail.html` |
| 8 | 29 May 03:00 PM | `feat: add Order entities and service with cart-to-order placement and cancellation` | `entity/OrderStatus.java`, `entity/Order.java`, `entity/OrderItem.java`, `repository/OrderRepository.java`, `repository/OrderItemRepository.java`, `service/OrderService.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/entity/OrderStatus.java src/main/java/com/shopping/system/entity/Order.java src/main/java/com/shopping/system/entity/OrderItem.java src/main/java/com/shopping/system/repository/OrderRepository.java src/main/java/com/shopping/system/repository/OrderItemRepository.java src/main/java/com/shopping/system/service/OrderService.java` |
| 9 | 30 May 09:00 AM | `feat: add order placement, history, details, and cancel confirmation pages` | `controller/OrderController.java`, `templates/orders/confirmation.html`, `templates/orders/history.html`, `templates/orders/details.html`, `templates/orders/cancel-confirm.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/OrderController.java src/main/resources/templates/orders/confirmation.html src/main/resources/templates/orders/history.html src/main/resources/templates/orders/details.html src/main/resources/templates/orders/cancel-confirm.html` |
| 10 | 30 May 02:00 PM | `feat: add customer feedback submission and admin feedback viewer` | `entity/Feedback.java`, `repository/FeedbackRepository.java`, `service/FeedbackService.java`, `controller/FeedbackController.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/entity/Feedback.java src/main/java/com/shopping/system/repository/FeedbackRepository.java src/main/java/com/shopping/system/service/FeedbackService.java src/main/java/com/shopping/system/controller/FeedbackController.java` |
| 11 | 31 May 10:00 AM | `feat: add session interceptor, global error handler, and shared navbar fragment` | `controller/SessionInterceptor.java`, `controller/GlobalExceptionHandler.java`, `templates/fragments/navbar.html`, `templates/error.html`, `templates/error/404.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/SessionInterceptor.java src/main/java/com/shopping/system/controller/GlobalExceptionHandler.java src/main/resources/templates/fragments/navbar.html src/main/resources/templates/error.html src/main/resources/templates/error/404.html` |
| 12 | 1 Jun 11:00 AM | `fix: seed users via DataInitializer, fix autocomplete attributes, add favicon` | `DataInitializer.java`, `templates/login.html`, `static/favicon.svg` | `git checkout origin/plan -- src/main/java/com/shopping/system/DataInitializer.java src/main/resources/templates/login.html src/main/resources/static/favicon.svg` |

---

## Aliya — 11 Commits

| # | Date | Commit Message | Files | Git Command |
|---|------|----------------|-------|-------------|
| 1 | 27 May 11:00 AM | `feat: add Product entity and Category enum with display names` | `entity/Category.java`, `entity/Product.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/entity/Category.java src/main/java/com/shopping/system/entity/Product.java` |
| 2 | 27 May 03:30 PM | `feat: add ProductRepository with search/filter and ProductService CRUD` | `repository/ProductRepository.java`, `service/ProductService.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/repository/ProductRepository.java src/main/java/com/shopping/system/service/ProductService.java` |
| 3 | 27 May 06:00 PM | `feat: add admin product management CRUD with list, add, and edit pages` | `controller/AdminProductController.java`, `templates/admin/products/list.html`, `templates/admin/products/add.html`, `templates/admin/products/edit.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/AdminProductController.java src/main/resources/templates/admin/products/list.html src/main/resources/templates/admin/products/add.html src/main/resources/templates/admin/products/edit.html` |
| 4 | 28 May 10:30 AM | `feat: add Cart and CartItem entities with auto-calculated subtotal` | `entity/Cart.java`, `entity/CartItem.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/entity/Cart.java src/main/java/com/shopping/system/entity/CartItem.java` |
| 5 | 28 May 03:00 PM | `feat: add CartService with add, remove, update quantity, and cart total logic` | `repository/CartRepository.java`, `repository/CartItemRepository.java`, `service/CartService.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/repository/CartRepository.java src/main/java/com/shopping/system/repository/CartItemRepository.java src/main/java/com/shopping/system/service/CartService.java` |
| 6 | 29 May 09:00 AM | `feat: add cart controller and cart view page with quantity update and remove` | `controller/CartController.java`, `templates/cart/cart.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/CartController.java src/main/resources/templates/cart/cart.html` |
| 7 | 29 May 02:00 PM | `feat: add order cancellation with inventory restore in OrderService` | `service/OrderService.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/service/OrderService.java` |
| 8 | 30 May 10:00 AM | `feat: add SalesAnalysisService with weekly, monthly, quarterly, yearly sales and fast/slow moving products` | `service/SalesAnalysisService.java`, `repository/OrderRepository.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/service/SalesAnalysisService.java src/main/java/com/shopping/system/repository/OrderRepository.java` |
| 9 | 30 May 03:30 PM | `feat: add sales dashboard with period tabs and fast/slow moving product tables` | `controller/SalesController.java`, `templates/admin/sales/dashboard.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/SalesController.java src/main/resources/templates/admin/sales/dashboard.html` |
| 10 | 31 May 11:00 AM | `feat: add client-side form validation for required fields, email, password strength, and numeric inputs` | `static/js/validation.js` | `git checkout origin/plan -- src/main/resources/static/js/validation.js` |
| 11 | 1 Jun 10:00 AM | `fix: polish admin product pages, add CSS custom properties, fix category dropdown` | `static/css/main.css`, `templates/admin/products/list.html`, `templates/admin/products/add.html`, `templates/admin/products/edit.html` | `git checkout origin/plan -- src/main/resources/static/css/main.css src/main/resources/templates/admin/products/list.html src/main/resources/templates/admin/products/add.html src/main/resources/templates/admin/products/edit.html` |

---

## Mehwish — 10 Commits

| # | Date | Commit Message | Files | Git Command |
|---|------|----------------|-------|-------------|
| 1 | 27 May 12:00 PM | `chore: configure MySQL datasource, JPA, Thymeleaf, and server properties` | `src/main/resources/application.properties` | `git checkout origin/plan -- src/main/resources/application.properties` |
| 2 | 27 May 04:00 PM | `feat: add DataInitializer to seed default users and sample products on startup` | `DataInitializer.java` | `git checkout origin/plan -- src/main/java/com/shopping/system/DataInitializer.java` |
| 3 | 28 May 09:00 AM | `feat: add customer dashboard with cart count, order stats, and quick navigation tiles` | `controller/CustomerController.java`, `templates/customer/dashboard.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/CustomerController.java src/main/resources/templates/customer/dashboard.html` |
| 4 | 28 May 05:00 PM | `feat: add customer profile view and update with password change support` | `controller/CustomerController.java`, `templates/customer/profile.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/CustomerController.java src/main/resources/templates/customer/profile.html` |
| 5 | 29 May 11:00 AM | `feat: add admin dashboard with total products, orders, customers, today's sales, and low stock alerts` | `service/DashboardService.java`, `controller/AdminController.java`, `templates/admin/dashboard.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/service/DashboardService.java src/main/java/com/shopping/system/controller/AdminController.java src/main/resources/templates/admin/dashboard.html` |
| 6 | 30 May 09:30 AM | `feat: add reports hub and sales report with date range filter and revenue summary` | `controller/ReportController.java`, `templates/admin/reports/dashboard.html`, `templates/admin/reports/sales.html` | `git checkout origin/plan -- src/main/java/com/shopping/system/controller/ReportController.java src/main/resources/templates/admin/reports/dashboard.html src/main/resources/templates/admin/reports/sales.html` |
| 7 | 30 May 02:30 PM | `feat: add inventory report with stock status badges and customer activity report` | `templates/admin/reports/inventory.html`, `templates/admin/reports/customers.html` | `git checkout origin/plan -- src/main/resources/templates/admin/reports/inventory.html src/main/resources/templates/admin/reports/customers.html` |
| 8 | 31 May 09:00 AM | `feat: add automated start script with MySQL install, service start, and DB creation` | `templates/admin/reports/dashboard.html`, `start.sh` | `git checkout origin/plan -- src/main/resources/templates/admin/reports/dashboard.html start.sh` |
| 9 | 31 May 03:00 PM | `feat: add admin feedback report page with rating colour coding` | `templates/admin/feedback.html` | `git checkout origin/plan -- src/main/resources/templates/admin/feedback.html` |
| 10 | 1 Jun 12:00 PM | `docs: add setup issues log, favicon, and fix all template head sections` | `static/favicon.svg`, all HTML templates (favicon link added) | `git checkout origin/plan -- src/main/resources/static/favicon.svg src/main/resources/templates/` |
