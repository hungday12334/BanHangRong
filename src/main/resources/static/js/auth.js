// Auth JavaScript

// Helper function to parse response and handle errors
async function parseResponse(response) {
    try {
        return await response.json();
    } catch (e) {
        console.error('JSON parse error:', e);
        showMessage('An error occurred while processing server response!', 'error');
        return null;
    }
}

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
        
        const result = await parseResponse(response);
        if (!result) return;
        
        if (response.ok) {
            // Lưu token vào localStorage
            localStorage.setItem('token', result.token);
            localStorage.setItem('user', JSON.stringify({
                userId: result.userId,
                username: result.username,
                email: result.email,
                userType: result.userType
            }));
            
            showMessage('Login successful!', 'success');
            
            // Redirect to menu after 1 second
            setTimeout(() => {
                window.location.href = '/menu';
            }, 1000);
        } else {
            console.error('Login error:', result);
            const errorMessage = result?.error || result || 'Login failed!';
            showMessage(errorMessage, 'error');
            // Reset CAPTCHA on error
            grecaptcha.reset();
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('An error occurred during login!', 'error');
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
    
    // Check password confirmation
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    if (password !== confirmPassword) {
        showMessage('Passwords do not match!', 'error');
        return;
    }
    
    // Check Terms of Service
    const termsAccepted = formData.get('termsAccepted');
    if (!termsAccepted) {
        showMessage('Please agree to Terms of Service and Privacy Policy!', 'error');
        return;
    }
    
    // Validate phone number
    const phoneNumber = formData.get('phoneNumber');
    if (!isValidPhone(phoneNumber)) {
        showFieldError('phoneNumber', 'Invalid phone number');
        return;
    }
    
    // Validate birth date
    const birthDate = formData.get('birthDate');
    if (birthDate && !isValidBirthDate(birthDate)) {
        showFieldError('birthDate', 'Invalid birth date');
        return;
    }
    
    // Get CAPTCHA response
    const captchaResponse = grecaptcha.getResponse();
    if (!captchaResponse) {
        showMessage('Please verify CAPTCHA!', 'error');
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
        
        const result = await parseResponse(response);
        if (!result) return;
        
        console.log('Register response:', response.status, result);
        
        if (response.ok) {
            // Show email verification message below email field
            showEmailVerificationMessage();
            
            // Show general message
            showMessage('Registration successful! Verification email has been sent to your email address.', 'success');
            
            // Redirect to login page after 5 seconds
            setTimeout(() => {
                window.location.href = '/login';
            }, 5000);
        } else {
            console.error('Register error:', result);
            const errorMessage = result?.error || result || 'Registration failed!';
            showMessage(errorMessage, 'error');
            // Reset CAPTCHA on error
            grecaptcha.reset();
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('An error occurred during registration!', 'error');
        // Reset CAPTCHA on error
        grecaptcha.reset();
    } finally {
        showLoading(form, false);
    }
}

async function handleForgotPassword(e) {
    e.preventDefault();
    
    // Redirect to forgot password page instead of using prompt
    window.location.href = '/forgot-password';
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
            showMessage('Password reset email has been sent!', 'success');
        } else {
            showMessage(result || 'An error occurred!', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showMessage('An error occurred!', 'error');
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

function showEmailVerificationMessage() {
    const emailMessage = document.getElementById('emailVerificationMessage');
    if (emailMessage) {
        emailMessage.style.display = 'flex';
        emailMessage.style.animation = 'slideDown 0.3s ease-out';
    }
}

// Check authentication on page load
function checkAuth() {
    const token = localStorage.getItem('token');
    if (token && window.location.pathname === '/login') {
        window.location.href = '/menu';
    }
}

// ✅ Show field error - ngắn gọn dưới input
function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    if (!field) return;
    
    // Remove existing error
    const existingError = field.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
    
    // Add new error message
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    errorDiv.style.color = '#dc3545';
    errorDiv.style.fontSize = '13px';
    errorDiv.style.marginTop = '5px';
    errorDiv.style.fontWeight = '500';
    errorDiv.style.display = 'block';
    errorDiv.style.animation = 'fadeIn 0.3s ease-in';
    
    field.parentNode.appendChild(errorDiv);
    
    // Focus on the field
    field.focus();
    field.style.borderColor = '#dc3545';
    
    // Remove error on input
    field.addEventListener('input', function() {
        const error = this.parentNode.querySelector('.field-error');
        if (error) {
            error.remove();
        }
        this.style.borderColor = '';
    }, { once: true });
}

// ✅ Phone number validation for Vietnamese numbers
function isValidPhone(phone) {
    const regex = /^(03|05|07|08|09)\d{8}$/;
    return phone != null && phone.trim() !== '' && regex.test(phone);
}

// ✅ Birth date validation
function isValidBirthDate(birthDate) {
    if (!birthDate || birthDate.trim() === '') {
        return true; // Birth date is optional
    }
    
    try {
        const date = new Date(birthDate);
        const today = new Date();
        const minDate = new Date(today.getFullYear() - 100, today.getMonth(), today.getDate()); // Maximum 100 years old
        const maxDate = new Date(today.getFullYear() - 13, today.getMonth(), today.getDate()); // Minimum 13 years old
        
        return date <= today && date >= minDate && date <= maxDate;
    } catch (e) {
        return false;
    }
}

// ✅ Real-time phone validation
function validatePhoneInput() {
    const phoneInput = document.getElementById('phoneNumber');
    if (phoneInput) {
        phoneInput.addEventListener('input', function() {
            const phone = this.value.trim();
            const small = this.nextElementSibling;
            
                    if (phone === '') {
                        small.style.color = '#999';
                        small.textContent = '10 digits, start with: 03, 05, 07, 08, 09';
                    } else if (isValidPhone(phone)) {
                        small.style.color = '#28a745';
                        small.textContent = '✓ Valid';
                    } else {
                        small.style.color = '#dc3545';
                        small.textContent = '✗ Invalid';
                    }
        });
    }
}

// ✅ Real-time birth date validation
function validateBirthDateInput() {
    const birthDateInput = document.getElementById('birthDate');
    if (birthDateInput) {
        birthDateInput.addEventListener('change', function() {
            const birthDate = this.value;
            const small = this.nextElementSibling;
            
            if (!small) {
                // Create small element if it doesn't exist
                const smallElement = document.createElement('small');
                smallElement.style.color = '#999';
                smallElement.style.fontSize = '12px';
                smallElement.style.marginTop = '5px';
                smallElement.style.display = 'block';
                this.parentNode.appendChild(smallElement);
            }
            
            const smallElement = this.nextElementSibling;
            
                    if (!birthDate) {
                        smallElement.style.color = '#999';
                        smallElement.textContent = 'Optional - Minimum age: 13';
                    } else if (isValidBirthDate(birthDate)) {
                        smallElement.style.color = '#28a745';
                        smallElement.textContent = '✓ Valid';
                    } else {
                        smallElement.style.color = '#dc3545';
                        smallElement.textContent = '✗ Invalid (minimum age 13)';
                    }
        });
    }
}

// Gọi checkAuth khi load trang
checkAuth();
validatePhoneInput();
validateBirthDateInput();
