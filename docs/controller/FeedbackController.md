# FeedbackController.java

**File:** `src/main/java/com/shopping/system/controller/FeedbackController.java`  
**Package:** `com.shopping.system.controller`  
**Owner:** HeenuReet  
**Type:** Spring MVC Controller  
**Purpose:** Handles customer feedback submission and the admin view of all feedback. The feedback form allows optional product selection and requires a star rating (1–5).

---

## Endpoints

### `GET /feedback` — Show Feedback Form
```java
@GetMapping("/feedback")
public String feedbackForm(Model model, HttpSession session) {
    model.addAttribute("products", productService.getAllProducts());
    model.addAttribute("currentUser", user);
    model.addAttribute("myFeedback", feedbackService.getFeedbackByUser(user.getId()));
    return "feedback/form";
}
```

Three things are loaded:
1. All products — for the optional product dropdown
2. Current user — for the navbar
3. User's own previous feedback — shown below the form so they can see what they've already submitted

---

### `POST /feedback` — Submit Feedback
```java
@PostMapping("/feedback")
public String submitFeedback(@RequestParam(required = false) Long productId,
                             @RequestParam int rating,
                             @RequestParam String comment, ...) {
    if (rating < 1 || rating > 5) {
        redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5.");
        return "redirect:/feedback";
    }
    feedbackService.submitFeedback(user, productId, rating, comment);
    redirectAttributes.addFlashAttribute("success", "Your feedback has been submitted. Thank you!");
    return "redirect:/feedback";
}
```

`productId` is `required = false` — general feedback (no product) is valid.  
Rating validation is server-side (in addition to the HTML `min="1" max="5"` attribute in the form).

---

### `GET /admin/feedback` — Admin View
```java
@GetMapping("/admin/feedback")
public String adminFeedback(Model model, HttpSession session) {
    if (user.getRole() != UserRole.ADMIN) return "redirect:/customer/dashboard";
    List<Feedback> feedbackList = feedbackService.getAllFeedback();
    model.addAttribute("feedbackList", feedbackList);
    return "admin/feedback";
}
```

Returns all feedback sorted by date. The template color-codes ratings: ≥4 = green, 3 = yellow, <3 = red.
