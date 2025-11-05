/**
 * Frontend Validation for Voucher Form
 * Provides instant feedback before submitting to backend
 *
 * Usage:
 * const errors = validateVoucherForm(formData);
 * if (Object.keys(errors).length === 0) {
 *     // Submit form
 * } else {
 *     // Display errors
 * }
 */

/**
 * Main validation function
 * @param {Object} formData - Form data to validate
 * @param {BigDecimal} productPrice - Product price for validation
 * @returns {Object} errors - Object containing field errors
 */
function validateVoucherForm(formData, productPrice) {
    const errors = {};

    // ============================================================
    // A. DATE VALIDATION
    // ============================================================

    // A.1: Start date not in past
    if (formData.startAt) {
        const now = new Date();
        const startDate = new Date(formData.startAt);

        // Allow 5 minute tolerance
        const fiveMinutesAgo = new Date(now.getTime() - 5 * 60000);

        if (startDate < fiveMinutesAgo) {
            errors.startAt = "Ngày bắt đầu không được là ngày trong quá khứ";
        }
    } else {
        errors.startAt = "Ngày bắt đầu không được để trống";
    }

    // A.2: End date after start date
    if (formData.startAt && formData.endAt) {
        const startDate = new Date(formData.startAt);
        const endDate = new Date(formData.endAt);

        if (endDate <= startDate) {
            errors.endAt = "Ngày kết thúc phải sau ngày bắt đầu";
        }
    } else if (!formData.endAt) {
        errors.endAt = "Ngày kết thúc không được để trống";
    }

    // A.3: Duration not exceed 1 year
    if (formData.startAt && formData.endAt) {
        const startDate = new Date(formData.startAt);
        const endDate = new Date(formData.endAt);

        // Calculate 1 year from start date
        const oneYearLater = new Date(startDate);
        oneYearLater.setFullYear(startDate.getFullYear() + 1);

        if (endDate > oneYearLater) {
            errors.endAt = "Thời gian voucher không được vượt quá 1 năm";
        }
    }

    // ============================================================
    // B. DISCOUNT VALIDATION
    // ============================================================

    // Validate discount type
    if (!formData.discountType) {
        errors.discountType = "Loại giảm giá không được để trống";
    } else if (formData.discountType !== 'PERCENT' && formData.discountType !== 'AMOUNT') {
        errors.discountType = "Loại giảm giá phải là PERCENT hoặc AMOUNT";
    }

    // Validate discount value
    if (formData.discountValue === null || formData.discountValue === undefined || formData.discountValue === '') {
        errors.discountValue = "Giá trị giảm giá không được để trống";
    } else {
        const discountValue = parseFloat(formData.discountValue);

        if (discountValue <= 0) {
            errors.discountValue = "Giá trị giảm giá phải lớn hơn 0";
        }

        // B.4: If PERCENT, must be <= 100
        if (formData.discountType === 'PERCENT') {
            if (discountValue > 100) {
                errors.discountValue = "Giá trị giảm giá % phải từ 0.01 đến 100";
            }
        }

        // B.5: If AMOUNT, must be <= product price
        if (formData.discountType === 'AMOUNT' && productPrice) {
            if (discountValue > productPrice) {
                errors.discountValue = `Giá trị giảm giá không được vượt quá giá sản phẩm (${formatCurrency(productPrice)})`;
            }
        }

        // B.6: Discount should not exceed minOrder
        if (formData.minOrder !== null && formData.minOrder !== undefined && formData.minOrder !== '') {
            const minOrder = parseFloat(formData.minOrder);
            if (discountValue > minOrder) {
                errors.discountValue = "Giá trị giảm giá không được vượt quá giá trị đơn hàng tối thiểu";
            }
        }
    }

    // ============================================================
    // C. MINIMUM ORDER VALUE
    // ============================================================

    if (formData.minOrder !== null && formData.minOrder !== undefined && formData.minOrder !== '') {
        const minOrder = parseFloat(formData.minOrder);

        // C.7: MinOrder >= 0
        if (minOrder < 0) {
            errors.minOrder = "Giá trị đơn hàng tối thiểu phải >= 0";
        }

        // C.8: MinOrder <= 10,000,000
        if (minOrder > 10000000) {
            errors.minOrder = "Giá trị đơn hàng tối thiểu không được vượt quá 10,000,000 VNĐ";
        }

        // C.9: For AMOUNT type, minOrder must be >= discountValue
        if (formData.discountType === 'AMOUNT' && formData.discountValue) {
            const discountValue = parseFloat(formData.discountValue);
            if (minOrder < discountValue) {
                errors.minOrder = "Giá trị đơn hàng tối thiểu phải >= giá trị giảm giá (với loại AMOUNT)";
            }
        }
    }

    // ============================================================
    // D. USAGE LIMITS
    // ============================================================

    // D.10: maxUses >= 0
    if (formData.maxUses !== null && formData.maxUses !== undefined && formData.maxUses !== '') {
        const maxUses = parseInt(formData.maxUses);
        if (maxUses < 0) {
            errors.maxUses = "Số lần sử dụng tối đa phải >= 0 (0 = không giới hạn)";
        }
    }

    // D.10: limitPerUser >= 0
    if (formData.maxUsesPerUser !== null && formData.maxUsesPerUser !== undefined && formData.maxUsesPerUser !== '') {
        const limitPerUser = parseInt(formData.maxUsesPerUser);
        if (limitPerUser < 0) {
            errors.maxUsesPerUser = "Số lần sử dụng tối đa/người phải >= 0 (0 = không giới hạn)";
        }

        // D.11: limitPerUser <= maxUses (if maxUses > 0)
        if (formData.maxUses && formData.maxUses > 0) {
            const maxUses = parseInt(formData.maxUses);
            if (limitPerUser > maxUses) {
                errors.maxUsesPerUser = "Số lần sử dụng/người không được vượt quá tổng số lần sử dụng";
            }
        }
    }

    // ============================================================
    // E. REQUIRED FIELDS
    // ============================================================

    // Voucher code
    if (!formData.code || formData.code.trim() === '') {
        errors.code = "Mã voucher không được để trống";
    } else {
        const code = formData.code.trim().toUpperCase();

        // Check length
        if (code.length < 3 || code.length > 20) {
            errors.code = "Mã voucher phải từ 3-20 ký tự";
        }

        // Check format (only A-Z and 0-9)
        if (!/^[A-Z0-9]+$/.test(code)) {
            errors.code = "Mã voucher chỉ được chứa chữ IN HOA và số";
        }
    }

    // Status
    if (!formData.status || formData.status.trim() === '') {
        errors.status = "Trạng thái không được để trống";
    } else if (formData.status !== 'active' && formData.status !== 'inactive') {
        errors.status = "Trạng thái phải là 'active' hoặc 'inactive'";
    }

    return errors;
}

/**
 * Display validation errors on form
 * @param {Object} errors - Validation errors
 */
function displayValidationErrors(errors) {
    // Clear all previous errors
    document.querySelectorAll('.error-message').forEach(el => el.remove());
    document.querySelectorAll('.form-control.is-invalid').forEach(el => {
        el.classList.remove('is-invalid');
    });

    // Display new errors
    Object.keys(errors).forEach(fieldName => {
        const input = document.getElementById(fieldName);
        if (input) {
            input.classList.add('is-invalid');

            const errorDiv = document.createElement('div');
            errorDiv.className = 'error-message text-danger small mt-1';
            errorDiv.textContent = errors[fieldName];

            input.parentNode.appendChild(errorDiv);
        }
    });

    // Scroll to first error
    const firstError = document.querySelector('.is-invalid');
    if (firstError) {
        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
        firstError.focus();
    }
}

/**
 * Real-time validation for specific field
 * @param {string} fieldName - Field name to validate
 * @param {Object} formData - Current form data
 * @param {BigDecimal} productPrice - Product price
 */
function validateField(fieldName, formData, productPrice) {
    const errors = validateVoucherForm(formData, productPrice);

    // Clear error for this field
    const input = document.getElementById(fieldName);
    if (input) {
        input.classList.remove('is-invalid');
        const existingError = input.parentNode.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }

        // Display error if exists
        if (errors[fieldName]) {
            input.classList.add('is-invalid');

            const errorDiv = document.createElement('div');
            errorDiv.className = 'error-message text-danger small mt-1';
            errorDiv.textContent = errors[fieldName];

            input.parentNode.appendChild(errorDiv);
        }
    }
}

/**
 * Helper function to format currency
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

/**
 * Example usage in form submission
 */
function handleVoucherFormSubmit(event) {
    event.preventDefault();

    // Get form data
    const formData = {
        code: document.getElementById('voucherCode').value,
        discountType: document.getElementById('discountType').value,
        discountValue: document.getElementById('discountValue').value,
        minOrder: document.getElementById('minOrder').value,
        startAt: document.getElementById('startAt').value,
        endAt: document.getElementById('endAt').value,
        maxUses: document.getElementById('maxUses').value,
        maxUsesPerUser: document.getElementById('maxUsesPerUser').value,
        status: document.getElementById('status').value
    };

    // Get product price (from hidden field or data attribute)
    const productPrice = parseFloat(document.getElementById('selectedProductPrice').textContent.replace(/[^0-9]/g, ''));

    // Validate
    const errors = validateVoucherForm(formData, productPrice);

    if (Object.keys(errors).length === 0) {
        // No errors - submit form
        console.log('Form is valid, submitting...');
        submitVoucherForm(formData);
    } else {
        // Display errors
        console.log('Validation errors:', errors);
        displayValidationErrors(errors);

        // Show alert
        showAlert('Vui lòng kiểm tra lại các trường đã nhập', 'error');
    }
}

/**
 * Setup real-time validation on form inputs
 */
function setupRealtimeValidation() {
    const fields = [
        'voucherCode', 'discountType', 'discountValue', 'minOrder',
        'startAt', 'endAt', 'maxUses', 'maxUsesPerUser', 'status'
    ];

    fields.forEach(fieldName => {
        const input = document.getElementById(fieldName);
        if (input) {
            // Validate on blur (when user leaves field)
            input.addEventListener('blur', function() {
                validateSingleField(fieldName);
            });

            // Also validate on input for immediate feedback
            input.addEventListener('input', function() {
                // Clear error while typing
                clearFieldError(fieldName);

                // Debounce validation
                clearTimeout(input.validationTimeout);
                input.validationTimeout = setTimeout(() => {
                    validateSingleField(fieldName);
                }, 500); // Wait 500ms after user stops typing
            });

            // Special handling for discountType change
            if (fieldName === 'discountType') {
                input.addEventListener('change', function() {
                    // Re-validate discountValue and minOrder when type changes
                    validateSingleField('discountValue');
                    validateSingleField('minOrder');
                });
            }
        }
    });

    console.log('✅ Real-time validation enabled');
}

/**
 * Validate a single field and show error immediately
 */
function validateSingleField(fieldName) {
    const formData = getFormData();
    const productPrice = getProductPrice();
    const errors = validateVoucherForm(formData, productPrice);

    const input = document.getElementById(fieldName);
    if (!input) return;

    // Clear previous error/success
    input.classList.remove('is-invalid', 'is-valid');
    const existingMsg = input.parentNode.querySelector('.error-message, .success-message');
    if (existingMsg) {
        existingMsg.remove();
    }

    // Show error if exists
    if (errors[fieldName]) {
        input.classList.add('is-invalid');

        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = `<i class="ti ti-alert-circle"></i> ${errors[fieldName]}`;

        input.parentNode.appendChild(errorDiv);
    } else if (input.value.trim() !== '') {
        // Show success if field is valid and not empty
        input.classList.add('is-valid');

        const successDiv = document.createElement('div');
        successDiv.className = 'success-message';
        successDiv.innerHTML = `<i class="ti ti-check"></i> Hợp lệ`;

        input.parentNode.appendChild(successDiv);
    }
}

/**
 * Clear error for a specific field
 */
function clearFieldError(fieldName) {
    const input = document.getElementById(fieldName);
    if (input) {
        input.classList.remove('is-invalid');
        const existingError = input.parentNode.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }
    }
}

/**
 * Clear all validation states
 */
function clearAllValidation() {
    const fields = [
        'voucherCode', 'discountType', 'discountValue', 'minOrder',
        'startAt', 'endAt', 'maxUses', 'maxUsesPerUser', 'status'
    ];

    fields.forEach(fieldName => {
        const input = document.getElementById(fieldName);
        if (input) {
            input.classList.remove('is-invalid', 'is-valid');
            const msg = input.parentNode.querySelector('.error-message, .success-message');
            if (msg) msg.remove();
        }
    });
}

/**
 * Helper to get current form data
 */
function getFormData() {
    return {
        code: document.getElementById('voucherCode')?.value || '',
        discountType: document.getElementById('discountType')?.value || '',
        discountValue: document.getElementById('discountValue')?.value || '',
        minOrder: document.getElementById('minOrder')?.value || '',
        startAt: document.getElementById('startAt')?.value || '',
        endAt: document.getElementById('endAt')?.value || '',
        maxUses: document.getElementById('maxUses')?.value || '',
        maxUsesPerUser: document.getElementById('maxUsesPerUser')?.value || '',
        status: document.getElementById('status')?.value || 'active'
    };
}

/**
 * Helper to get product price
 */
function getProductPrice() {
    const priceText = document.getElementById('selectedProductPrice')?.textContent || '0';
    return parseFloat(priceText.replace(/[^0-9]/g, ''));
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Voucher validation script loaded');

    // Wait a bit for page to fully render
    setTimeout(() => {
        setupRealtimeValidation();
    }, 500);
});

