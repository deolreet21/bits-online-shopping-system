# e-Kiosk Online Shopping System — Documentation Index

This `docs/` branch contains professor-grade documentation for every file in the project.
Each source file has its own `.md` with: purpose, every annotation explained with reason,
end-to-end flow traces, backward/forward linkages, design decisions, and challenges.

---

## Navigation

| Section | Files |
|---------|-------|
| [Architecture Overview](architecture/OVERVIEW.md) | System design, DB schema, layer diagram, auth flow |
| **Config** | |
| [pom.xml](config/pom-xml.md) | Maven build — every dependency explained |
| [application.properties](config/application-properties.md) | Spring Boot config — every property explained |
| [start.sh](config/start-sh.md) | Launch script — step-by-step walkthrough |
| [data.sql](config/data-sql.md) | Seed SQL — why it exists, when it runs |
| [.env / .env.example](config/env-file.md) | Environment variables — what each does |
| [mvnw / mvnw.cmd](config/mvnw.md) | Maven Wrapper — what it is and why it's used |
| [validation.js](config/validation-js.md) | Client-side validation script |
| [main.css](config/main-css.md) | Custom CSS — Bootstrap extension |
| **Entities** | |
| [User](entity/User.md) | User JPA entity — fields, annotations, lifecycle |
| [UserRole](entity/UserRole.md) | CUSTOMER / ADMIN enum |
| [Product](entity/Product.md) | Product entity — price, stock, category, timestamps |
| [Category](entity/Category.md) | 6-value category enum with displayName |
| [Cart](entity/Cart.md) | Shopping cart entity — total calculation |
| [CartItem](entity/CartItem.md) | Cart line item — auto-subtotal calculation |
| [Order](entity/Order.md) | Order entity — status lifecycle, items |
| [OrderItem](entity/OrderItem.md) | Order line item — price captured at order time |
| [OrderStatus](entity/OrderStatus.md) | PENDING → CONFIRMED → SHIPPED → DELIVERED / CANCELLED |
| [Feedback](entity/Feedback.md) | User feedback — optional product link |
| **Repositories** | |
| [UserRepository](repository/UserRepository.md) | Lookup by username/email, existence checks |
| [ProductRepository](repository/ProductRepository.md) | Category filter, name search |
| [CartRepository](repository/CartRepository.md) | Cart lookup by user |
| [CartItemRepository](repository/CartItemRepository.md) | Cart item by cart+product |
| [OrderRepository](repository/OrderRepository.md) | Orders, JOIN FETCH, sales queries |
| [OrderItemRepository](repository/OrderItemRepository.md) | Items by order ID |
| [FeedbackRepository](repository/FeedbackRepository.md) | Feedback by user/product/date |
| **Services** | |
| [UserService](service/UserService.md) | Register, login (BCrypt), lookup |
| [ProductService](service/ProductService.md) | CRUD, search, category, low stock |
| [CartService](service/CartService.md) | Add/remove/update/clear/total |
| [OrderService](service/OrderService.md) | Create from cart, cancel, history |
| [FeedbackService](service/FeedbackService.md) | Submit and retrieve feedback |
| [DashboardService](service/DashboardService.md) | Admin metrics aggregation |
| [SalesAnalysisService](service/SalesAnalysisService.md) | Weekly/monthly/quarterly/yearly + product movement |
| [EmailNotificationService](service/EmailNotificationService.md) | Async email for order events |
| **Controllers** | |
| [AuthController](controller/AuthController.md) | Login, register, logout, session |
| [SessionInterceptor](controller/SessionInterceptor.md) | Global auth guard |
| [GlobalExceptionHandler](controller/GlobalExceptionHandler.md) | Centralized error handling |
| [ProductController](controller/ProductController.md) | Browse, search, detail |
| [CartController](controller/CartController.md) | Cart CRUD |
| [OrderController](controller/OrderController.md) | Place, history, details, cancel |
| [CustomerController](controller/CustomerController.md) | Customer dashboard, profile |
| [AdminController](controller/AdminController.md) | Admin dashboard |
| [AdminProductController](controller/AdminProductController.md) | Admin product CRUD |
| [FeedbackController](controller/FeedbackController.md) | Submit feedback, admin view |
| [SalesController](controller/SalesController.md) | Sales analysis views |
| [ReportController](controller/ReportController.md) | Admin reports |
| **Templates** | |
| [login.html](templates/login.md) | Login form |
| [register.html](templates/register.md) | Registration form |
| [navbar fragment](templates/navbar.md) | Shared navigation bar |
| [customer/dashboard + profile](templates/customer-pages.md) | Customer home and profile |
| [products/list](templates/products-list.md) | Product catalogue with search |
| [products/detail](templates/product-detail.md) | Single product view |
| [cart/cart](templates/cart.md) | Cart page |
| [orders/history](templates/orders-history.md) | Order list (customer + admin) |
| [orders/details + cancel-confirm](templates/order-detail-cancel.md) | Order detail and cancel |
| [orders/confirmation](templates/order-confirmation.md) | Order placed confirmation |
| [admin/dashboard](templates/admin-dashboard.md) | Admin home with metrics |
| [admin/products/*](templates/admin-products.md) | Admin product CRUD forms |
| [admin/feedback](templates/admin-feedback.md) | Admin feedback view |
| [admin/sales + admin/reports/*](templates/admin-reports-sales.md) | Sales analytics and reports |
| [feedback/form](templates/feedback-form.md) | Feedback submission form |
| [error pages](templates/error-pages.md) | 404/500 error views |
| [OnlineShoppingSystemApplication](config/OnlineShoppingSystemApplication.md) | Main class, @SpringBootApplication, BCrypt bean |
| [DataInitializer](config/DataInitializer.md) | CommandLineRunner seed data |
| **End-to-End Flows** | |
| [Login Flow](flows/01-login-flow.md) | Browser → Session setup |
| [Browse Products Flow](flows/02-browse-products-flow.md) | Search + filter |
| [Add to Cart Flow](flows/03-add-to-cart-flow.md) | Stock check → cart update |
| [Place Order Flow](flows/04-place-order-flow.md) | Cart → Order → Email notification |
| [Cancel Order Flow](flows/05-cancel-order-flow.md) | Validation → stock restore |
| [Admin Status Update Flow](flows/06-admin-update-status-flow.md) | Status change → email |
| [Email Notification Flow](flows/07-email-notification-flow.md) | Async SMTP delivery |
| **Design Decisions** | |
| [Design Decisions & Challenges](challenges/DESIGN-DECISIONS.md) | Why BCrypt, sessions, JOIN FETCH, @Async, etc. |

---

## Quick Start for Professor Review

1. Start with **[Architecture Overview](architecture/OVERVIEW.md)** for the big picture
2. Pick any feature (e.g. "Place Order") and follow **[flows/04-place-order-flow.md](flows/04-place-order-flow.md)**
3. Drill into any file using its dedicated `.md` above
4. See **[challenges/DESIGN-DECISIONS.md](challenges/DESIGN-DECISIONS.md)** for why things were built the way they were
