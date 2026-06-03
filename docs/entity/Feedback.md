# Feedback.java

**File:** `src/main/java/com/shopping/system/entity/Feedback.java`  
**Package:** `com.shopping.system.entity`  
**Owner:** HeenuReet  
**Type:** JPA Entity  
**Purpose:** Stores feedback submitted by customers. A feedback entry has a star rating (1–5), a text comment, an optional link to a specific product, and a timestamp. Used for admin review and displayed on the admin dashboard.

---

## Class-Level Annotations

```java
@Entity
@Table(name = "feedback")
```

---

## Fields

### `user`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```
Every feedback entry must be tied to a user — anonymous feedback is not allowed. FK is `user_id` in the `feedback` table.

---

### `product`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = true)
private Product product;
```
**`nullable = true`** — feedback can be either:
1. **Product-specific**: "I bought the Samsung TV — 5 stars, great picture quality"
2. **General/store feedback**: "The shipping was fast!" (no product linked)

This is the only nullable FK in the system. The `FeedbackController` handles `productId == null` gracefully.

---

### `rating`
```java
@Column(nullable = false)
private Integer rating;
```
Expected to be 1–5. This constraint is **not enforced at the DB level** — it's validated in `FeedbackController.submitFeedback()`:
```java
if (rating < 1 || rating > 5) {
    redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5.");
    return "redirect:/feedback";
}
```

---

### `comment`
```java
@Column(columnDefinition = "TEXT")
private String comment;
```
`TEXT` type allows long comments. A `VARCHAR(255)` would cut off detailed feedback.

---

### `feedbackDate`
```java
@Column(name = "feedback_date")
private LocalDateTime feedbackDate;
```
Set automatically by `@PrePersist`.

---

## Lifecycle Method

```java
@PrePersist
public void prePersist() {
    this.feedbackDate = LocalDateTime.now();
}
```

---

## Forward Linkage

| File | How |
|------|-----|
| `FeedbackRepository` | Queries by user, product, or date |
| `FeedbackService` | `submitFeedback()`, `getAllFeedback()` |
| `FeedbackController` | HTTP handlers for submit and admin view |
| `DashboardService` | `getRecentFeedback()` for admin dashboard widget |
| `ReportController` | Feedback report page |
| Templates | `feedback/form.html`, `admin/feedback.html` |

---

## DB Column Summary

| Java Field | DB Column | Type | Constraints |
|------------|-----------|------|-------------|
| id | id | BIGINT | PK, AUTO_INCREMENT |
| user | user_id | BIGINT | FK → users.id, NOT NULL |
| product | product_id | BIGINT | FK → products.id, nullable |
| rating | rating | INT | NOT NULL |
| comment | comment | TEXT | nullable |
| feedbackDate | feedback_date | DATETIME | nullable |
