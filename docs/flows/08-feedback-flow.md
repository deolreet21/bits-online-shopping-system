# Flow 08: Feedback Flow

**End-to-End Trace: Customer submits feedback → saved with optional product link → admin views all feedback**

---

## ASCII Flow Diagram

```
Browser                  Controller              Service            Repository          DB
  │                          │                      │                   │               │
  │  GET /feedback            │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                    FeedbackController            │                   │               │
  │                    .feedbackForm()               │                   │               │
  │                          │  getAllProducts()     │                   │               │
  │                          │─────────────────────────────────────────>│               │
  │                          │                      │     SELECT * FROM products────────>│
  │                          │                      │                   │<──────────────│
  │                          │  getFeedbackByUser() │                   │               │
  │                          │─────────────────────────────────────────>│               │
  │                          │                      │     SELECT * FROM feedback        │
  │                          │                      │     WHERE user_id = ?─────────────>│
  │                          │                      │                   │<──────────────│
  │  feedback/form.html       │                      │                   │               │
  │<──────────────────────────│                      │                   │               │
  │  (product dropdown,       │                      │                   │               │
  │   my past feedback list)  │                      │                   │               │
  │                          │                      │                   │               │
  │  POST /feedback           │                      │                   │               │
  │  {productId?, rating,     │                      │                   │               │
  │   comment}                │                      │                   │               │
  │──────────────────────────>│                      │                   │               │
  │                    FeedbackController            │                   │               │
  │                    .submitFeedback()             │                   │               │
  │                    validate rating 1–5           │                   │               │
  │                          │  submitFeedback()     │                   │               │
  │                          │─────────────────────>│                   │               │
  │                          │               FeedbackService            │               │
  │                          │               .submitFeedback()          │               │
  │                          │                      │  findById(productId)             │
  │                          │                      │─────────────────────────────────>│
  │                          │                      │           SELECT products WHERE id=?
  │                          │                      │                   │<──────────────│
  │                          │                      │  save(feedback)   │               │
  │                          │                      │─────────────────────────────────>│
  │                          │                      │                   │  INSERT feedback
  │                          │                      │                   │<──────────────│
  │  redirect:/feedback       │                      │                   │               │
  │  (flash: "Thank you!")    │                      │                   │               │
  │<──────────────────────────│                      │                   │               │
```

---

## Step-by-Step Walkthrough

### Step 1: `GET /feedback` — Show Feedback Form

**File:** `FeedbackController.java`
```java
@GetMapping("/feedback")
public String feedbackForm(Model model, HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user == null) return "redirect:/login";

    model.addAttribute("products", productService.getAllProducts());
    model.addAttribute("currentUser", user);
    model.addAttribute("myFeedback", feedbackService.getFeedbackByUser(user.getId()));
    return "feedback/form";
}
```

The form loads two things:
- **Product dropdown** — all products so the customer can optionally link feedback to a specific item
- **myFeedback** — the customer's own past submissions, shown below the form so they can see what they've already written

Session guard: if no `loggedInUser` in session, redirect to `/login`. All customer-facing pages apply this same pattern.

**Template:** `feedback/form.html`
- Dropdown populated with `th:each="p : ${products}"` — product is optional (`required = false` on the param)
- Star rating input (1–5) enforced in the template and re-validated in the controller
- Past feedback rendered in a table below the form

---

### Step 2: `POST /feedback` — Validate and Submit

**File:** `FeedbackController.java`
```java
@PostMapping("/feedback")
public String submitFeedback(@RequestParam(required = false) Long productId,
                             @RequestParam int rating,
                             @RequestParam String comment,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
```

**Step 2a: Rating validation in controller**
```java
if (rating < 1 || rating > 5) {
    redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5.");
    return "redirect:/feedback";
}
```

Validation happens at the controller level (not via `@Valid`) because the rating is a plain `int` param, not a bound model object. The template also enforces this via `min="1" max="5"` on the input, but the controller provides server-side defence.

**Step 2b: Delegate to service**
```java
feedbackService.submitFeedback(user, productId, rating, comment);
redirectAttributes.addFlashAttribute("success", "Your feedback has been submitted. Thank you!");
return "redirect:/feedback";
```

POST-Redirect-GET: after saving, redirect back to `GET /feedback`. The flash attribute `"success"` survives exactly one redirect and is shown as a green alert on the reloaded form.

---

### Step 3: `FeedbackService.submitFeedback()` — Save to DB

**File:** `FeedbackService.java`
```java
public Feedback submitFeedback(User user, Long productId, int rating, String comment) {
    Feedback feedback = new Feedback();
    feedback.setUser(user);
    feedback.setRating(rating);
    feedback.setComment(comment);

    if (productId != null) {
        Optional<Product> product = productRepository.findById(productId);
        product.ifPresent(feedback::setProduct);
    }

    return feedbackRepository.save(feedback);
}
```

**`productId` is nullable.** If the customer didn't select a product, `feedback.product` stays `null` — this is valid (general store feedback). The `@JoinColumn(nullable = true)` on the entity allows it.

**Why `findById` instead of trusting the ID?** The product dropdown is populated from the DB, but an ID could be manipulated in the browser. `findById` ensures the product actually exists before linking; `product.ifPresent(...)` silently ignores a non-existent ID (sets no product rather than throwing).

**`@PrePersist` on Feedback:**
```java
@PrePersist
public void prePersist() {
    this.feedbackDate = LocalDateTime.now();
}
```

`feedbackDate` is set automatically by JPA before the `INSERT`, so the controller/service never manually set the timestamp.

---

### Step 4: Admin Views All Feedback

**File:** `FeedbackController.java`
```java
@GetMapping("/admin/feedback")
public String adminFeedback(Model model, HttpSession session) {
    User user = (User) session.getAttribute("loggedInUser");
    if (user == null) return "redirect:/login";
    if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";

    List<Feedback> feedbackList = feedbackService.getAllFeedback();
    model.addAttribute("feedbackList", feedbackList);
    return "admin/feedback";
}
```

Double guard: session must exist **and** role must be `ADMIN`. A logged-in customer hitting `/admin/feedback` is redirected to their own dashboard rather than seeing a 403 error page.

**`getAllFeedback()` ordering:**
```java
// FeedbackRepository.java
List<Feedback> findAllByOrderByFeedbackDateDesc();
```

Spring Data derives the query from the method name — `OrderByFeedbackDateDesc` translates to `ORDER BY feedback_date DESC`. Newest feedback appears at the top of the admin table.

**Template:** `admin/feedback.html`
- Shows username, product name (or "General" if null), star rating, comment, date
- `th:text="${f.product != null ? f.product.name : 'General'}"` — null-safe product display

---

## What's Optional in This Flow

| Data | Required? | Behaviour if absent |
|------|-----------|---------------------|
| `productId` | No | Feedback saved as general (no product link) |
| `comment` | No (TEXT column, no `nullable=false`) | Saved as null |
| `rating` | Yes | Controller rejects < 1 or > 5 |

---

## Files Involved

| File | Role |
|------|------|
| `feedback/form.html` | Feedback form + past feedback list |
| `admin/feedback.html` | Admin view of all feedback |
| `FeedbackController.java` | `GET /feedback`, `POST /feedback`, `GET /admin/feedback` |
| `FeedbackService.java` | `submitFeedback()`, `getFeedbackByUser()`, `getAllFeedback()` |
| `FeedbackRepository.java` | `findByUserId()`, `findAllByOrderByFeedbackDateDesc()` |
| `ProductRepository.java` | `findById()` to validate optional product link |
| `Feedback.java` | Entity with nullable product FK, `@PrePersist` timestamp |
