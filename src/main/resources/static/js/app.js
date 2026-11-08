// Small helpers for every page that uses the shared layout (templates/fragments/layout.html):
//  1. The page entrance animation plays only once (not again when live.js reloads <main>).
//  2. Forms: the eye button shows or hides a password, Persian digits become 0-9 while typing,
//     and the submit button shows "busy" after the form is sent (no double sending).
//  3. On phones, the menu button opens the sidebar; the dark layer, the close button,
//     the Escape key or choosing a link closes it.
(function () {
    var root = document.documentElement;

    // ---------- 1. entrance animation ----------
    // The animation in app.css runs while <html> does not have "entered" (it lasts 260 ms).
    setTimeout(function () {
        root.classList.add('entered');
    }, 400);

    // ---------- 2. forms ----------

    // The eye button next to a password box: <button data-password-toggle aria-controls="id-of-the-box">
    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        var input = document.getElementById(button.getAttribute('aria-controls'));
        if (!input) {
            return;
        }
        button.addEventListener('click', function () {
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            var label = show ? button.dataset.hide : button.dataset.show;
            button.setAttribute('aria-pressed', String(show));
            button.setAttribute('aria-label', label);
            button.title = label;
            button.querySelector('.icon-eye').hidden = show;
            button.querySelector('.icon-eye-off').hidden = !show;
            input.focus({preventScroll: true});
        });
    });

    // Persian (۰-۹) and Arabic (٠-٩) digits become 0-9 as they are typed: <input data-ascii-digits>.
    // One character becomes one character, so the cursor stays where it was.
    function asciiDigits(text) {
        return text.replace(/[\u06F0-\u06F9]/g, function (d) { return String(d.charCodeAt(0) - 0x06F0); })
                   .replace(/[\u0660-\u0669]/g, function (d) { return String(d.charCodeAt(0) - 0x0660); });
    }
    document.querySelectorAll('[data-ascii-digits]').forEach(function (input) {
        input.addEventListener('input', function () {
            var fixed = asciiDigits(input.value);
            if (fixed !== input.value) {
                var start = input.selectionStart;
                var end = input.selectionEnd;
                input.value = fixed;
                try {
                    input.setSelectionRange(start, end);
                } catch (e) { /* not a text box */ }
            }
        });
    });

    // <form data-invalid-if="id-of-an-error-box">: if that error box is on the page (the server showed an
    // error), the form's boxes are marked red and point to the error for screen readers.
    document.querySelectorAll('form[data-invalid-if]').forEach(function (form) {
        var errorBox = document.getElementById(form.dataset.invalidIf);
        if (!errorBox) {
            return;
        }
        form.querySelectorAll('input:not([type="hidden"])').forEach(function (input) {
            input.classList.add('is-invalid');
            input.setAttribute('aria-invalid', 'true');
            input.setAttribute('aria-describedby', errorBox.id);
        });
    });

    // <form data-busy-form>: after sending, its submit button shows a spinner and cannot be pressed again.
    // (The "submit" event only fires when the browser's own checks, like "required", have passed.)
    document.querySelectorAll('form[data-busy-form]').forEach(function (form) {
        form.addEventListener('submit', function () {
            var button = form.querySelector('button[type="submit"]');
            if (button) {
                button.classList.add('is-busy');
                button.setAttribute('aria-busy', 'true');
            }
        });
    });
    // Coming back with the browser's Back button must not show a button that is still "busy".
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            document.querySelectorAll('button.is-busy').forEach(function (button) {
                button.classList.remove('is-busy');
                button.removeAttribute('aria-busy');
            });
        }
    });

    // ---------- 3. phone menu ----------
    var openButton = document.querySelector('[data-nav-open]');
    var sidebar = document.getElementById('sidebar');
    if (!openButton || !sidebar) {
        return;
    }

    function open() {
        root.classList.add('nav-open');
        openButton.setAttribute('aria-expanded', 'true');
        // Move the keyboard into the menu once it has slid in.
        var first = sidebar.querySelector('.sidebar-close');
        if (first) {
            setTimeout(function () { first.focus({preventScroll: true}); }, 50);
        }
    }

    function close() {
        if (!root.classList.contains('nav-open')) {
            return;
        }
        root.classList.remove('nav-open');
        openButton.setAttribute('aria-expanded', 'false');
        openButton.focus({preventScroll: true});
    }

    openButton.addEventListener('click', open);

    document.querySelectorAll('[data-nav-close]').forEach(function (element) {
        element.addEventListener('click', close);
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            close();
        }
    });

    // Choosing a page in the menu closes it (the new page loads behind it).
    sidebar.addEventListener('click', function (event) {
        if (event.target.closest('a')) {
            root.classList.remove('nav-open');
        }
    });

    // Turning a phone sideways (or widening the window) must not leave the page locked.
    window.matchMedia('(min-width: 56.01rem)').addEventListener('change', function (query) {
        if (query.matches) {
            root.classList.remove('nav-open');
            openButton.setAttribute('aria-expanded', 'false');
        }
    });
})();
