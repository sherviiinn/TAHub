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

    // ---------- bubble drop-down lists: <select data-pretty-select> ----------
    // The browser draws the open list of a normal <select> itself, so CSS cannot shape it. Here the <select>
    // stays in the form (hidden, it is still what the form sends) and a button with a bubble list is drawn
    // next to it. On touch screens (phones) the phone's own picker is kept, because it is easier to use there.
    var touch = window.matchMedia('(pointer: coarse)').matches;
    var openSelect = null;   // the one that is open now (only one at a time)
    var pickId = 0;

    var caretSvg = '<svg class="icon pselect-caret" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 9.5l6 6 6-6"/></svg>';
    var checkSvg = '<svg class="icon pselect-check" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>';

    function enhanceSelect(select) {
        if (select.dataset.prettyReady || touch) {
            return;
        }
        select.dataset.prettyReady = 'true';
        pickId++;

        var wrap = document.createElement('div');
        wrap.className = 'pselect';
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'pselect-button';
        button.id = 'pselect-' + pickId;
        button.setAttribute('aria-haspopup', 'listbox');
        button.setAttribute('aria-expanded', 'false');
        var list = document.createElement('ul');
        list.className = 'pselect-list';
        list.id = 'pselect-list-' + pickId;
        list.setAttribute('role', 'listbox');
        list.tabIndex = -1;
        button.setAttribute('aria-controls', list.id);

        // The label of the <select> now names the button, and a click on it opens nothing but focuses the button.
        var label = select.id ? document.querySelector('label[for="' + select.id + '"]') : null;
        if (label) {
            if (!label.id) {
                label.id = button.id + '-label';
            }
            button.setAttribute('aria-labelledby', label.id + ' ' + button.id);
            list.setAttribute('aria-labelledby', label.id);
            label.addEventListener('click', function (event) {
                event.preventDefault();
                button.focus();
            });
        }

        select.classList.add('pselect-native');
        select.tabIndex = -1;
        select.setAttribute('aria-hidden', 'true');
        select.parentNode.insertBefore(wrap, select.nextSibling);
        wrap.appendChild(button);
        wrap.appendChild(list);

        // One line in the list per real option (the empty "Choose ..." option is only the placeholder).
        var placeholder = '';
        var items = [];
        Array.prototype.forEach.call(select.options, function (option, index) {
            if (option.value === '') {
                if (!placeholder) {
                    placeholder = option.text;
                }
                return;
            }
            var item = document.createElement('li');
            item.className = 'pselect-option';
            item.id = list.id + '-' + index;
            item.setAttribute('role', 'option');
            item.dataset.index = String(index);
            item.innerHTML = '<span class="pselect-text"></span>' + checkSvg;
            item.querySelector('.pselect-text').textContent = option.text;
            if (option.disabled) {
                item.setAttribute('aria-disabled', 'true');
            }
            list.appendChild(item);
            items.push(item);
        });

        var active = -1;   // position in "items" that the keyboard is on

        function sync() {
            var chosen = select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
            var empty = !chosen || chosen.value === '';
            button.innerHTML = '<span class="pselect-value"></span>' + caretSvg;
            button.querySelector('.pselect-value').textContent = empty ? placeholder : chosen.text;
            button.classList.toggle('is-placeholder', empty);
            items.forEach(function (item) {
                item.setAttribute('aria-selected', String(Number(item.dataset.index) === select.selectedIndex));
            });
            wrap.classList.toggle('is-invalid', select.classList.contains('is-invalid'));
        }

        function setActive(position) {
            if (!items.length) {
                return;
            }
            active = Math.max(0, Math.min(items.length - 1, position));
            items.forEach(function (item, i) { item.classList.toggle('is-active', i === active); });
            list.setAttribute('aria-activedescendant', items[active].id);
            items[active].scrollIntoView({block: 'nearest'});
        }

        function open() {
            if (openSelect && openSelect !== close) {
                openSelect();
            }
            // Open upwards when there is not enough room below.
            var box = button.getBoundingClientRect();
            var below = window.innerHeight - box.bottom;
            wrap.classList.toggle('is-up', below < 260 && box.top > below);
            wrap.classList.add('is-open');
            button.setAttribute('aria-expanded', 'true');
            var current = items.findIndex(function (item) { return item.getAttribute('aria-selected') === 'true'; });
            setActive(current >= 0 ? current : 0);
            openSelect = close;
        }

        function close() {
            wrap.classList.remove('is-open');
            button.setAttribute('aria-expanded', 'false');
            list.removeAttribute('aria-activedescendant');
            if (openSelect === close) {
                openSelect = null;
            }
        }

        function choose(position) {
            var item = items[position];
            if (!item || item.getAttribute('aria-disabled') === 'true') {
                return;
            }
            select.selectedIndex = Number(item.dataset.index);
            // Tell the page the value changed, like a real choice in the <select> would.
            select.dispatchEvent(new Event('input', {bubbles: true}));
            select.dispatchEvent(new Event('change', {bubbles: true}));
            sync();
            close();
            button.focus({preventScroll: true});
        }

        button.addEventListener('click', function () {
            if (wrap.classList.contains('is-open')) {
                close();
            } else {
                open();
            }
        });

        button.addEventListener('keydown', function (event) {
            var isOpen = wrap.classList.contains('is-open');
            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                if (!isOpen) {
                    open();
                } else {
                    setActive(active + (event.key === 'ArrowDown' ? 1 : -1));
                }
            } else if (event.key === 'Home' && isOpen) {
                event.preventDefault();
                setActive(0);
            } else if (event.key === 'End' && isOpen) {
                event.preventDefault();
                setActive(items.length - 1);
            } else if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                if (isOpen) {
                    choose(active);
                } else {
                    open();
                }
            } else if (event.key === 'Escape' && isOpen) {
                event.preventDefault();
                event.stopPropagation();
                close();
            } else if (event.key === 'Tab' && isOpen) {
                close();
            } else if (event.key.length === 1 && /\S/.test(event.key)) {
                // Typing a letter jumps to the next option that starts with it.
                var letter = event.key.toLowerCase();
                for (var step = 1; step <= items.length; step++) {
                    var i = (active + step) % items.length;
                    if (items[i].textContent.trim().toLowerCase().indexOf(letter) === 0) {
                        if (!isOpen) {
                            open();
                        }
                        setActive(i);
                        break;
                    }
                }
            }
        });

        list.addEventListener('mousedown', function (event) {
            event.preventDefault();   // keep the focus on the button
        });
        list.addEventListener('click', function (event) {
            var item = event.target.closest('.pselect-option');
            if (item) {
                choose(items.indexOf(item));
            }
        });
        list.addEventListener('mousemove', function (event) {
            var item = event.target.closest('.pselect-option');
            if (item && items.indexOf(item) !== active) {
                setActive(items.indexOf(item));
            }
        });

        // The value can also change from outside (for example live.js giving back what the user chose).
        select.addEventListener('change', sync);

        sync();
        // The server may have put the cursor on this field (autofocus): give it to the button instead.
        if (select.hasAttribute('autofocus') || document.activeElement === select) {
            button.focus({preventScroll: true});
        }
    }

    function enhanceAll(root) {
        if (root.matches && root.matches('select[data-pretty-select]')) {
            enhanceSelect(root);
        }
        if (root.querySelectorAll) {
            root.querySelectorAll('select[data-pretty-select]').forEach(enhanceSelect);
        }
    }

    enhanceAll(document);
    // Pages that live.js reloads get new <select> boxes: dress them too.
    new MutationObserver(function (changes) {
        changes.forEach(function (change) {
            change.addedNodes.forEach(function (node) {
                if (node.nodeType === 1) {
                    enhanceAll(node);
                }
            });
        });
    }).observe(document.body, {childList: true, subtree: true});

    // A click anywhere else closes the open list.
    document.addEventListener('mousedown', function (event) {
        if (openSelect && !event.target.closest('.pselect')) {
            openSelect();
        }
    });

    // ---------- sign in page: the example cards follow the mouse a little ----------
    // Sets --px and --py (from -1 to 1) on the dark half; app.css moves each card by its own depth.
    var aside = document.querySelector('.auth-aside');
    var calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (aside && !calm) {
        var frame = 0;
        aside.addEventListener('mousemove', function (event) {
            var box = aside.getBoundingClientRect();
            var x = ((event.clientX - box.left) / box.width) * 2 - 1;
            var y = ((event.clientY - box.top) / box.height) * 2 - 1;
            cancelAnimationFrame(frame);
            frame = requestAnimationFrame(function () {
                aside.style.setProperty('--px', x.toFixed(3));
                aside.style.setProperty('--py', y.toFixed(3));
            });
        });
        aside.addEventListener('mouseleave', function () {
            aside.style.setProperty('--px', '0');
            aside.style.setProperty('--py', '0');
        });
    }

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
