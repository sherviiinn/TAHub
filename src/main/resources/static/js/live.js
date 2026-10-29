// Live updates without reloading the page (Server-Sent Events from /notifications/stream):
//  1. "unread":  the number in "Notifications (n)" in the menu.
//  2. "changed": a topic changed (like "offering:5"). If this page shows that topic
//     (<main data-live-topics="offering:5">), its <main> is quietly reloaded. Whatever the user
//     was typing or choosing in a form, the cursor position and the scroll position are kept.
//  3. "changed" with "banner": the yellow top bar is reloaded.
// On pages without the menu link (login, register) this does nothing.
(function () {
    var link = document.getElementById('nav-notifications');
    if (!link || !window.EventSource) {
        return;
    }
    var source = new EventSource(link.dataset.stream);

    // ---------- 1. unread count ----------
    source.addEventListener('unread', function (event) {
        link.textContent = link.dataset.label.replace('{0}', event.data);
        link.classList.toggle('has-unread', event.data !== '0');
    });

    // ---------- 2 and 3. changed topics ----------
    var waiting = {};   // topic -> timer, so many changes in a row cause one reload
    source.addEventListener('changed', function (event) {
        var topic = event.data;
        if (topic === 'banner') {
            later('banner', reloadBanner);
        } else if (pageTopics().indexOf(topic) !== -1) {
            later('main', reloadMain);
        }
    });

    function later(key, action) {
        clearTimeout(waiting[key]);
        waiting[key] = setTimeout(action, 300);
    }

    function pageTopics() {
        var main = document.querySelector('main[data-live-topics]');
        return main ? main.dataset.liveTopics.split(/\s+/) : [];
    }

    function currentPath() {
        return location.pathname + location.search;
    }

    function reloadBanner() {
        var slot = document.getElementById('site-banner-slot');
        if (!slot) {
            return;
        }
        fetch(slot.dataset.url + '?back=' + encodeURIComponent(currentPath()), {credentials: 'same-origin'})
            .then(function (response) { return response.ok ? response.text() : null; })
            .then(function (html) {
                if (html !== null) {
                    slot.outerHTML = html;
                }
            })
            .catch(function () { /* the next change will try again */ });
    }

    var busy = false;
    var again = false;

    function reloadMain() {
        if (busy) {           // a reload is running: do one more right after it
            again = true;
            return;
        }
        busy = true;
        fetch(currentPath(), {credentials: 'same-origin'})
            .then(function (response) {
                if (!response.ok || response.redirected) {
                    // No longer allowed here, or logged out: show what the server says.
                    location.reload();
                    return null;
                }
                return response.text();
            })
            .then(function (html) {
                if (html === null) {
                    return;
                }
                var fresh = new DOMParser().parseFromString(html, 'text/html').querySelector('main');
                var old = document.querySelector('main');
                if (!fresh || !old) {
                    location.reload();
                    return;
                }
                swap(old, fresh);
            })
            .catch(function () { /* the next change will try again */ })
            .finally(function () {
                busy = false;
                if (again) {
                    again = false;
                    reloadMain();
                }
            });
    }

    // ---------- keeping what the user typed ----------

    // A field is found again by its form's action, its name, (for checkboxes) its value, and its order.
    function keyOf(field, counts) {
        var form = field.form ? field.form.getAttribute('action') : '';
        var base = form + '|' + (field.name || field.id) + '|' +
            ((field.type === 'checkbox' || field.type === 'radio') ? field.value : '');
        counts[base] = (counts[base] || 0) + 1;
        return base + '|' + counts[base];
    }

    function fields(root) {
        return Array.prototype.filter.call(root.querySelectorAll('input, textarea, select'), function (f) {
            return f.type !== 'hidden' && f.type !== 'submit' && f.type !== 'button' && (f.name || f.id);
        });
    }

    // True if the user changed this field compared to what the server sent.
    function changedByUser(field) {
        if (field.type === 'checkbox' || field.type === 'radio') {
            return field.checked !== field.defaultChecked;
        }
        if (field.tagName === 'SELECT') {
            return Array.prototype.some.call(field.options, function (o) { return o.selected !== o.defaultSelected; });
        }
        return field.value !== field.defaultValue;
    }

    function swap(old, fresh) {
        // 1. Remember the user's work.
        var counts = {};
        var typed = [];
        var focusKey = null;
        var selection = null;
        fields(old).forEach(function (field) {
            var key = keyOf(field, counts);
            if (changedByUser(field)) {
                typed.push({key: key, field: field});
            }
            if (field === document.activeElement) {
                focusKey = key;
                try {
                    selection = [field.selectionStart, field.selectionEnd];
                } catch (e) { /* not a text field */ }
            }
        });
        var scroll = window.scrollY;

        // 2. Put the new content in.
        old.replaceWith(fresh);

        // 3. Give the user's work back.
        var freshCounts = {};
        var byKey = {};
        fields(fresh).forEach(function (field) { byKey[keyOf(field, freshCounts)] = field; });

        var lost = false;
        typed.forEach(function (item) {
            var target = byKey[item.key];
            if (!target) {
                lost = true;
                return;
            }
            if (target.type === 'checkbox' || target.type === 'radio') {
                target.checked = item.field.checked;
            } else if (target.tagName === 'SELECT') {
                Array.prototype.forEach.call(target.options, function (o, i) {
                    o.selected = item.field.options[i] ? item.field.options[i].selected : false;
                });
            } else {
                target.value = item.field.value;
            }
        });
        if (focusKey && byKey[focusKey]) {
            var focused = byKey[focusKey];
            focused.focus({preventScroll: true});
            if (selection) {
                try {
                    focused.setSelectionRange(selection[0], selection[1]);
                } catch (e) { /* not a text field */ }
            }
        }
        window.scrollTo(0, scroll);

        // Only if a form the user was filling in is gone (for example a permission was taken away).
        if (lost && link.dataset.formGone) {
            var note = document.createElement('p');
            note.className = 'live-note';
            note.textContent = link.dataset.formGone;
            fresh.insertBefore(note, fresh.firstChild);
        }
    }
})();
