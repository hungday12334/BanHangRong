CREATE DATABASE IF NOT EXISTS wap;
USE wap;

-- Vô hiệu hóa kiểm tra khóa ngoại để có thể xóa các bảng theo thứ tự bất kỳ, sau đó bật lại
SET FOREIGN_KEY_CHECKS = 0;

-- Xóa các bảng nếu chúng tồn tại (thứ tự không quan trọng vì đã tắt kiểm tra khóa ngoại)
DROP TABLE IF EXISTS voucher_redemptions;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS withdrawal_requests;
DROP TABLE IF EXISTS bank_accounts;
DROP TABLE IF EXISTS product_licenses;
DROP TABLE IF EXISTS product_reviews;
DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS shopping_cart;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS categories_products;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS user_social_auth;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS users;

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;


-- Bảng users (Người dùng)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    password VARCHAR(512) NOT NULL,
    user_type VARCHAR(20),
    avatar_url VARCHAR(255),
    phone_number VARCHAR(20),
    gender VARCHAR(10),
    birth_date DATE,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ux_users_username UNIQUE (username),
    CONSTRAINT ux_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng user_social_auth (Xác thực mạng xã hội)
CREATE TABLE IF NOT EXISTS user_social_auth (
    auth_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_social_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_user_social_provider UNIQUE (provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng password_reset_tokens (Token đặt lại mật khẩu)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng email_verification_tokens (Token xác thực email)
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng categories (Danh mục)
CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ux_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng products (Sản phẩm)
CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    sale_price DECIMAL(15, 2),
    quantity INT DEFAULT 0,
    download_url VARCHAR(255) NOT NULL,
    total_sales INT DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(15) ,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng categories_products (Bảng nối Danh mục - Sản phẩm)
CREATE TABLE IF NOT EXISTS categories_products (
    category_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id, product_id),
    CONSTRAINT fk_catprod_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CONSTRAINT fk_catprod_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng product_images (Hình ảnh sản phẩm)
CREATE TABLE IF NOT EXISTS product_images (
    image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng shopping_cart (Giỏ hàng)
CREATE TABLE IF NOT EXISTS shopping_cart (
    cart_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT ux_cart_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng orders (Đơn hàng)
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng order_items (Chi tiết đơn hàng)
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_at_time DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng product_reviews (Đánh giá sản phẩm)
CREATE TABLE IF NOT EXISTS product_reviews (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_reviews_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng product_licenses (Giấy phép sản phẩm)
CREATE TABLE IF NOT EXISTS product_licenses (
    license_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_item_id BIGINT,
    user_id BIGINT,
    license_key VARCHAR(255) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    activation_date DATETIME,
    last_used_date DATETIME,
    device_identifier VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_licenses_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
    CONSTRAINT fk_licenses_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng user_sessions (Phiên đăng nhập)
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_identifier VARCHAR(255) NOT NULL,
    last_activity DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng payment_transactions (Giao dịch thanh toán)
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_provider VARCHAR(20) NOT NULL,
    provider_transaction_id VARCHAR(255),
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng vouchers (Mã giảm giá)
CREATE TABLE IF NOT EXISTS vouchers (
    voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    discount_type VARCHAR(16) NOT NULL, -- PERCENT | AMOUNT
    discount_value DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_order DECIMAL(12,2) DEFAULT NULL,
    start_at DATETIME DEFAULT NULL,
    end_at DATETIME DEFAULT NULL,
    max_uses INT DEFAULT NULL,
    max_uses_per_user INT DEFAULT NULL,
    used_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ux_vouchers_seller_product_code UNIQUE (seller_id, product_id, code),
    INDEX ix_vouchers_product (product_id, status),
    CONSTRAINT fk_vouchers_seller FOREIGN KEY (seller_id) REFERENCES users(user_id),
    CONSTRAINT fk_vouchers_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng voucher_redemptions (Lượt sử dụng mã giảm giá)
CREATE TABLE IF NOT EXISTS voucher_redemptions (
    redeem_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    order_id BIGINT DEFAULT NULL,
    user_id BIGINT DEFAULT NULL,
    discount_amount DECIMAL(12,2) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_voucher_redemptions_voucher (voucher_id, created_at),
    CONSTRAINT fk_redemptions_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    CONSTRAINT fk_redemptions_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_redemptions_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng bank_accounts (Tài khoản ngân hàng)
CREATE TABLE IF NOT EXISTS bank_accounts (
    bank_account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20),
    account_number VARCHAR(64) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    branch VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_user_account UNIQUE (user_id, account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng withdrawal_requests (Yêu cầu rút tiền)
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    withdrawal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_account_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    fee_percent DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    fee_amount DECIMAL(15,2) NOT NULL,
    net_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    note VARCHAR(255),
    payout_provider VARCHAR(50),
    provider_reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    CONSTRAINT fk_withdraw_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_withdraw_bank FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(bank_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
