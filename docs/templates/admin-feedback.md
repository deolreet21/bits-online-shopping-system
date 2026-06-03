# Template: admin/feedback.html

**File:** `src/main/resources/templates/admin/feedback.html`  
**Owner:** HeenuReet  
**Controller:** `FeedbackController.java` → `GET /admin/feedback`  
**Purpose:** Admin view of all customer feedback — table showing user, product (if any), rating badge (color-coded), comment, and date.

---

## Color-Coded Rating Badge

```html
<span th:class="${feedback.rating >= 4 ? 'badge bg-success' :
                  (feedback.rating == 3 ? 'badge bg-warning text-dark' :
                   'badge bg-danger')}"
      th:text="${feedback.rating} + '★'"></span>
```

Nested ternary in Thymeleaf:
- Rating ≥ 4 → green badge (`bg-success`)
- Rating = 3 → yellow badge (`bg-warning text-dark`)
- Rating < 3 → red badge (`bg-danger`)

`th:text="${feedback.rating} + '★'"` — shows e.g. `"4★"`.

---

## Nullable Product Display

```html
<td th:text="${feedback.product != null ? feedback.product.name : 'General'}"></td>
```

`feedback.product` is nullable (`@ManyToOne(optional = true)` / nullable FK). General feedback shows "General" in the Product column.

---

## Date Formatting

```html
<td th:text="${#temporals.format(feedback.feedbackDate, 'dd MMM yyyy, HH:mm')}"></td>
```

Same `#temporals.format()` pattern as order dates.

---

## Sorted by Date

The `feedbackList` from `feedbackService.getAllFeedback()` calls `feedbackRepository.findAllByOrderByFeedbackDateDesc()` — already sorted newest first. No explicit sort in the template.

---

## Model Attributes Expected

| Attribute | Type | Source |
|-----------|------|--------|
| `feedbackList` | `List<Feedback>` | `feedbackService.getAllFeedback()` |
| `currentUser` | `User` | Session |
