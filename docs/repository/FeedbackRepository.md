# FeedbackRepository.java

**File:** `src/main/java/com/shopping/system/repository/FeedbackRepository.java`  
**Package:** `com.shopping.system.repository`  
**Owner:** HeenuReet  
**Type:** Spring Data JPA Repository Interface  
**Purpose:** Provides database access for `Feedback` entities. Supports retrieving feedback filtered by user, product, or sorted by date for the admin dashboard.

---

## Interface Declaration

```java
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> { ... }
```

---

## Custom Methods

### `findByUserId`
```java
List<Feedback> findByUserId(Long userId);
```
**Generated SQL:** `SELECT * FROM feedback WHERE user_id = ?`  
**Used by:** `FeedbackService.getFeedbackByUser()` → `FeedbackController.feedbackForm()` (shows user's own previous feedback) and `CustomerController.profilePage()` (feedback count).

---

### `findByProductId`
```java
List<Feedback> findByProductId(Long productId);
```
**Generated SQL:** `SELECT * FROM feedback WHERE product_id = ?`  
**Used by:** `FeedbackService.getFeedbackByProduct()` — available for product detail pages to show reviews (can be wired in future).

---

### `findAllByOrderByFeedbackDateDesc`
```java
List<Feedback> findAllByOrderByFeedbackDateDesc();
```
**Generated SQL:** `SELECT * FROM feedback ORDER BY feedback_date DESC`

**Breaking down the method name:**
- `findAll` — no WHERE filter
- `By` — required separator for the `OrderBy` clause
- `OrderBy` — specifies sorting
- `FeedbackDate` — the field to sort on
- `Desc` — descending order (newest first)

This is Spring Data's method name DSL for sorting — no `@Query` annotation needed.

**Used by:** `FeedbackService.getAllFeedback()` → `FeedbackController.adminFeedback()`, `DashboardService.getRecentFeedback()`

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `FeedbackService` | `save`, `findByUserId`, `findByProductId`, `findAllByOrderByFeedbackDateDesc` |
