// Search theo username hoặc email
window.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra đăng nhập
    checkUserLogin();
    
    var searchInput = document.getElementById('searchInput');
    if (!searchInput) return;
    searchInput.addEventListener('keyup', function() {
        var filter = this.value.toLowerCase();
        var rows = document.querySelectorAll('#usersBody tr');
        rows.forEach(function(row) {
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

function checkUserLogin() {
    const user = localStorage.getItem('user');
    const userInfo = document.getElementById('userInfo');
    const loginLink = document.getElementById('loginLink');
    const welcomeSection = document.getElementById('welcomeSection');
    
    if (user) {
        const userData = JSON.parse(user);
        document.getElementById('userName').textContent = `Xin chào, ${userData.username}`;
        userInfo.style.display = 'flex';
        loginLink.style.display = 'none';
        welcomeSection.style.display = 'block';
    } else {
        userInfo.style.display = 'none';
        loginLink.style.display = 'block';
        welcomeSection.style.display = 'none';
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
}
