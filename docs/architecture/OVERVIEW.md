# Architecture Overview — e-Kiosk Online Shopping System

## 1. Project Summary

| Item | Value |
|------|-------|
| Name | e-Kiosk Online Shopping System |
| Type | Full-stack web application (e-commerce) |
| Framework | Spring Boot 3.2.0 |
| Language | Java 17 |
| Database | MySQL 8/9 |
| Template Engine | Thymeleaf 3 |
| Front-end | Bootstrap 5 + custom CSS |
| Build Tool | Maven 3 |
| Auth | Session-based (HttpSession, BCrypt) |
| Email | Spring Mail (Gmail SMTP, SSL port 465) |

---

## 2. Technology Stack — Why Each Was Chosen

| Technology | Why Used |
|------------|----------|
| **Spring Boot** | Auto-configuration eliminates boilerplate; embedded Tomcat means no separate server setup |
| **Spring MVC** | Clean Controller → Service → Repository layering with URL mapping annotations |
| **Spring Data JPA / Hibernate** | Maps Java objects to DB tables automatically; eliminates raw SQL for CRUD |
| **Thymeleaf** | Server-side templates that can use Java objects directly without REST API layer |
| **BCryptPasswordEncoder** | One-way hashing; identical passwords produce different hashes (salted); industry standard |
| **HttpSession** | Simple, stateful login without full Spring Security overhead |
| **Spring Mail** | Sends email via JavaMailSender with minimal config; integrates with @Async |
| **MySQL** | Relational DB chosen for strong FK relationships between users, orders, products |
| **Bootstrap 5** | Responsive grid, pre-built components (cards, badges, tables) reduce custom CSS work |

---

## 3. Layer Architecture

```
┌────────────────────────────────────────────────────────┐
│                     BROWSER (Client)                   │
│         HTML forms, Thymeleaf-rendered pages           │
└────────────────────┬───────────────────────────────────┘
                     │ HTTP Request (GET/POST)
                     ▼
┌────────────────────────────────────────────────────────┐
│               CONTROLLER LAYER                         │
│  AuthController, ProductController, CartController,    │
│  OrderController, CustomerController, AdminController, │
│  AdminProductController, FeedbackController,           │
│  SalesController, ReportController                     │
│                                                        │
│  ► Reads session (loggedInUser)                        │
│  ► Validates request params                            │
│  ► Calls service methods                               │
│  ► Adds data to Model for Thymeleaf                    │
│  ► Returns template name or redirect                   │
│                                                        │
│  Cross-cutting concerns:                               │
│    SessionInterceptor — blocks unauthenticated access  │
│    GlobalExceptionHandler — catches all exceptions     │
└────────────────────┬───────────────────────────────────┘
                     │ Method calls
                     ▼
┌────────────────────────────────────────────────────────┐
│               SERVICE LAYER                            │
│  UserService, ProductService, CartService,             │
│  OrderService, FeedbackService, DashboardService,      │
│  SalesAnalysisService, EmailNotificationService        │
│                                                        │
│  ► Contains all business logic                         │
│  ► @Transactional on multi-step DB operations          │
│  ► @Async on email sending (non-blocking)              │
│  ► Throws IllegalArgumentException / IllegalState      │
│    when business rules are violated                    │
└────────────────────┬───────────────────────────────────┘
                     │ JPA method calls
                     ▼
┌────────────────────────────────────────────────────────┐
│               REPOSITORY LAYER                         │
│  UserRepository, ProductRepository, CartRepository,    │
│  CartItemRepository, OrderRepository,                  │
│  OrderItemRepository, FeedbackRepository               │
│                                                        │
│  ► All extend JpaRepository<Entity, Long>              │
│  ► Spring Data auto-generates SQL from method names    │
│  ► @Query for complex JPQL (JOIN FETCH, aggregates)    │
└────────────────────┬───────────────────────────────────┘
                     │ JDBC / Hibernate
                     ▼
┌────────────────────────────────────────────────────────┐
│               DATABASE (MySQL)                         │
│  Tables: users, products, carts, cart_items,           │
│           orders, order_items, feedback                │
│                                                        │
│  Schema auto-created/updated by:                       │
│    spring.jpa.hibernate.ddl-auto=update                │
└────────────────────────────────────────────────────────┘
```

---

## 4. Database Schema (ER Diagram)

```
users
  id (PK, AUTO_INCREMENT)
  username (UNIQUE, NOT NULL, VARCHAR 50)
  email    (UNIQUE, NOT NULL, VARCHAR 100)
  password (NOT NULL)          ← BCrypt hash
  role     (ENUM: CUSTOMER/ADMIN)
  created_date

      │ 1
      │ ├─────────────────────────────────┐
      │                                   │
      ▼ many                              ▼ 1
  orders                               carts
    id (PK)                              id (PK)
    user_id (FK → users)                 user_id (FK → users)
    order_date                           created_date
    total_amount
    status (ENUM)                              │ 1
    shipping_address                           ▼ many
                                          cart_items
          │ 1                               id (PK)
          ▼ many                            cart_id    (FK → carts)
      order_items                           product_id (FK → products)
        id (PK)                             quantity
        order_id   (FK → orders)            price      ← captured at add time
        product_id (FK → products)          subtotal   ← auto-calculated
        quantity
        price      ← captured at order time
        subtotal   ← auto-calculated

products                              feedback
  id (PK)                               id (PK)
  name (NOT NULL)                       user_id    (FK → users)
  description (TEXT)                    product_id (FK → products, nullable)
  price (DECIMAL 10,2)                  rating     (1-5)
  quantity_on_hand                      comment    (TEXT)
  category (ENUM)                       feedback_date
  created_date
  updated_date
```

**Key relationships:**
- One **User** has one **Cart** (1:1 in practice, enforced by getOrCreateCart logic)
- One **User** has many **Orders**
- One **Order** has many **OrderItems**
- One **Cart** has many **CartItems**
- One **Product** appears in many **CartItems** and **OrderItems**
- One **User** submits many **Feedback** entries

---

## 5. Session-Based Authentication Flow

```
Browser                 SessionInterceptor        AuthController        UserService
  │                           │                        │                    │
  │── GET /products ──────────▶                        │                    │
  │                           │ preHandle()            │                    │
  │                           │ getSession(false)      │                    │
  │                           │ "loggedInUser" null?   │                    │
  │◀── redirect /login ───────│                        │                    │
  │                           │                        │                    │
  │── POST /login ────────────────────────────────────▶                    │
  │   username, password      │                        │ loginUser()       │
  │                           │                        │──────────────────▶│
  │                           │                        │ BCrypt.matches()  │
  │                           │                        │◀──────────────────│
  │                           │                        │ session.setAttribute
  │                           │                        │ ("loggedInUser", user)
  │◀── redirect /dashboard ───────────────────────────│                    │
  │                           │                        │                    │
  │── GET /products ──────────▶                        │                    │
  │                           │ session has user ✓    │                    │
  │                           │──── proceed ──────────▶ ProductController  │
  │◀── products/list.html ────────────────────────────│                    │
```

---

## 6. Request Lifecycle (Happy Path)

1. **Browser** sends HTTP GET/POST
2. **DispatcherServlet** (Spring MVC front controller) receives it
3. **SessionInterceptor.preHandle()** runs — checks session; redirects to /login if no user
4. **Controller method** executes — reads session, calls service, adds to Model
5. **Service method** executes — business logic, DB operations via repository
6. **Repository** translates JPA method call to SQL via Hibernate
7. **MySQL** executes SQL, returns result set
8. Data flows back up: Repository → Service → Controller → Model
9. **Thymeleaf** renders the HTML template with Model data
10. **Browser** receives and displays HTML

---

## 7. Annotation Layer Map

| Layer | Key Annotations | Purpose |
|-------|----------------|---------|
| Entity | `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `@Enumerated`, `@PrePersist`, `@PreUpdate` | ORM mapping |
| Repository | `@Repository`, `@Query`, `@Param` | Data access |
| Service | `@Service`, `@Autowired`, `@Transactional`, `@Async` | Business logic |
| Controller | `@Controller`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam`, `@ModelAttribute`, `@ControllerAdvice`, `@ExceptionHandler` | HTTP handling |
| App | `@SpringBootApplication`, `@Bean`, `@Component`, `@EnableAsync` | Bootstrap & config |

---

## 8. Package Structure

```
com.shopping.system
├── OnlineShoppingSystemApplication.java   ← @SpringBootApplication entry point
├── DataInitializer.java                   ← @Component CommandLineRunner for seed data
├── entity/                                ← JPA entity classes (DB tables)
│   ├── User.java
│   ├── UserRole.java (enum)
│   ├── Product.java
│   ├── Category.java (enum)
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java (enum)
│   └── Feedback.java
├── repository/                            ← Spring Data JPA interfaces
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── FeedbackRepository.java
├── service/                               ← Business logic
│   ├── UserService.java
│   ├── ProductService.java
│   ├── CartService.java
│   ├── OrderService.java
│   ├── FeedbackService.java
│   ├── DashboardService.java
│   ├── SalesAnalysisService.java
│   └── EmailNotificationService.java
└── controller/                            ← HTTP request handlers
    ├── AuthController.java
    ├── ProductController.java
    ├── CartController.java
    ├── OrderController.java
    ├── CustomerController.java
    ├── AdminController.java
    ├── AdminProductController.java
    ├── FeedbackController.java
    ├── SalesController.java
    ├── ReportController.java
    ├── SessionInterceptor.java
    └── GlobalExceptionHandler.java
```

---

## 9. URL Map

| URL | Method | Controller | Access |
|-----|--------|------------|--------|
| `/` | GET | AuthController | Public |
| `/login` | GET/POST | AuthController | Public |
| `/register` | GET/POST | AuthController | Public |
| `/logout` | GET | AuthController | Authenticated |
| `/products` | GET | ProductController | Authenticated |
| `/products/{id}` | GET | ProductController | Authenticated |
| `/cart` | GET | CartController | Customer |
| `/cart/add` | POST | CartController | Customer |
| `/cart/remove/{id}` | POST | CartController | Customer |
| `/cart/update` | POST | CartController | Customer |
| `/cart/clear` | POST | CartController | Customer |
| `/orders/place` | POST | OrderController | Customer |
| `/orders` | GET | OrderController | Authenticated |
| `/orders/{id}` | GET | OrderController | Authenticated |
| `/orders/{id}/cancel` | GET/POST | OrderController | Authenticated |
| `/customer/dashboard` | GET | CustomerController | Customer |
| `/customer/profile` | GET/POST | CustomerController | Customer |
| `/feedback` | GET/POST | FeedbackController | Customer |
| `/admin/dashboard` | GET | AdminController | Admin |
| `/admin/products` | GET | AdminProductController | Admin |
| `/admin/products/add` | GET/POST | AdminProductController | Admin |
| `/admin/products/edit/{id}` | GET/POST | AdminProductController | Admin |
| `/admin/products/delete/{id}` | POST | AdminProductController | Admin |
| `/admin/feedback` | GET | FeedbackController | Admin |
| `/admin/sales` | GET | SalesController | Admin |
| `/admin/reports` | GET | ReportController | Admin |
| `/admin/reports/sales` | GET | ReportController | Admin |
| `/admin/reports/customers` | GET | ReportController | Admin |
| `/admin/reports/inventory` | GET | ReportController | Admin |
