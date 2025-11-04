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
    console.log('handleRegister called');
    e.preventDefault();
    
    const form = e.target;
    const formData = new FormData(form);
    
    console.log('Form data collected');

    // Validate username
    const username = formData.get('username');
    console.log('Username:', username);
    if (!isValidUsername(username)) {
        showFieldError('regUsername', 'Username must be 3-20 characters, alphanumeric and underscore only');
        return;
    }

    // Validate full name
    const fullName = formData.get('fullName');
    if (!fullName || fullName.trim().length < 2 || fullName.trim().length > 100) {
        showFieldError('fullName', 'Full name must be between 2 and 100 characters');
        return;
    }

    // Validate email
    const email = formData.get('email');
    if (!isValidEmail(email)) {
        showFieldError('regEmail', 'Invalid email format');
        return;
    }

    // Check password confirmation
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    // Validate password strength
    if (!isValidPassword(password)) {
        showFieldError('regPassword', 'Password must be at least 8 characters and contain both letters and numbers');
        return;
    }

    if (password !== confirmPassword) {
        showFieldError('confirmPassword', 'Passwords do not match!');
        return;
    }

    // Validate gender
    const gender = formData.get('gender');
    if (!gender || gender === '') {
        showFieldError('gender', 'Please select gender');
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
        showFieldError('phoneNumber', 'Invalid phone number. Must be 10 digits starting with 03, 05, 07, 08, or 09');
        return;
    }
    
    // Validate birth date
    const birthDate = formData.get('birthDate');
    if (birthDate && !isValidBirthDate(birthDate)) {
        showFieldError('birthDate', 'Invalid birth date. You must be at least 13 years old');
        return;
    }
    
    // Get CAPTCHA response (temporarily disabled for testing)
    let captchaResponse = 'test-captcha-token'; // Default for testing
    try {
        if (typeof grecaptcha !== 'undefined') {
            captchaResponse = grecaptcha.getResponse();
            if (!captchaResponse) {
                showMessage('Please verify CAPTCHA!', 'error');
                return;
            }
        } else {
            console.warn('reCAPTCHA not loaded, using test token');
        }
    } catch (e) {
        console.warn('CAPTCHA check failed:', e);
    }

    console.log('Form validation passed, preparing to submit...');

    const registerData = {
        username: username,
        fullName: fullName.trim(),
        email: email,
        password: password,
        confirmPassword: confirmPassword,
        phoneNumber: phoneNumber,
        gender: gender,
        birthDate: birthDate || null,
        termsAccepted: termsAccepted === 'on',
        captchaResponse: captchaResponse
    };
    
    console.log('Register data:', registerData);

    try {
        showLoading(form, true);
        clearMessages();
        
        console.log('Sending registration request...');

        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(registerData)
        });
        
        console.log('Response received:', response.status, response.statusText);

        const result = await parseResponse(response);
        if (!result) {
            console.error('Failed to parse response');
            showLoading(form, false);
            return;
        }

        console.log('Register response:', response.status, result);
        
        if (response.ok) {
            console.log('Registration successful!');

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
            console.log('Showing error message:', errorMessage);
            showMessage(errorMessage, 'error');

            // Reset CAPTCHA on error (if loaded)
            try {
                if (typeof grecaptcha !== 'undefined') {
                    grecaptcha.reset();
                }
            } catch (e) {
                console.warn('Could not reset CAPTCHA:', e);
            }
        }
    } catch (error) {
        console.error('Error during registration:', error);
        showMessage('An error occurred during registration!', 'error');

        // Reset CAPTCHA on error (if loaded)
        try {
            if (typeof grecaptcha !== 'undefined') {
                grecaptcha.reset();
            }
        } catch (e) {
            console.warn('Could not reset CAPTCHA:', e);
        }
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
    console.log('showMessage called:', type, message);
    clearMessages();
    
    if (type === 'error') {
        const errorDiv = document.getElementById('errorMessage');
        const errorText = document.getElementById('errorText');
        console.log('Error div found:', errorDiv !== null, 'Error text found:', errorText !== null);
        if (errorDiv && errorText) {
            errorText.textContent = message;
            errorDiv.style.display = 'flex';
            errorDiv.classList.add('show');
            console.log('Error message displayed');
        } else {
            console.error('Could not find error message elements!');
        }
    } else if (type === 'success') {
        const successDiv = document.getElementById('successMessage');
        console.log('Success div found:', successDiv !== null);
        if (successDiv) {
            const messageSpan = successDiv.querySelector('span');
            if (messageSpan) {
                messageSpan.textContent = message;
            }
            successDiv.style.display = 'flex';
            successDiv.classList.add('show');
            console.log('Success message displayed');
        } else {
            console.error('Could not find success message element!');
        }
    }
}

function clearMessages() {
    const errorDiv = document.getElementById('errorMessage');
    const successDiv = document.getElementById('successMessage');
    const verificationDiv = document.getElementById('emailVerificationMessage');

    if (errorDiv) {
        errorDiv.style.display = 'none';
        errorDiv.classList.remove('show');
    }
    if (successDiv) {
        successDiv.style.display = 'none';
        successDiv.classList.remove('show');
    }
    if (verificationDiv) {
        verificationDiv.style.display = 'none';
        verificationDiv.classList.remove('show');
    }
}

function showEmailVerificationMessage() {
    const emailMessage = document.getElementById('emailVerificationMessage');
    if (emailMessage) {
        emailMessage.style.display = 'flex';
        emailMessage.classList.add('show');
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

// ✅ Username validation
function isValidUsername(username) {
    if (!username || username.trim() === '') {
        return false;
    }
    // Username: 3-20 characters, alphanumeric and underscore only
    const regex = /^[a-zA-Z0-9_]{3,20}$/;
    return regex.test(username);
}

// ✅ Email validation
function isValidEmail(email) {
    if (!email || email.trim() === '') {
        return false;
    }
    const regex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return regex.test(email);
}

// ✅ Password strength validation
function isValidPassword(password) {
    if (!password || password.length < 8) {
        return false;
    }
    // At least 8 characters, must contain at least one letter and one number
    return /[A-Za-z]/.test(password) && /\d/.test(password);
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

// ✅ Real-time username validation
function validateUsernameInput() {
    const usernameInput = document.getElementById('regUsername');
    if (usernameInput) {
        usernameInput.addEventListener('input', function() {
            const username = this.value.trim();
            // Remove existing error
            const existingError = this.parentNode.querySelector('.field-error');
            if (existingError) {
                existingError.remove();
                this.style.borderColor = '';
            }

            if (username === '') {
                this.style.borderColor = '';
            } else if (isValidUsername(username)) {
                this.style.borderColor = '#28a745';
            } else {
                this.style.borderColor = '#ffc107';
            }
        });
    }
}

// ✅ Real-time email validation
function validateEmailInput() {
    const emailInput = document.getElementById('regEmail');
    if (emailInput) {
        emailInput.addEventListener('input', function() {
            const email = this.value.trim();
            // Remove existing error
            const existingError = this.parentNode.querySelector('.field-error');
            if (existingError) {
                existingError.remove();
                this.style.borderColor = '';
            }

            if (email === '') {
                this.style.borderColor = '';
            } else if (isValidEmail(email)) {
                this.style.borderColor = '#28a745';
            } else {
                this.style.borderColor = '#ffc107';
            }
        });
    }
}

// ✅ Real-time password validation
function validatePasswordInput() {
    const passwordInput = document.getElementById('regPassword');
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const password = this.value;
            // Remove existing error
            const existingError = this.parentNode.querySelector('.field-error');
            if (existingError) {
                existingError.remove();
                this.style.borderColor = '';
            }

            if (password === '') {
                this.style.borderColor = '';
            } else if (isValidPassword(password)) {
                this.style.borderColor = '#28a745';
            } else {
                this.style.borderColor = '#ffc107';
            }
        });
    }
}

// ✅ Real-time full name validation
function validateFullNameInput() {
    const fullNameInput = document.getElementById('fullName');
    if (fullNameInput) {
        fullNameInput.addEventListener('input', function() {
            const fullName = this.value.trim();
            // Remove existing error
            const existingError = this.parentNode.querySelector('.field-error');
            if (existingError) {
                existingError.remove();
                this.style.borderColor = '';
            }

            if (fullName === '') {
                this.style.borderColor = '';
            } else if (fullName.length >= 2 && fullName.length <= 100) {
                this.style.borderColor = '#28a745';
            } else {
                this.style.borderColor = '#ffc107';
            }
        });
    }
}

// Gọi checkAuth khi load trang
checkAuth();
validatePhoneInput();
validateBirthDateInput();
validateUsernameInput();
validateEmailInput();
validatePasswordInput();
validateFullNameInput();
