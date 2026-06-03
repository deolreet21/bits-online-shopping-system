# FeedbackService.java

**File:** `src/main/java/com/shopping/system/service/FeedbackService.java`  
**Package:** `com.shopping.system.service`  
**Owner:** HeenuReet  
**Type:** Spring Service  
**Purpose:** Business logic for feedback submission and retrieval. Validates the optional product link and delegates DB operations to `FeedbackRepository`.

---

## Methods

### `submitFeedback`
```java
public Feedback submitFeedback(User user, Long productId, int rating, String comment) {
    Feedback feedback = new Feedback();
    feedback.setUser(user);
    feedback.setRating(rating);
    feedback.setComment(comment);

    if (productId != null) {
        Optional<Product> product = productRepository.findById(productId);
        product.ifPresent(feedback::setProduct);  // method reference: sets product if found
    }

    return feedbackRepository.save(feedback);
}
```

**`productId` is optional** — if null, feedback is general (not tied to a product). If provided, the product is looked up and linked. `product.ifPresent(feedback::setProduct)` is a concise way to set the product only if it exists.

**`@PrePersist` in `Feedback`** sets `feedbackDate` automatically before save.

---

### `getFeedbackByUser`
```java
public List<Feedback> getFeedbackByUser(Long userId) {
    return feedbackRepository.findByUserId(userId);
}
```
Used in `FeedbackController.feedbackForm()` to show the customer their own previous submissions, and in `CustomerController.profilePage()` to get the feedback count.

---

### `getAllFeedback`
```java
public List<Feedback> getAllFeedback() {
    return feedbackRepository.findAllByOrderByFeedbackDateDesc();
}
```
Returns all feedback sorted by date (newest first). Used for admin review page and dashboard widget.

---

### `getFeedbackByProduct`
```java
public List<Feedback> getFeedbackByProduct(Long productId) {
    return feedbackRepository.findByProductId(productId);
}
```
Available for product-specific reviews — not yet wired to a template but ready.

---

## Forward Linkage

| File | Methods Used |
|------|-------------|
| `FeedbackController` | `submitFeedback`, `getFeedbackByUser`, `getAllFeedback` |
| `CustomerController` | `getFeedbackByUser` (for count) |
| `DashboardService` | `getAllFeedback` (limits to 5 for dashboard widget) |
| `ReportController` | `getAllFeedback` |
