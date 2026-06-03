# UserRole.java

**File:** `src/main/java/com/shopping/system/entity/UserRole.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** Java Enum  
**Purpose:** Defines the two roles a user can have in the system: `CUSTOMER` (shopper) and `ADMIN` (store manager). Used in `User.role` and throughout controllers to control access.

---

## Enum Values

```java
public enum UserRole {
    CUSTOMER,
    ADMIN
}
```

| Value | Meaning | Who has it |
|-------|---------|------------|
| `CUSTOMER` | A regular shopper who can browse, cart, order, and give feedback | Default for new registrations |
| `ADMIN` | Store manager who can manage products, view all orders, access sales reports | Seeded manually via DataInitializer |

---

## Why an Enum (Not a String or Integer)?

- **Type safety:** A `String` role field could accidentally be set to `"ADMINISTRATORR"` (typo). An enum makes invalid values a compile error.
- **Readability:** `if (user.getRole() == UserRole.ADMIN)` is clearer than `if (user.getRole().equals("ADMIN"))`.
- **Refactoring safety:** Renaming an enum value is a compile-time change caught across the whole codebase.

---

## How It Is Stored in the DB

In `User.java`:
```java
@Enumerated(EnumType.STRING)
private UserRole role;
```
Stored as `"CUSTOMER"` or `"ADMIN"` (VARCHAR) — not as `0`/`1`. This is intentional: if we ever add a new role (e.g. `VENDOR`) between `CUSTOMER` and `ADMIN`, the ordinal-based values would shift and corrupt existing data.

---

## How It Is Used for Authorization

Every admin-only controller method does:
```java
User user = (User) session.getAttribute("loggedInUser");
if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";
```

The `SessionInterceptor` handles authentication (is the user logged in?) but **not** authorization (does the user have the right role?). Role checks happen manually in each controller.

---

## Forward Linkage

| File | Usage |
|------|-------|
| `User.java` | Type of the `role` field |
| `AuthController` | Redirects ADMIN to admin dashboard, CUSTOMER to customer dashboard after login |
| `CustomerController` | Blocks ADMIN from accessing customer dashboard |
| `AdminController` | Blocks CUSTOMER from accessing admin dashboard |
| `AdminProductController` | `isAdmin()` helper checks this |
| `SalesController` | `isAdmin()` helper checks this |
| `ReportController` | `isAdmin()` helper checks this |
| `FeedbackController` | Admin view requires `ADMIN` role |
| `OrderController` | Admins can see all orders; customers only their own |
| `DataInitializer` | Seeds admin user with `UserRole.ADMIN` |
