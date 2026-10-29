// Keeps "Notifications (n)" in the menu up to date without reloading the page.
// The server pushes the new count (Server-Sent Events); the browser reconnects by itself if the
// connection drops. On pages without the menu link (login, register) this does nothing.
(function () {
    var link = document.getElementById('nav-notifications');
    if (!link || !window.EventSource) {
        return;
    }
    var label = link.dataset.label;            // like "Notifications ({0})"
    var source = new EventSource(link.dataset.stream);

    source.addEventListener('unread', function (event) {
        link.textContent = label.replace('{0}', event.data);
        link.classList.toggle('has-unread', event.data !== '0');
    });
})();
