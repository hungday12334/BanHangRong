// Home page functionality
window.addEventListener('DOMContentLoaded', function () {
    // Check authentication status
    checkAuthStatus();
    
    // Search functionality
    var searchInput = document.getElementById('searchInput');
    if (searchInput) {
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
    }
});

function checkAuthStatus() {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
        try {
            const user = JSON.parse(userData);
            
            // Show user info and hide login link
            const userInfo = document.getElementById('userInfo');
            const loginLink = document.getElementById('loginLink');
            const welcomeSection = document.getElementById('welcomeSection');
            const databaseSection = document.getElementById('databaseSection');
            const heroSection = document.querySelector('.hero-section');
            
            if (userInfo) {
                userInfo.style.display = 'flex';
                document.getElementById('userName').textContent = user.username;
            }
            
            if (loginLink) {
                loginLink.style.display = 'none';
            }
            
            if (welcomeSection) {
                welcomeSection.style.display = 'block';
                welcomeSection.querySelector('h2').textContent = `Welcome back, ${user.username}!`;
            }
            
            // Show database section for admin users
            if (user.userType === 'ADMIN' && databaseSection) {
                databaseSection.style.display = 'block';
            }
            
            // Hide hero section for logged in users
            if (heroSection) {
                heroSection.style.display = 'none';
            }
            
        } catch (e) {
            console.error('Error parsing user data:', e);
            logout();
        }
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/';
}
