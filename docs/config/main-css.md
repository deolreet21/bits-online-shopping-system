# main.css

**File:** `src/main/resources/static/css/main.css`  
**Owner:** Mehwish  
**Type:** CSS stylesheet  
**Purpose:** Custom CSS that extends Bootstrap 5. Defines CSS variables for consistent colors, overrides Bootstrap's default card/button/form/table styles for the e-Kiosk look, and adds project-specific status badge colors.

---

## How It Works With Bootstrap

Every template loads Bootstrap first, then this file:

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"/>
<link rel="stylesheet" th:href="@{/css/main.css}"/>
```

CSS cascade: later stylesheets win. `main.css` overrides Bootstrap's defaults without modifying Bootstrap itself.

---

## CSS Custom Properties (Variables)

```css
:root {
    --primary: #0d6efd;
    --primary-dark: #0a58ca;
    --accent: #198754;
    --bg-light: #f8f9fa;
    --text-muted: #6c757d;
}
```

**`:root`** — the `<html>` element. CSS variables defined here are available everywhere.

**Why these match Bootstrap?** `#0d6efd` is Bootstrap 5's `$blue` / `--bs-primary`. These variables allow one-place color changes if the brand color ever needs to update.

---

## Body

```css
body {
    background-color: #f4f6f9;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
```

`#f4f6f9` — a slightly blue-tinted grey, softer than Bootstrap's `#f8f9fa`. Creates visual separation between the navbar/cards and the page background.

Font stack falls back: Segoe UI (Windows) → Tahoma → Geneva → Verdana → system sans-serif.

---

## Navbar

```css
.navbar {
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.navbar-brand {
    font-weight: 700;
    font-size: 1.3rem;
    letter-spacing: 0.5px;
}
```

Adds a subtle drop shadow under the navbar to create visual depth. The brand name "e-Kiosk" is bold and slightly larger.

---

## Cards

```css
.card {
    border: none;
    border-radius: 10px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.07);
}
.card-header {
    background-color: #fff;
    border-bottom: 1px solid #e9ecef;
}
```

Bootstrap's default cards have a `1px solid` border. This removes it and replaces it with a shadow — cleaner, modern look. `border-radius: 10px` rounds all four corners.

---

## Buttons

```css
.btn {
    border-radius: 6px;
    font-weight: 500;
}
```

Slightly less rounded than Bootstrap's default `6px` — subtle change for a more professional look. `font-weight: 500` (medium weight) makes button text slightly bolder than body text.

---

## Tables

```css
.table th {
    font-weight: 600;
    font-size: 0.85rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}
.table td {
    vertical-align: middle;
}
```

Table headers become small caps (uppercase + letter-spacing). `vertical-align: middle` keeps cell content centered vertically when rows have varying heights (e.g., action buttons next to text).

---

## Forms

```css
.form-control:focus, .form-select:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 0.2rem rgba(13,110,253,0.15);
}
```

On focus, the input border turns blue and a soft blue glow appears. This is Bootstrap's default behavior but the color is explicitly set here to match the custom primary variable.

---

## Order Status Badges

```css
.status-pending   { background-color: #ffc107; color: #000; }
.status-confirmed { background-color: #0d6efd; color: #fff; }
.status-shipped   { background-color: #0dcaf0; color: #000; }
.status-delivered { background-color: #198754; color: #fff; }
.status-cancelled { background-color: #dc3545; color: #fff; }
```

Used in order templates:
```html
<span th:class="${'badge fs-6 status-' + order.status.name().toLowerCase()}">
```

`order.status.name()` returns `"PENDING"`, `.toLowerCase()` makes it `"pending"`, concatenated: `"badge fs-6 status-pending"`.

**Why dynamic class names?** A single Thymeleaf expression handles all 5 statuses without `th:if` / `th:switch` blocks.

---

## Served As Static Content

The file is at `src/main/resources/static/css/main.css`. Spring Boot automatically serves everything in `/static/` at the root URL:

```
/static/css/main.css  →  http://localhost:8080/css/main.css
```

Thymeleaf's `@{/css/main.css}` generates `href="/css/main.css"` — the browser fetches it from the Spring Boot server.

---

## What Bootstrap Provides (Not in main.css)

main.css only extends/overrides. Bootstrap provides:
- Grid system (`container`, `row`, `col-*`)
- All utility classes (`d-flex`, `mb-3`, `text-muted`, etc.)
- Component base styles (navbar, card, table, badge, alert, form-control)
- Responsive breakpoints
- Bootstrap Icons (separate CDN link: `bootstrap-icons@1.10.5`)
