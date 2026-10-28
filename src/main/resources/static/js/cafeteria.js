// Keeps the reservation bars on the cafeteria page up to date without reloading the page.
// The server already drew everything correctly; this only moves it forward with time.
(function () {
    var labels = document.getElementById('reservation-labels');
    if (!labels) {
        return;
    }
    // Use the server clock: the difference between the server time and this computer's time.
    var offset = Number(labels.dataset.serverNow) - Date.now();

    function shortDuration(ms) {
        var minutes = Math.max(0, Math.floor(ms / 60000));
        var days = Math.floor(minutes / (24 * 60));
        var hours = Math.floor(minutes / 60) % 24;
        var mins = minutes % 60;
        return (days > 0 ? days + 'd ' : '') + hours + 'h ' + mins + 'm';
    }

    function update() {
        var now = Date.now() + offset;
        document.querySelectorAll('.reservation').forEach(function (box) {
            var opens = Number(box.dataset.opens);
            var closes = Number(box.dataset.closes);
            var fill = box.querySelector('.reservation-fill');
            var text = box.querySelector('.reservation-text');
            var color;
            var percent;

            if (now < opens) {
                color = 'bar-grey';
                percent = 100;
                text.textContent = labels.dataset.opensIn.replace('{0}', shortDuration(opens - now));
            } else if (now < closes) {
                var left = (closes - now) / (closes - opens);
                color = left > 2 / 3 ? 'bar-green' : (left > 1 / 3 ? 'bar-yellow' : 'bar-red');
                percent = Math.round(left * 100);
                text.textContent = labels.dataset.closesIn.replace('{0}', shortDuration(closes - now));
            } else {
                color = 'bar-grey';
                percent = 0;
                text.textContent = labels.dataset.closed;
            }
            fill.classList.remove('bar-grey', 'bar-green', 'bar-yellow', 'bar-red');
            fill.classList.add(color);
            fill.style.width = percent + '%';
        });
    }

    update();
    setInterval(update, 30000); // every 30 seconds is enough for minutes
})();
