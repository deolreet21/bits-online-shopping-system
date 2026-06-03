# All Endpoints — e-Kiosk Online Shopping System

**Base URL:** `http://localhost:8080`  
**Auth mechanism:** Session cookie (`JSESSIONID`). Login via `POST /login` first — the cookie is automatically sent on all subsequent requests.  
**Access levels:** `PUBLIC` = no login required · `CUSTOMER` = any logged-in user · `ADMIN` = role must be ADMIN

---

## Quick Reference Table

| # | Method | URL | Access | Description |
|---|--------|-----|--------|-------------|
| 1 | GET | `/` | PUBLIC | Root redirect — sends to login or dashboard |
| 2 | GET | `/login` | PUBLIC | Show login form |
| 3 | POST | `/login` | PUBLIC | Submit credentials, create session |
| 4 | GET | `/register` | PUBLIC | Show registration form |
| 5 | POST | `/register` | PUBLIC | Create new customer account |
| 6 | GET | `/logout` | CUSTOMER | Invalidate session |
| 7 | GET | `/customer/dashboard` | CUSTOMER | Customer home — cart count, recent orders |
| 8 | GET | `/customer/profile` | CUSTOMER | View profile + stats |
| 9 | POST | `/customer/profile` | CUSTOMER | Update username / email / password |
| 10 | GET | `/products` | CUSTOMER | Browse all products, optional search + filter |
| 11 | GET | `/products/search` | CUSTOMER | Search alias (same as `/products?keyword=`) |
| 12 | GET | `/products/category/{category}` | CUSTOMER | Filter by category |
| 13 | GET | `/products/{id}` | CUSTOMER | Product detail page |
| 14 | GET | `/cart` | CUSTOMER | View cart |
| 15 | POST | `/cart/add` | CUSTOMER | Add product to cart |
| 16 | POST | `/cart/update` | CUSTOMER | Change quantity of a cart item |
| 17 | POST | `/cart/remove/{itemId}` | CUSTOMER | Remove one item from cart |
| 18 | POST | `/cart/clear` | CUSTOMER | Empty entire cart |
| 19 | POST | `/orders/place` | CUSTOMER | Place order from cart |
| 20 | GET | `/orders` | CUSTOMER / ADMIN | Order history (own orders / all orders) |
| 21 | GET | `/orders/{id}` | CUSTOMER / ADMIN | Order detail |
| 22 | GET | `/orders/{id}/cancel` | CUSTOMER / ADMIN | Cancellation confirmation page |
| 23 | POST | `/orders/{id}/cancel` | CUSTOMER / ADMIN | Confirm and cancel the order |
| 24 | GET | `/feedback` | CUSTOMER | View feedback form + own past feedback |
| 25 | POST | `/feedback` | CUSTOMER | Submit feedback |
| 26 | GET | `/admin/dashboard` | ADMIN | Admin home — KPIs, low stock, recent orders |
| 27 | GET | `/admin/products` | ADMIN | List all products |
| 28 | GET | `/admin/products/add` | ADMIN | Show add-product form |
| 29 | POST | `/admin/products/add` | ADMIN | Create new product |
| 30 | GET | `/admin/products/edit/{id}` | ADMIN | Show edit-product form |
| 31 | POST | `/admin/products/edit/{id}` | ADMIN | Update existing product |
| 32 | POST | `/admin/products/delete/{id}` | ADMIN | Delete a product |
| 33 | GET | `/admin/feedback` | ADMIN | View all customer feedback |
| 34 | GET | `/admin/sales` | ADMIN | Sales analytics dashboard (all periods) |
| 35 | GET | `/admin/sales/weekly` | ADMIN | Weekly sales total |
| 36 | GET | `/admin/sales/monthly` | ADMIN | Monthly sales total |
| 37 | GET | `/admin/sales/quarterly` | ADMIN | Quarterly sales total |
| 38 | GET | `/admin/sales/yearly` | ADMIN | Yearly sales total |
| 39 | GET | `/admin/sales/products` | ADMIN | Fast and slow moving products |
| 40 | GET | `/admin/reports` | ADMIN | Reports hub page |
| 41 | GET | `/admin/reports/sales` | ADMIN | Date-range sales report |
| 42 | GET | `/admin/reports/products` | ADMIN | Product / inventory report |
| 43 | GET | `/admin/reports/customers` | ADMIN | Customer list with order counts |
| 44 | GET | `/admin/reports/feedback` | ADMIN | Full feedback report |
| 45 | GET | `/admin/reports/inventory` | ADMIN | Inventory levels report |

---

## Detailed Endpoint Reference

---

### AUTH

---

#### 1. `GET /`
| Field | Value |
|-------|-------|
| **Access** | PUBLIC |
| **Description** | Root redirect. If no session → `/login`. If ADMIN → `/admin/dashboard`. If CUSTOMER → `/customer/dashboard`. |
| **Output** | HTTP 302 redirect |

---

#### 2. `GET /login`
| Field | Value |
|-------|-------|
| **Access** | PUBLIC |
| **Description** | Renders the login form. If already logged in, redirects to the appropriate dashboard. |
| **Output** | `login.html` — form with username/password fields and optional flash error/success |

**Bruno:**
```
GET http://localhost:8080/login
```

---

#### 3. `POST /login`
| Field | Value |
|-------|-------|
| **Access** | PUBLIC |
| **Description** | Authenticates the user. Sets `JSESSIONID` cookie on success. Redirects to admin or customer dashboard based on role. |
| **Form params** | `username`, `password` |
| **Success output** | HTTP 302 → `/admin/dashboard` or `/customer/dashboard` |
| **Failure output** | HTTP 302 → `/login` with flash `error` = "Invalid username or password." |

**Bruno — login as admin:**
```
POST http://localhost:8080/login
Content-Type: application/x-www-form-urlencoded

username=admin&password=admin123
```

**Bruno — login as customer:**
```
POST http://localhost:8080/login
Content-Type: application/x-www-form-urlencoded

username=alice&password=password123
```

---

#### 4. `GET /register`
| Field | Value |
|-------|-------|
| **Access** | PUBLIC |
| **Description** | Renders the customer registration form. Redirects to `/` if already logged in. |
| **Output** | `register.html` |

**Bruno:**
```
GET http://localhost:8080/register
```

---

#### 5. `POST /register`
| Field | Value |
|-------|-------|
| **Access** | PUBLIC |
| **Description** | Creates a new CUSTOMER account. Password is BCrypt-hashed before saving. Validates: passwords match, password ≥ 6 chars, username/email not already taken. |
| **Form params** | `username`, `email`, `password`, `confirmPassword`, `role` (optional, defaults to `CUSTOMER`) |
| **Success output** | HTTP 302 → `/login` with flash `success` |
| **Failure output** | HTTP 302 → `/register` with flash `error` |

**Bruno:**
```
POST http://localhost:8080/register
Content-Type: application/x-www-form-urlencoded

username=riya&email=riya@example.com&password=riya1234&confirmPassword=riya1234
```

---

#### 6. `GET /logout`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Invalidates the `HttpSession`, clearing all session data. Redirects to login with a success flash. |
| **Output** | HTTP 302 → `/login` with flash `success` = "You have been logged out successfully." |

**Bruno:**
```
GET http://localhost:8080/logout
Cookie: JSESSIONID={{session_id}}
```

---

### CUSTOMER

---

#### 7. `GET /customer/dashboard`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Customer home page. Shows cart item count, total orders placed, and last 5 orders. ADMINs are redirected to `/admin/dashboard`. |
| **Output** | `customer/dashboard.html` with `cartCount`, `totalOrders`, `recentOrders` |

**Bruno:**
```
GET http://localhost:8080/customer/dashboard
Cookie: JSESSIONID={{session_id}}
```

---

#### 8. `GET /customer/profile`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | View own profile. Shows username, email, total orders placed, total feedback submitted, cart item count. |
| **Output** | `customer/profile.html` with `currentUser`, `orderCount`, `feedbackCount`, `cartCount` |

**Bruno:**
```
GET http://localhost:8080/customer/profile
Cookie: JSESSIONID={{session_id}}
```

---

#### 9. `POST /customer/profile`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Update own username, email, and optionally password. Validates uniqueness of new username/email. Updates session with new user object after save. |
| **Form params** | `username`, `email`, `newPassword` (optional) |
| **Success output** | HTTP 302 → `/customer/profile` with flash `successMessage` |
| **Failure output** | HTTP 302 → `/customer/profile` with flash `errorMessage` |

**Bruno:**
```
POST http://localhost:8080/customer/profile
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

username=alice_updated&email=alice@example.com
```

---

### PRODUCTS (Customer Browse)

---

#### 10. `GET /products`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Browse all products. Optional `keyword` for name search and `category` filter. If both provided, both filters apply. |
| **Query params** | `keyword` (optional), `category` (optional — one of: `ELECTRONICS`, `ELECTRICAL`, `FURNITURE`, `COSMETICS`, `TOYS`, `BOOKS`) |
| **Output** | `products/list.html` with `products` list, `categories`, `selectedCategory`, `keyword` |

**Bruno — all products:**
```
GET http://localhost:8080/products
Cookie: JSESSIONID={{session_id}}
```

**Bruno — search by keyword:**
```
GET http://localhost:8080/products?keyword=mouse
Cookie: JSESSIONID={{session_id}}
```

**Bruno — filter by category:**
```
GET http://localhost:8080/products?category=ELECTRONICS
Cookie: JSESSIONID={{session_id}}
```

**Bruno — keyword + category:**
```
GET http://localhost:8080/products?keyword=lamp&category=ELECTRICAL
Cookie: JSESSIONID={{session_id}}
```

---

#### 11. `GET /products/search`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Alias for `GET /products` — accepts same `keyword` and `category` params. Delegates to the same handler. |
| **Output** | Same as `GET /products` |

**Bruno:**
```
GET http://localhost:8080/products/search?keyword=chair
Cookie: JSESSIONID={{session_id}}
```

---

#### 12. `GET /products/category/{category}`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Filter products by category via path variable. |
| **Path param** | `category` — one of: `ELECTRONICS`, `ELECTRICAL`, `FURNITURE`, `COSMETICS`, `TOYS`, `BOOKS` |
| **Output** | `products/list.html` filtered to that category |

**Bruno:**
```
GET http://localhost:8080/products/category/FURNITURE
Cookie: JSESSIONID={{session_id}}
```

---

#### 13. `GET /products/{id}`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Show detail page for a single product — name, description, price, stock, category. Includes add-to-cart form. |
| **Path param** | `id` — product ID |
| **Output** | `products/detail.html` with `product` |
| **Error** | 500 / error page if product not found |

**Bruno:**
```
GET http://localhost:8080/products/1
Cookie: JSESSIONID={{session_id}}
```

---

### CART

---

#### 14. `GET /cart`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | View the current user's shopping cart. Creates an empty cart if none exists yet. Shows all items, quantities, subtotals, and grand total. |
| **Output** | `cart/cart.html` with `cart` object (items + totals) |

**Bruno:**
```
GET http://localhost:8080/cart
Cookie: JSESSIONID={{session_id}}
```

---

#### 15. `POST /cart/add`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Add a product to the cart. Validates stock — rejects if out of stock or requested quantity exceeds stock. If product already in cart, quantity is incremented. |
| **Form params** | `productId` (required), `quantity` (optional, default `1`) |
| **Success output** | HTTP 302 → `/cart` with flash `success` |
| **Failure output** | HTTP 302 → `/products/{id}` with flash `error` |

**Bruno:**
```
POST http://localhost:8080/cart/add
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

productId=3&quantity=2
```

---

#### 16. `POST /cart/update`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Change the quantity of an existing cart item. |
| **Form params** | `cartItemId`, `quantity` |
| **Output** | HTTP 302 → `/cart` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/cart/update
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

cartItemId=5&quantity=3
```

---

#### 17. `POST /cart/remove/{itemId}`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Remove a single item from the cart by its cart-item ID (not product ID). |
| **Path param** | `itemId` — cart item ID |
| **Output** | HTTP 302 → `/cart` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/cart/remove/5
Cookie: JSESSIONID={{session_id}}
```

---

#### 18. `POST /cart/clear`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Remove all items from the cart. The Cart row itself is kept; only CartItems are deleted (via `orphanRemoval`). |
| **Output** | HTTP 302 → `/cart` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/cart/clear
Cookie: JSESSIONID={{session_id}}
```

---

### ORDERS

---

#### 19. `POST /orders/place`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Place an order from the current cart. Atomically: validates stock, deducts inventory, creates Order + OrderItems, clears cart, sends async confirmation email. Requires non-empty cart and non-blank shipping address. |
| **Form params** | `shippingAddress` |
| **Success output** | HTTP 302 → `/orders/{newOrderId}` |
| **Failure output** | HTTP 302 → `/cart` with flash `error` (empty cart, blank address, or insufficient stock) |

**Bruno:**
```
POST http://localhost:8080/orders/place
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

shippingAddress=42 MG Road, Bengaluru, Karnataka 560001
```

---

#### 20. `GET /orders`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER / ADMIN |
| **Description** | Order history. CUSTOMERs see only their own orders. ADMINs see all orders across all users, sorted newest-first. |
| **Output** | `orders/history.html` with `orders` list |

**Bruno:**
```
GET http://localhost:8080/orders
Cookie: JSESSIONID={{session_id}}
```

---

#### 21. `GET /orders/{id}`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER (own orders only) / ADMIN (any order) |
| **Description** | Full order detail — items, quantities, prices, subtotals, total, status, shipping address. CUSTOMERs who try to view another user's order get a 500 error. |
| **Path param** | `id` — order ID |
| **Output** | `orders/details.html` with `order`, `canCancel` flag |

**Bruno:**
```
GET http://localhost:8080/orders/7
Cookie: JSESSIONID={{session_id}}
```

---

#### 22. `GET /orders/{id}/cancel`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER (own) / ADMIN |
| **Description** | Cancellation confirmation page. If the order is not in a cancellable state (already shipped/delivered/cancelled), redirects back to the order detail page instead. |
| **Output** | `orders/cancel-confirm.html` with `order` |

**Bruno:**
```
GET http://localhost:8080/orders/7/cancel
Cookie: JSESSIONID={{session_id}}
```

---

#### 23. `POST /orders/{id}/cancel`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER (own) / ADMIN |
| **Description** | Cancels the order — sets status to `CANCELLED` and restores inventory. Fails if order is not in a cancellable state. |
| **Path param** | `id` — order ID |
| **Success output** | HTTP 302 → `/orders/{id}` with flash `success` |
| **Failure output** | HTTP 302 → `/orders/{id}` with flash `error` |

**Bruno:**
```
POST http://localhost:8080/orders/7/cancel
Cookie: JSESSIONID={{session_id}}
```

---

### FEEDBACK

---

#### 24. `GET /feedback`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Feedback form page. Loads all products for the optional product dropdown and the user's own past submissions shown below the form. |
| **Output** | `feedback/form.html` with `products`, `myFeedback` list |

**Bruno:**
```
GET http://localhost:8080/feedback
Cookie: JSESSIONID={{session_id}}
```

---

#### 25. `POST /feedback`
| Field | Value |
|-------|-------|
| **Access** | CUSTOMER |
| **Description** | Submit feedback. `productId` is optional (general store feedback if omitted). Rating must be 1–5. Timestamp set automatically by `@PrePersist`. |
| **Form params** | `productId` (optional), `rating` (1–5, required), `comment` |
| **Success output** | HTTP 302 → `/feedback` with flash `success` |
| **Failure output** | HTTP 302 → `/feedback` with flash `error` (invalid rating) |

**Bruno — feedback for a specific product:**
```
POST http://localhost:8080/feedback
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

productId=3&rating=5&comment=Excellent quality, fast delivery!
```

**Bruno — general store feedback (no product):**
```
POST http://localhost:8080/feedback
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{session_id}}

rating=4&comment=Great shopping experience overall.
```

---

### ADMIN — DASHBOARD & PRODUCTS

---

#### 26. `GET /admin/dashboard`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Admin home. Shows: total products, total orders, total customers, today's sales revenue, low-stock alerts (qty < 5), last 10 orders, last 5 feedback entries. |
| **Output** | `admin/dashboard.html` with all KPI attributes |

**Bruno:**
```
GET http://localhost:8080/admin/dashboard
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 27. `GET /admin/products`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | List all products in the system with name, category, price, stock. Links to edit/delete each product. |
| **Output** | `admin/products/list.html` with `products` list |

**Bruno:**
```
GET http://localhost:8080/admin/products
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 28. `GET /admin/products/add`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Renders the add-product form with a blank `Product` object and all `Category` enum values for the dropdown. |
| **Output** | `admin/products/add.html` |

**Bruno:**
```
GET http://localhost:8080/admin/products/add
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 29. `POST /admin/products/add`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Create a new product. `@PrePersist` sets `createdDate` and `updatedDate` automatically. |
| **Form params** | `name`, `description`, `price`, `quantityOnHand`, `category` |
| **Success output** | HTTP 302 → `/admin/products` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/admin/products/add
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{admin_session_id}}

name=Mechanical Keyboard&description=RGB backlit, tactile switches&price=1299.00&quantityOnHand=30&category=ELECTRONICS
```

---

#### 30. `GET /admin/products/edit/{id}`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Load the edit form pre-filled with the existing product data. |
| **Path param** | `id` — product ID |
| **Output** | `admin/products/edit.html` with `product`, `categories` |

**Bruno:**
```
GET http://localhost:8080/admin/products/edit/3
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 31. `POST /admin/products/edit/{id}`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Update an existing product. Re-fetches the managed entity from DB, copies new field values onto it, then saves — ensuring a JPA UPDATE (not INSERT). `@PreUpdate` refreshes `updatedDate`. |
| **Form params** | `name`, `description`, `price`, `quantityOnHand`, `category` |
| **Success output** | HTTP 302 → `/admin/products` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/admin/products/edit/3
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID={{admin_session_id}}

name=Wireless Mouse Pro&description=Ergonomic, 2.4GHz, DPI adjustable&price=899.00&quantityOnHand=45&category=ELECTRONICS
```

---

#### 32. `POST /admin/products/delete/{id}`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Delete a product by ID. Will fail with a DB constraint error if the product is referenced by existing order items or feedback. |
| **Path param** | `id` — product ID |
| **Success output** | HTTP 302 → `/admin/products` with flash `success` |

**Bruno:**
```
POST http://localhost:8080/admin/products/delete/12
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 33. `GET /admin/feedback`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | View all customer feedback across all users and products, sorted newest-first. Shows username, product name (or "General"), rating, comment, date. |
| **Output** | `admin/feedback.html` with `feedbackList` |

**Bruno:**
```
GET http://localhost:8080/admin/feedback
Cookie: JSESSIONID={{admin_session_id}}
```

---

### ADMIN — SALES ANALYTICS

---

#### 34. `GET /admin/sales`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Full sales analytics dashboard. Loads all four period totals, fast-moving top-10 products, slow-moving bottom-10 products (including never-ordered), and revenue by category. |
| **Output** | `admin/sales/dashboard.html` with `weeklySales`, `monthlySales`, `quarterlySales`, `yearlySales`, `fastMovingProducts`, `slowMovingProducts`, `salesByCategory` |

**Bruno:**
```
GET http://localhost:8080/admin/sales
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 35. `GET /admin/sales/weekly`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Revenue total for the rolling last-7-days window. Excludes CANCELLED orders. |
| **Output** | `admin/sales/dashboard.html` with `weeklySales` (BigDecimal) and `period = "Weekly"` |

**Bruno:**
```
GET http://localhost:8080/admin/sales/weekly
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 36. `GET /admin/sales/monthly`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Revenue total for the rolling last-30-days window. |
| **Output** | `admin/sales/dashboard.html` with `monthlySales`, `period = "Monthly"` |

**Bruno:**
```
GET http://localhost:8080/admin/sales/monthly
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 37. `GET /admin/sales/quarterly`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Revenue total for the rolling last-90-days window. |
| **Output** | `admin/sales/dashboard.html` with `quarterlySales`, `period = "Quarterly"` |

**Bruno:**
```
GET http://localhost:8080/admin/sales/quarterly
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 38. `GET /admin/sales/yearly`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Revenue total for the rolling last-365-days window. |
| **Output** | `admin/sales/dashboard.html` with `yearlySales`, `period = "Yearly"` |

**Bruno:**
```
GET http://localhost:8080/admin/sales/yearly
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 39. `GET /admin/sales/products`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Fast-moving (top 10 by units sold) and slow-moving (bottom 10 + never-ordered) product ranking. |
| **Output** | `admin/sales/dashboard.html` with `fastMovingProducts` and `slowMovingProducts` — each a `List<Map<String, Object>>` with keys `product` and `totalSold` |

**Bruno:**
```
GET http://localhost:8080/admin/sales/products
Cookie: JSESSIONID={{admin_session_id}}
```

---

### ADMIN — REPORTS

---

#### 40. `GET /admin/reports`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Reports hub — navigation page linking to all individual report types. |
| **Output** | `admin/reports/dashboard.html` |

**Bruno:**
```
GET http://localhost:8080/admin/reports
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 41. `GET /admin/reports/sales`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Date-range sales report. Defaults to last 30 days if no params given. Returns orders in range with total revenue and average order value. |
| **Query params** | `from` (ISO date, optional e.g. `2026-01-01`), `to` (ISO date, optional) |
| **Output** | `admin/reports/sales.html` with `orders`, `totalOrderCount`, `totalRevenue`, `avgOrderValue`, `fromDate`, `toDate` |

**Bruno — last 30 days (default):**
```
GET http://localhost:8080/admin/reports/sales
Cookie: JSESSIONID={{admin_session_id}}
```

**Bruno — specific date range:**
```
GET http://localhost:8080/admin/reports/sales?from=2026-05-01&to=2026-05-31
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 42. `GET /admin/reports/products`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Full product listing report — all products with name, category, price, stock quantity. Same data as inventory report, rendered in inventory template. |
| **Output** | `admin/reports/inventory.html` with `products` list |

**Bruno:**
```
GET http://localhost:8080/admin/reports/products
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 43. `GET /admin/reports/customers`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | All CUSTOMER accounts with their order count. Shows total customers and count of "active" customers (those who placed at least one order). |
| **Output** | `admin/reports/customers.html` with `customers` (list of `CustomerSummary{user, orderCount}`), `totalCustomers`, `activeCustomers` |

**Bruno:**
```
GET http://localhost:8080/admin/reports/customers
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 44. `GET /admin/reports/feedback`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Full feedback report — all feedback entries sorted newest-first. Renders the same `admin/feedback.html` template as `GET /admin/feedback`. |
| **Output** | `admin/feedback.html` with `feedbackList` |

**Bruno:**
```
GET http://localhost:8080/admin/reports/feedback
Cookie: JSESSIONID={{admin_session_id}}
```

---

#### 45. `GET /admin/reports/inventory`
| Field | Value |
|-------|-------|
| **Access** | ADMIN |
| **Description** | Inventory levels report — all products with current `quantityOnHand`. Use this to identify low-stock items (dashboard auto-highlights qty < 5). |
| **Output** | `admin/reports/inventory.html` with `products` list |

**Bruno:**
```
GET http://localhost:8080/admin/reports/inventory
Cookie: JSESSIONID={{admin_session_id}}
```

---

## Bruno — How to Authenticate First

All authenticated endpoints require a valid `JSESSIONID` cookie. Bruno does not follow redirects automatically, so the flow is:

**Step 1 — Login and capture the cookie:**
```
POST http://localhost:8080/login
Content-Type: application/x-www-form-urlencoded

username=admin&password=admin123
```
Copy the `JSESSIONID` value from the `Set-Cookie` response header.

**Step 2 — Use it in all subsequent requests:**
```
Cookie: JSESSIONID=<paste value here>
```

In Bruno you can store this in an environment variable:
```
{{session_id}}   → value copied from login response
```

---

## Sample DB IDs for Testing

Use these real IDs when running Bruno commands (replace if your local DB differs):

| Entity | ID | Details |
|--------|----|---------|
| Product | `1` | First product in DB |
| Product | `3` | Use for add-to-cart / feedback tests |
| Cart item | `5` | Example cart item ID for update/remove |
| Order | `7` | Example order for detail / cancel |
| Admin user | — | `username=admin` |
| Customer user | — | `username=alice` |
