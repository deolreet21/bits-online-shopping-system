# validation.js

**File:** `src/main/resources/static/js/validation.js`  
**Owner:** Aliya  
**Type:** Vanilla JavaScript  
**Purpose:** Client-side form validation on top of HTML5 built-in validation. Adds real-time feedback on blur, prevents double-submit, and displays Bootstrap-styled error messages inline below inputs.

---

## Why Client-Side Validation in Addition to Server-Side?

The server already validates all inputs (e.g., rating 1–5 check in `FeedbackController`, duplicate username check in `AuthController`). Client-side validation adds:

1. **Instant feedback** — user sees the error before submitting
2. **Reduced server load** — invalid forms never reach the server
3. **Better UX** — red borders appear while the user is still filling in the form

**Security note:** Client-side validation can always be bypassed (disable JS, use curl). Server-side validation is the security layer. Client-side is the UX layer.

---

## `DOMContentLoaded` Wrapper

```javascript
document.addEventListener('DOMContentLoaded', function () {
    // all setup code here
});
```

Waits until the full HTML DOM is parsed before attaching event listeners. Without this, `document.querySelectorAll('form')` would return empty if the script runs before the `<body>` is fully rendered.

The `<script>` tags are at the bottom of `<body>` in all templates anyway, but this is an extra safety net.

---

## Double-Submit Prevention

```javascript
document.querySelectorAll('form').forEach(function (form) {
    form.addEventListener('submit', function () {
        const btn = form.querySelector('[type="submit"]');
        if (btn) {
            setTimeout(function () { btn.disabled = true; }, 10);
        }
    });
});
```

Attaches to every `<form>` on the page. On submit, disables the submit button after 10ms.

**Why `setTimeout(..., 10)` and not immediately?** Disabling the button immediately would prevent the form from submitting in some browsers — the click event is cancelled before the form data is collected. The 10ms delay lets the submit fire first, then disables the button to block re-clicks.

**Problem it solves:** Without this, clicking "Place Order" twice would create two orders.

---

## Email Validation (on blur)

```javascript
input.addEventListener('blur', function () {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (input.value && !emailRegex.test(input.value)) {
        showFieldError(input, 'Please enter a valid email address.');
    } else {
        clearFieldError(input);
    }
});
```

**`blur`** event fires when the user leaves the field (clicks elsewhere). Not `input` (fires on every keypress) — showing an error while typing is annoying.

**Regex breakdown: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`**
- `^` — start of string
- `[^\s@]+` — one or more chars that are not whitespace or `@`
- `@` — literal `@`
- `[^\s@]+` — domain name (no spaces, no `@`)
- `\.` — literal dot
- `[^\s@]+` — TLD (`.com`, `.org`, etc.)
- `$` — end of string

This is intentionally simple — catches obvious typos like `user@` or `user.com`. The server's `@Email` annotation (via Bean Validation) or database unique constraint is the authoritative validator.

---

## Password Length Check

```javascript
if (input.value && input.value.length < 6) {
    showFieldError(input, 'Password must be at least 6 characters.');
}
```

`input.value &&` — only validate if the user has typed something. No error if field is empty (that's handled by the `required` check below).

`length < 6` matches `minlength="6"` in `register.html`.

---

## Numeric Min Validation

```javascript
const min = parseFloat(input.getAttribute('min') || '0');
if (input.value !== '' && parseFloat(input.value) < min) {
    showFieldError(input, 'Value must be at least ' + min + '.');
}
```

Reads the `min` attribute from the HTML element (e.g., `min="1"` on rating input, `min="1"` on cart quantity). Falls back to `0` if no `min` attribute.

`parseFloat(input.getAttribute('min') || '0')` — if `getAttribute` returns `null`, `null || '0'` = `'0'`.

---

## Required Field Check

```javascript
document.querySelectorAll('[required]').forEach(function (input) {
    input.addEventListener('blur', function () {
        if (!input.value.trim()) {
            showFieldError(input, 'This field is required.');
        } else {
            clearFieldError(input);
        }
    });
});
```

Attaches to all elements with the `required` attribute. `trim()` — a space-only input should still fail.

---

## Error Display Functions

```javascript
function showFieldError(input, message) {
    clearFieldError(input);                        // remove existing error first
    input.classList.add('is-invalid');             // Bootstrap red border
    const div = document.createElement('div');
    div.className = 'invalid-feedback js-error';   // Bootstrap error text style
    div.textContent = message;
    input.parentNode.appendChild(div);
}

function clearFieldError(input) {
    input.classList.remove('is-invalid');
    const existing = input.parentNode.querySelector('.js-error');
    if (existing) existing.remove();
}
```

**`is-invalid`** — Bootstrap 5 class that:
1. Turns the input border red
2. Shows `div.invalid-feedback` elements (which are hidden by default)

**`js-error`** — custom class added to distinguish JS-injected errors from server-side Thymeleaf error spans. Allows `clearFieldError` to find and remove only JS errors.

**`input.parentNode.appendChild(div)`** — adds the error message div as a sibling of the input, inside the `div.input-group` wrapper. Bootstrap's `.invalid-feedback` appears just below the input.

---

## Bootstrap Integration

This file works with Bootstrap 5's form validation classes:

| Class | Effect |
|-------|--------|
| `is-invalid` | Red border on input |
| `invalid-feedback` | Shows error text below input (hidden by default) |
| `is-valid` | Green border (not used in this project) |
