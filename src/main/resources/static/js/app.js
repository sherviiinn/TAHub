// Small helpers for every page that uses the shared layout (templates/fragments/layout.html):
//  1. The page entrance animation plays only once (not again when live.js reloads <main>).
//  2. On phones, the menu button opens the sidebar; the dark layer, the close button,
//     the Escape key or choosing a link closes it.
(function () {
    var root = document.documentElement;

    // ---------- 1. entrance animation ----------
    // The animation in app.css runs while <html> does not have "entered" (it lasts 260 ms).
    setTimeout(function () {
        root.classList.add('entered');
    }, 400);

    // ---------- 2. phone menu ----------
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
