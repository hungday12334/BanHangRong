// Search theo username hoặc email
window.addEventListener('DOMContentLoaded', function () {
    var searchInput = document.getElementById('searchInput');
    if (!searchInput) return;
    searchInput.addEventListener('keyup', function () {
        var filter = this.value.toLowerCase();
        var rows = document.querySelectorAll('#usersBody tr');
        rows.forEach(function (row) {
            var username = row.children[1].textContent.toLowerCase();
            var email = row.children[2].textContent.toLowerCase();
            if (username.includes(filter) || email.includes(filter)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    });
});
