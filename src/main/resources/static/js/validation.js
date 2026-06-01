// Owner: Aliya | UI/UX | Client-side form validation: required fields, email format, numeric inputs, delete confirmations

document.addEventListener('DOMContentLoaded', function () {

    // Prevent double-submit on all forms
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            const btn = form.querySelector('[type="submit"]');
            if (btn) {
                setTimeout(function () { btn.disabled = true; }, 10);
            }
        });
    });

    // Real-time email validation
    document.querySelectorAll('input[type="email"]').forEach(function (input) {
        input.addEventListener('blur', function () {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (input.value && !emailRegex.test(input.value)) {
                showFieldError(input, 'Please enter a valid email address.');
            } else {
                clearFieldError(input);
            }
        });
    });

    // Real-time password length check
    document.querySelectorAll('input[name="password"]').forEach(function (input) {
        input.addEventListener('blur', function () {
            if (input.value && input.value.length < 6) {
                showFieldError(input, 'Password must be at least 6 characters.');
            } else {
                clearFieldError(input);
            }
        });
    });

    // Numeric fields — reject negative values
    document.querySelectorAll('input[type="number"]').forEach(function (input) {
        input.addEventListener('blur', function () {
            const min = parseFloat(input.getAttribute('min') || '0');
            if (input.value !== '' && parseFloat(input.value) < min) {
                showFieldError(input, 'Value must be at least ' + min + '.');
            } else {
                clearFieldError(input);
            }
        });
    });

    // Required field check on blur
    document.querySelectorAll('[required]').forEach(function (input) {
        input.addEventListener('blur', function () {
            if (!input.value.trim()) {
                showFieldError(input, 'This field is required.');
            } else {
                clearFieldError(input);
            }
        });
    });
});

function showFieldError(input, message) {
    clearFieldError(input);
    input.classList.add('is-invalid');
    const div = document.createElement('div');
    div.className = 'invalid-feedback js-error';
    div.textContent = message;
    input.parentNode.appendChild(div);
}

function clearFieldError(input) {
    input.classList.remove('is-invalid');
    const existing = input.parentNode.querySelector('.js-error');
    if (existing) existing.remove();
}
