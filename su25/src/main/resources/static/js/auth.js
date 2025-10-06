// Auth JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Toggle password visibility
    const togglePassword = document.getElementById('togglePassword');
    const toggleRegPassword = document.getElementById('toggleRegPassword');
    const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');
    
    if (togglePassword) {
        togglePassword.addEventListener('click', function() {
            togglePasswordVisibility('password', this);
        });
    }
    
    if (toggleRegPassword) {
        toggleRegPassword.addEventListener('click', function() {
            togglePasswordVisibility('regPassword', this);
        });
    }
    
    if (toggleConfirmPassword) {
        toggleConfirmPassword.addEventListener('click', function() {
            togglePasswordVisibility('confirmPassword', this);
        });
    }
    
    // Login form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    // Register form
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
    
    // Forgot password link
    const forgotPasswordLink = document.getElementById('forgotPassword');
    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener('click', handleForgotPassword);
    }
    
    // Forgot password form
    const forgotPasswordForm = document.getElementById('forgotPasswordForm');
    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener('submit', handleForgotPasswordForm);
    }
});

function togglePasswordVisibility(inputId, icon) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    
    const form = e.target;
    const formData = new FormData(form);
    
    // Get CAPTCHA response
    const captchaResponse = grecaptcha.getResponse();
    if (!captchaResponse) {
        showMessage('Vui lòng xác thực CAPTCHA!', 'error');
        return;
    }
    
    const loginData = {
        username: formData.get('username'),
        password: formData.get('password'),
        captchaResponse: captchaResponse
    };
    
    try {
        showLoading(form, true);
        clearMessages();
        
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData)
        });
        
        let result;
        try {
            result = await response.json();
        } catch (e) {
            console.error('JSON parse error:', e);
            showMessage('Có lỗi xảy ra khi xử lý phản hồi từ server!', 'error');
            return;
        }
        
        if (response.ok) {
            // Lưu token vào localStorage
            localStorage.setItem('token', result.token);
            localStorage.setItem('user', JSON.stringify({
                userId: result.userId,
                username: result.username,
                email: result.email,
                userType: result.userType
            }));
            
            showMessage('Đăng nhập thành công!', 'success');
            
            // Chuyển hướng sau 1 giây
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
        } else {
            console.error('Login error:', result);
            const errorMessage = result?.error || result || 'Đăng nhập thất bại!';
            showMessage(errorMessage, 'error');
            // Reset CAPTCHA on error
            grecaptcha.reset();
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('Có lỗi xảy ra khi đăng nhập!', 'error');
        // Reset CAPTCHA on error
        grecaptcha.reset();
    } finally {
        showLoading(form, false);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    
    const form = e.target;
    const formData = new FormData(form);
    
    // Kiểm tra mật khẩu xác nhận
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    if (password !== confirmPassword) {
        showMessage('Mật khẩu xác nhận không khớp!', 'error');
        return;
    }
    
    // Kiểm tra Terms of Service
    const termsAccepted = formData.get('termsAccepted');
    if (!termsAccepted) {
        showMessage('Vui lòng đồng ý với Terms of Service và Privacy Policy!', 'error');
        return;
    }
    
    // Get CAPTCHA response
    const captchaResponse = grecaptcha.getResponse();
    if (!captchaResponse) {
        showMessage('Vui lòng xác thực CAPTCHA!', 'error');
        return;
    }
    
    const registerData = {
        username: formData.get('username'),
        email: formData.get('email'),
        password: password,
        confirmPassword: confirmPassword,
        phoneNumber: formData.get('phoneNumber'),
        gender: formData.get('gender') || 'OTHER',
        birthDate: formData.get('birthDate') || null,
        termsAccepted: termsAccepted === 'on',
        captchaResponse: captchaResponse
    };
    
    try {
        showLoading(form, true);
        clearMessages();
        
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(registerData)
        });
        
        let result;
        try {
            result = await response.json();
        } catch (e) {
            console.error('JSON parse error:', e);
            showMessage('Có lỗi xảy ra khi xử lý phản hồi từ server!', 'error');
            return;
        }
        
        console.log('Register response:', response.status, result);
        
        if (response.ok) {
            showMessage('Đăng ký thành công! Vui lòng đăng nhập.', 'success');
            
            // Chuyển hướng về trang đăng nhập sau 2 giây
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else {
            console.error('Register error:', result);
            const errorMessage = result?.error || result || 'Đăng ký thất bại!';
            showMessage(errorMessage, 'error');
            // Reset CAPTCHA on error
            grecaptcha.reset();
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('Có lỗi xảy ra khi đăng ký!', 'error');
        // Reset CAPTCHA on error
        grecaptcha.reset();
    } finally {
        showLoading(form, false);
    }
}

async function handleForgotPassword(e) {
    e.preventDefault();
    
    const email = prompt('Nhập email của bạn:');
    if (!email) return;
    
    try {
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `email=${encodeURIComponent(email)}`
        });
        
        const result = await response.text();
        
        if (response.ok) {
            alert('Email đặt lại mật khẩu đã được gửi!');
        } else {
            alert(result || 'Có lỗi xảy ra!');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Có lỗi xảy ra!');
    }
}

async function handleForgotPasswordForm(e) {
    e.preventDefault();
    
    const form = e.target;
    const formData = new FormData(form);
    const email = formData.get('email');
    
    try {
        showLoading(form, true);
        clearMessages();
        
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `email=${encodeURIComponent(email)}`
        });
        
        const result = await response.text();
        
        if (response.ok) {
            showMessage('Email đặt lại mật khẩu đã được gửi!', 'success');
        } else {
            showMessage(result || 'Có lỗi xảy ra!', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('Có lỗi xảy ra!', 'error');
    } finally {
        showLoading(form, false);
    }
}

function showLoading(form, isLoading) {
    if (isLoading) {
        form.classList.add('loading');
    } else {
        form.classList.remove('loading');
    }
}

function showMessage(message, type) {
    clearMessages();
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `${type}-message`;
    messageDiv.textContent = message;
    
    const form = document.querySelector('.form');
    form.parentNode.insertBefore(messageDiv, form);
}

function clearMessages() {
    const existingMessages = document.querySelectorAll('.error-message, .success-message');
    existingMessages.forEach(msg => msg.remove());
}

// Kiểm tra đăng nhập khi load trang
function checkAuth() {
    const token = localStorage.getItem('token');
    if (token && window.location.pathname === '/login') {
        window.location.href = '/';
    }
}

// Gọi checkAuth khi load trang
checkAuth();
