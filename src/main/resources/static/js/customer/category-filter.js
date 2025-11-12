/**
 * Category Filter Handler
 * Xử lý form filter để loại bỏ các giá trị rỗng trước khi submit
 */
(function() {
    'use strict';

    // Đợi DOM load xong
    document.addEventListener('DOMContentLoaded', function() {
        const filterForm = document.querySelector('.filters');
        
        if (!filterForm) {
            return; // Không tìm thấy form, thoát
        }

        // Xử lý submit form
        filterForm.addEventListener('submit', function(e) {
            e.preventDefault(); // Ngăn submit mặc định

            // Lấy tất cả input và select trong form
            const inputs = filterForm.querySelectorAll('input, select');
            const params = [];

            inputs.forEach(function(input) {
                const name = input.name;
                let value = input.value;

                // Bỏ qua nếu không có name
                if (!name) {
                    return;
                }

                // Xử lý giá trị rỗng
                if (value === null || value === undefined || value === '') {
                    // Bỏ qua giá trị rỗng
                    return;
                }

                // Trim string values để loại bỏ khoảng trắng
                if (typeof value === 'string') {
                    value = value.trim();
                    if (value === '') {
                        return; // Bỏ qua nếu sau khi trim vẫn rỗng
                    }
                }

                // Thêm vào params array
                params.push(encodeURIComponent(name) + '=' + encodeURIComponent(value));
            });

            // Lấy base URL từ form action
            const baseUrl = filterForm.getAttribute('action') || window.location.pathname;
            
            // Build URL với query string (chỉ thêm nếu có params)
            let finalUrl = baseUrl;
            if (params.length > 0) {
                finalUrl += '?' + params.join('&');
            }

            // Redirect đến URL đã được xử lý
            window.location.href = finalUrl;
        });

        // Xử lý nút Reset
        const resetLink = filterForm.querySelector('a[href*="/category/"]');
        if (resetLink) {
            resetLink.addEventListener('click', function(e) {
                e.preventDefault();
                // Reset form và redirect về URL gốc (không có params)
                const baseUrl = filterForm.getAttribute('action') || window.location.pathname;
                window.location.href = baseUrl;
            });
        }
    });
})();

