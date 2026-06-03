# Template: feedback/form.html

**File:** `src/main/resources/templates/feedback/form.html`  
**Owner:** HeenuReet  
**Controller:** `FeedbackController.java` → `GET /feedback`  
**Purpose:** Feedback submission form with optional product dropdown, star rating input, and comment textarea. Also shows the user's own previous feedback below the form.

---

## Optional Product Dropdown

```html
<select class="form-select" name="productId">
    <option value="">-- General Feedback (no specific product) --</option>
    <option th:each="product : ${products}"
            th:value="${product.id}"
            th:text="${product.name}"></option>
</select>
```

`name="productId"` — maps to `@RequestParam(required = false) Long productId` in the controller. If the user selects "General Feedback", the submitted value is `""` (empty string), Spring converts that to `null` for `Long productId`.

`th:each` iterates all products from `productService.getAllProducts()`.

---

## Star Rating Input

```html
<input type="number" class="form-control" name="rating"
       min="1" max="5" placeholder="1-5" required/>
```

HTML5 number input with `min="1" max="5"`. The browser renders a spinner with up/down arrows. `validation.js` catches values outside range on blur.

The server also validates: `if (rating < 1 || rating > 5)` → error flash.

---

## Comment Textarea

```html
<textarea class="form-control" name="comment" rows="4"
          placeholder="Share your experience..." required></textarea>
```

Free text, `required`. The server receives it as `@RequestParam String comment`.

---

## User's Previous Feedback

```html
<div th:if="${!#lists.isEmpty(myFeedback)}">
    <h5>Your Previous Feedback</h5>
    <div th:each="fb : ${myFeedback}" class="card mb-2">
        <div class="card-body">
            <div class="d-flex justify-content-between">
                <strong th:text="${fb.product != null ? fb.product.name : 'General'}"></strong>
                <span th:text="'★'.repeat(fb.rating)"></span>
            </div>
            <p th:text="${fb.comment}"></p>
        </div>
    </div>
</div>
```

`fb.product != null ? fb.product.name : 'General'` — ternary to handle nullable product. General feedback shows "General".

`'★'.repeat(fb.rating)` — Thymeleaf calls `String.repeat()` on the star character. Rating 4 → `"★★★★"`. Concise alternative to `#numbers.sequence`.

---

## Flash Messages

```html
<div th:if="${success}" class="alert alert-success">
    <span th:text="${success}"></span>
</div>
<div th:if="${error}" class="alert alert-danger">
    <span th:text="${error}"></span>
</div>
```

After POST, `FeedbackController` redirects back to `GET /feedback` with flash attributes. The GET re-renders the form with the flash message visible.

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `products` | `List<Product>` | `productService.getAllProducts()` |
| `myFeedback` | `List<Feedback>` | `feedbackService.getFeedbackByUser(userId)` |
| `currentUser` | `User` | Session |
| `success` / `error` | `String` | Flash attributes |
