CREATE DATABASE IF NOT EXISTS wap;
USE wap;
-- Tắt FK để drop tự do
SET FOREIGN_KEY_CHECKS=0;

-- Users
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
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

-- Social Authentication
DROP TABLE IF EXISTS user_social_auth;
CREATE TABLE IF NOT EXISTS user_social_auth (
    auth_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_social_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_user_social_provider UNIQUE (provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Password Reset Tokens
DROP TABLE IF EXISTS password_reset_tokens;
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Email Verification Tokens
DROP TABLE IF EXISTS email_verification_tokens;
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categories
DROP TABLE IF EXISTS categories;
CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ux_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Products (no category_id here; many-to-many via categories_products)
DROP TABLE IF EXISTS products;
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
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categories <-> Products (many-to-many)
DROP TABLE IF EXISTS categories_products;
CREATE TABLE IF NOT EXISTS categories_products (
    category_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id, product_id),
    CONSTRAINT fk_catprod_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CONSTRAINT fk_catprod_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Product Images
DROP TABLE IF EXISTS product_images;
CREATE TABLE IF NOT EXISTS product_images (
    image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Shopping Cart
DROP TABLE IF EXISTS shopping_cart;
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

-- Orders (payment fields removed; see payment_transactions)
DROP TABLE IF EXISTS orders;
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order Items (license_key removed)
DROP TABLE IF EXISTS order_items;
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

-- Product Reviews
DROP TABLE IF EXISTS product_reviews;
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

-- Product Licenses (license_key centralized here)
DROP TABLE IF EXISTS product_licenses;
CREATE TABLE IF NOT EXISTS product_licenses (
    license_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
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

-- User Sessions (for device tracking)
DROP TABLE IF EXISTS user_sessions;
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

-- Payment Transactions (holds payment provider/status/transaction)
DROP TABLE IF EXISTS payment_transactions;
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


-- Thêm vào file database migration của bạn
-- Chat Rooms
CREATE TABLE IF NOT EXISTS chat_rooms (
                                          room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          seller_id BIGINT NOT NULL,
                                          customer_id BIGINT NOT NULL,
                                          product_id BIGINT,
                                          is_active BOOLEAN DEFAULT TRUE,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_chat_rooms_seller FOREIGN KEY (seller_id) REFERENCES users(user_id),
                                          CONSTRAINT fk_chat_rooms_customer FOREIGN KEY (customer_id) REFERENCES users(user_id),
                                          CONSTRAINT fk_chat_rooms_product FOREIGN KEY (product_id) REFERENCES products(product_id),
                                          CONSTRAINT ux_chat_rooms_seller_customer UNIQUE (seller_id, customer_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat Messages
CREATE TABLE IF NOT EXISTS chat_messages (
                                             message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             room_id BIGINT NOT NULL,
                                             sender_id BIGINT NOT NULL,
                                             message_type VARCHAR(20) DEFAULT 'TEXT',
                                             content TEXT,
                                             file_url VARCHAR(255),
                                             is_read BOOLEAN DEFAULT FALSE,
                                             read_at TIMESTAMP NULL,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id),
                                             CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Quản lý voucher
CREATE TABLE IF NOT EXISTS vouchers (
                                        voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        seller_id BIGINT NOT NULL,
                                        code VARCHAR(50) NOT NULL,
                                        name VARCHAR(255) NOT NULL,
                                        description TEXT,
                                        discount_type ENUM('percentage', 'fixed') NOT NULL,
                                        discount_value DECIMAL(15, 2) NOT NULL,
                                        max_usage INT DEFAULT 1,
                                        used_count INT DEFAULT 0,
                                        min_order_value DECIMAL(15, 2) DEFAULT 0.00,
                                        valid_from TIMESTAMP NOT NULL,
                                        valid_to TIMESTAMP NOT NULL,
                                        is_active BOOLEAN DEFAULT TRUE,

    -- Các trường cho Voucher Thông Minh
                                        is_auto_generated BOOLEAN DEFAULT FALSE,
                                        auto_generation_type ENUM('abandoned_cart', 'loyal_customer', 'manual') DEFAULT 'manual',
                                        auto_generation_rule JSON,

                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_vouchers_seller FOREIGN KEY (seller_id) REFERENCES users(user_id),
                                        CONSTRAINT ux_vouchers_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Voucher đã gửi cho user

CREATE TABLE IF NOT EXISTS user_vouchers (
                                             user_voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id BIGINT NOT NULL,
                                             voucher_id BIGINT NOT NULL,
                                             is_used BOOLEAN DEFAULT FALSE,
                                             used_at TIMESTAMP NULL,
                                             sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Context for tracking
                                             sent_reason VARCHAR(100), -- 'abandoned_cart', 'loyal_customer', etc.
                                             context_data JSON, -- {cart_id: 123, abandoned_hours: 24}

                                             CONSTRAINT fk_user_vouchers_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                             CONSTRAINT fk_user_vouchers_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
                                             CONSTRAINT ux_user_voucher UNIQUE (user_id, voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Lịch sử sử dụng voucher
CREATE TABLE IF NOT EXISTS voucher_usage (
                                             usage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_voucher_id BIGINT NOT NULL,
                                             order_id BIGINT NOT NULL,
                                             discount_amount DECIMAL(15, 2) NOT NULL,
                                             used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_voucher_usage_user_voucher FOREIGN KEY (user_voucher_id) REFERENCES user_vouchers(user_voucher_id),
                                             CONSTRAINT fk_voucher_usage_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Thêm voucher_id vào bảng orders
ALTER TABLE orders
    ADD COLUMN voucher_id BIGINT NULL AFTER user_id,
    ADD CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id);

-- Thêm discount_amount vào bảng orders
ALTER TABLE orders
    ADD COLUMN discount_amount DECIMAL(15, 2) DEFAULT 0.00 AFTER total_amount;
-- Thêm cột seller_response vào bảng product_reviews
ALTER TABLE product_reviews
    ADD COLUMN seller_response TEXT NULL AFTER comment,
    ADD COLUMN seller_response_at TIMESTAMP NULL AFTER seller_response;
-- Indexes for performance
CREATE INDEX idx_chat_messages_room_created ON chat_messages(room_id, created_at);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_read ON chat_messages(is_read, room_id);
CREATE INDEX idx_chat_rooms_seller ON chat_rooms(seller_id);
CREATE INDEX idx_chat_rooms_customer ON chat_rooms(customer_id);
CREATE INDEX idx_chat_rooms_updated ON chat_rooms(updated_at DESC);

-- Insert sample chat data for testing
INSERT IGNORE INTO chat_rooms (seller_id, customer_id, product_id, created_at, updated_at)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'),
     (SELECT user_id FROM users WHERE username='alice'),
     (SELECT product_id FROM products WHERE name='Software Pro'),
     NOW(), NOW()),

    ((SELECT user_id FROM users WHERE username='seller'),
     (SELECT user_id FROM users WHERE username='bob'),
     (SELECT product_id FROM products WHERE name='E-Book X'),
     NOW(), NOW());

INSERT IGNORE INTO chat_messages (room_id, sender_id, content, message_type, created_at)
VALUES
    (1, (SELECT user_id FROM users WHERE username='alice'),
     'Xin chào, tôi có thắc mắc về sản phẩm Software Pro', 'TEXT',
     DATE_SUB(NOW(), INTERVAL 2 HOUR)),

    (1, (SELECT user_id FROM users WHERE username='seller'),
     'Chào bạn! Tôi có thể giúp gì cho bạn?', 'TEXT',
     DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    (2, (SELECT user_id FROM users WHERE username='bob'),
     'E-Book X có hỗ trợ đọc trên mobile không?', 'TEXT',
     DATE_SUB(NOW(), INTERVAL 30 MINUTE));
-- Bật lại FK
SET FOREIGN_KEY_CHECKS=1;

-- =============================
-- SAMPLE DATA (safe to re-run)
-- =============================

-- Users (3 users: seller, alice, bob)
INSERT IGNORE INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active, last_login)
VALUES
    ('seller', 'seller@example.com', '$2a$10$hashseller', 'SELLER', '0900000000', 'other', 1000.00, TRUE, TRUE, NOW()),
    ('alice', 'alice@example.com', '$2a$10$hashalice', 'CUSTOMER', '0911111111', 'female', 50.00, TRUE, TRUE, NOW()),
    ('bob', 'bob@example.com', '$2a$10$hashbob', 'CUSTOMER', '0922222222', 'male', 25.00, FALSE, TRUE, NOW());

-- Social auth for seller and alice
INSERT IGNORE INTO user_social_auth (user_id, provider, provider_user_id)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'google', 'google-uid-seller'),
    ((SELECT user_id FROM users WHERE username='alice'), 'facebook', 'fb-uid-alice');

-- Password reset tokens (unused)
INSERT IGNORE INTO password_reset_tokens (user_id, token, expires_at, is_used)
VALUES
    ((SELECT user_id FROM users WHERE username='bob'), 'reset-bob-001', DATE_ADD(NOW(), INTERVAL 1 DAY), FALSE);

-- Email verification tokens (unused)
INSERT IGNORE INTO email_verification_tokens (user_id, token, expires_at, is_used)
VALUES
    ((SELECT user_id FROM users WHERE username='bob'), 'verify-bob-001', DATE_ADD(NOW(), INTERVAL 1 DAY), FALSE);

-- Categories
INSERT IGNORE INTO categories (name, description)
VALUES
    ('E-Books', 'Sách điện tử nhiều chủ đề'),
    ('Music', 'Gói âm nhạc, loop, sample'),
    ('Software', 'Phần mềm, tiện ích, tool');

-- Products by seller
INSERT IGNORE INTO products (seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, is_active)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'E-Book X', 'Cuốn sách hướng dẫn nâng cao', 19.99, 14.99, 100, 'https://cdn.example.com/ebooks/x.pdf', 10, 4.5, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'Music Pack Vol.1', 'Bộ sample âm nhạc đa thể loại', 9.99, NULL, 200, 'https://cdn.example.com/music/v1.zip', 25, 4.2, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'Software Pro', 'Tiện ích chuyên nghiệp cho công việc', 49.00, 39.00, 50, 'https://cdn.example.com/software/pro.exe', 5, 4.8, TRUE);

-- Map Categories <-> Products
INSERT IGNORE INTO categories_products (category_id, product_id)
VALUES
    ((SELECT category_id FROM categories WHERE name='E-Books'), (SELECT product_id FROM products WHERE name='E-Book X')),
    ((SELECT category_id FROM categories WHERE name='Music'), (SELECT product_id FROM products WHERE name='Music Pack Vol.1')),
    ((SELECT category_id FROM categories WHERE name='Software'), (SELECT product_id FROM products WHERE name='Software Pro'));

-- Product Images
INSERT IGNORE INTO product_images (product_id, image_url, is_primary)
VALUES
    ((SELECT product_id FROM products WHERE name='E-Book X'), 'https://cdn.example.com/images/ebookx-cover.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='Music Pack Vol.1'), 'https://cdn.example.com/images/musicpack-v1.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='Software Pro'), 'https://cdn.example.com/images/software-pro.jpg', TRUE);

-- Shopping Cart (alice, bob)
INSERT IGNORE INTO shopping_cart (user_id, product_id, quantity)
VALUES
    ((SELECT user_id FROM users WHERE username='alice'), (SELECT product_id FROM products WHERE name='Software Pro'), 1),
    ((SELECT user_id FROM users WHERE username='bob'), (SELECT product_id FROM products WHERE name='E-Book X'), 1),
    ((SELECT user_id FROM users WHERE username='bob'), (SELECT product_id FROM products WHERE name='Music Pack Vol.1'), 2);

-- Orders (bob buys 3 items total)
INSERT IGNORE INTO orders (user_id, total_amount)
VALUES
    ((SELECT user_id FROM users WHERE username='bob'), 39.97);

-- Order Items for bob's latest order
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time)
VALUES
    ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1), (SELECT product_id FROM products WHERE name='E-Book X'), 1, 19.99),
    ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1), (SELECT product_id FROM products WHERE name='Music Pack Vol.1'), 2, 9.99);

-- Product Reviews (alice + bob)
INSERT IGNORE INTO product_reviews (product_id, user_id, rating, comment)
VALUES
    ((SELECT product_id FROM products WHERE name='E-Book X'), (SELECT user_id FROM users WHERE username='alice'), 5, 'Rất hữu ích!'),
    ((SELECT product_id FROM products WHERE name='Music Pack Vol.1'), (SELECT user_id FROM users WHERE username='bob'), 4, 'Âm thanh ổn, giá tốt');

-- Product Licenses (license for E-Book X order item)
INSERT IGNORE INTO product_licenses (order_item_id, user_id, license_key, is_active, activation_date)
VALUES
    (
        (SELECT oi.order_item_id
         FROM order_items oi
         JOIN orders o ON oi.order_id = o.order_id
         JOIN users u ON o.user_id = u.user_id
         JOIN products p ON oi.product_id = p.product_id
         WHERE u.username='bob' AND p.name='E-Book X'
         ORDER BY oi.order_item_id DESC LIMIT 1),
        (SELECT user_id FROM users WHERE username='bob'),
        'LIC-EBKX-0001', TRUE, NOW()
    );

-- User Sessions
INSERT IGNORE INTO user_sessions (user_id, device_identifier, last_activity, is_active)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'seller-device-01', NOW(), TRUE),
    ((SELECT user_id FROM users WHERE username='alice'), 'alice-phone-01', NOW(), TRUE),
    ((SELECT user_id FROM users WHERE username='bob'), 'bob-laptop-01', NOW(), TRUE);

-- Payment Transactions for bob's order
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data)
VALUES
    ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
     'VNPay', 'VNP-2025-0001', 39.97, 'PAID', JSON_OBJECT('method','card','card_last4','4242'));

-- =============================================
-- EXTRA RICH SAMPLE DATA FOR DASHBOARD DEMO
-- =============================================

-- More users
INSERT IGNORE INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active, last_login)
VALUES
    ('charlie', 'charlie@example.com', '$2a$10$hashcharlie', 'CUSTOMER', '0933333333', 'male', 35.00, TRUE, TRUE, NOW()),
    ('dave', 'dave@example.com', '$2a$10$hashdave', 'CUSTOMER', '0944444444', 'male', 75.00, TRUE, TRUE, NOW());

-- More products (by 'seller')
INSERT IGNORE INTO products (seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, is_active)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'Antivirus Pro Plus', 'Bảo vệ máy tính toàn diện', 59.00, 44.90, 80, 'https://cdn.example.com/software/antivirus-pro.exe', 18, 4.6, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'VPN Secure Unlimited', 'VPN tốc độ cao không giới hạn', 11.99, NULL, 300, 'https://cdn.example.com/software/vpn-secure.exe', 40, 4.3, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'Password Manager X', 'Quản lý mật khẩu an toàn', 29.00, 19.00, 120, 'https://cdn.example.com/software/pwm-x.exe', 22, 4.7, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'Dev Toolkit Ultimate', 'Bộ công cụ cho developer', 79.00, 59.00, 40, 'https://cdn.example.com/software/dev-toolkit.exe', 8, 4.9, TRUE),
    ((SELECT user_id FROM users WHERE username='seller'), 'Software Lite', 'Bản rút gọn tiết kiệm', 19.00, NULL, 150, 'https://cdn.example.com/software/lite.exe', 12, 4.1, TRUE);

-- Map new products to Software category
INSERT IGNORE INTO categories_products (category_id, product_id)
SELECT c.category_id, p.product_id
FROM categories c, products p
WHERE c.name='Software' AND p.name IN ('Antivirus Pro Plus','VPN Secure Unlimited','Password Manager X','Dev Toolkit Ultimate','Software Lite');

-- Product images
INSERT IGNORE INTO product_images (product_id, image_url, is_primary)
VALUES
    ((SELECT product_id FROM products WHERE name='Antivirus Pro Plus'), 'https://cdn.example.com/images/antivirus-pro.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='VPN Secure Unlimited'), 'https://cdn.example.com/images/vpn-secure.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='Password Manager X'), 'https://cdn.example.com/images/pwm-x.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='Dev Toolkit Ultimate'), 'https://cdn.example.com/images/dev-toolkit.jpg', TRUE),
    ((SELECT product_id FROM products WHERE name='Software Lite'), 'https://cdn.example.com/images/software-lite.jpg', TRUE);

-- Helper: create orders over last 14 days for alice/bob/charlie with explicit created_at
-- Day offsets: 14 to 0
-- Day 14 - alice buys Antivirus Pro Plus x1 (44.90)
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='alice'), 44.90, DATE_SUB(NOW(), INTERVAL 14 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Antivirus Pro Plus'), 1, 44.90, DATE_SUB(NOW(), INTERVAL 14 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    'VNPay', 'VNP-2025-1001', 44.90, 'PAID', JSON_OBJECT('method','card','brand','VISA'), DATE_SUB(NOW(), INTERVAL 14 DAY));

-- Day 12 - bob buys VPN x2 (2*11.99=23.98) + Software Lite x1 (19.00) total 42.98
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='bob'), 42.98, DATE_SUB(NOW(), INTERVAL 12 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='VPN Secure Unlimited'), 2, 11.99, DATE_SUB(NOW(), INTERVAL 12 DAY)),
       ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Software Lite'), 1, 19.00, DATE_SUB(NOW(), INTERVAL 12 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
    'MoMo', 'MOMO-2025-2001', 42.98, 'PAID', JSON_OBJECT('method','wallet'), DATE_SUB(NOW(), INTERVAL 12 DAY));

-- Day 10 - charlie buys Password Manager X x1 (19.00)
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='charlie'), 19.00, DATE_SUB(NOW(), INTERVAL 10 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='charlie' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Password Manager X'), 1, 19.00, DATE_SUB(NOW(), INTERVAL 10 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='charlie' ORDER BY o.order_id DESC LIMIT 1),
    'ZaloPay', 'ZLP-2025-3001', 19.00, 'PAID', JSON_OBJECT('method','bank'), DATE_SUB(NOW(), INTERVAL 10 DAY));

-- Day 9 - alice buys Dev Toolkit Ultimate x1 (59.00)
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='alice'), 59.00, DATE_SUB(NOW(), INTERVAL 9 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Dev Toolkit Ultimate'), 1, 59.00, DATE_SUB(NOW(), INTERVAL 9 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    'VNPay', 'VNP-2025-1002', 59.00, 'PAID', JSON_OBJECT('method','card'), DATE_SUB(NOW(), INTERVAL 9 DAY));

-- Day 7 - dave buys Antivirus Pro Plus x1 (44.90) + VPN x1 (11.99) total 56.89
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='dave'), 56.89, DATE_SUB(NOW(), INTERVAL 7 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Antivirus Pro Plus'), 1, 44.90, DATE_SUB(NOW(), INTERVAL 7 DAY)),
       ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='VPN Secure Unlimited'), 1, 11.99, DATE_SUB(NOW(), INTERVAL 7 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    'MoMo', 'MOMO-2025-2002', 56.89, 'PAID', JSON_OBJECT('method','wallet'), DATE_SUB(NOW(), INTERVAL 7 DAY));

-- Day 5 - bob buys Software Lite x2 (38.00)
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='bob'), 38.00, DATE_SUB(NOW(), INTERVAL 5 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Software Lite'), 2, 19.00, DATE_SUB(NOW(), INTERVAL 5 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='bob' ORDER BY o.order_id DESC LIMIT 1),
    'VNPay', 'VNP-2025-1003', 38.00, 'PAID', JSON_OBJECT('method','card'), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- Day 3 - charlie buys VPN x3 (35.97) + PWM X x1 (19.00) total 54.97
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='charlie'), 54.97, DATE_SUB(NOW(), INTERVAL 3 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='charlie' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='VPN Secure Unlimited'), 3, 11.99, DATE_SUB(NOW(), INTERVAL 3 DAY)),
       ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='charlie' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Password Manager X'), 1, 19.00, DATE_SUB(NOW(), INTERVAL 3 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='charlie' ORDER BY o.order_id DESC LIMIT 1),
    'ZaloPay', 'ZLP-2025-3002', 54.97, 'PAID', JSON_OBJECT('method','bank'), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Day 1 - alice buys E-Book X x1 (14.99 sale) + Music Pack x1 (9.99) total 24.98
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='alice'), 24.98, DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='E-Book X'), 1, 14.99, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Music Pack Vol.1'), 1, 9.99, DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='alice' ORDER BY o.order_id DESC LIMIT 1),
    'MoMo', 'MOMO-2025-2003', 24.98, 'PAID', JSON_OBJECT('method','wallet'), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Today - dave buys Dev Toolkit Ultimate x1 (59.00) + PWM X x1 (19.00) total 78.00
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES ((SELECT user_id FROM users WHERE username='dave'), 78.00, NOW());
INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Dev Toolkit Ultimate'), 1, 59.00, NOW()),
       ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    (SELECT product_id FROM products WHERE name='Password Manager X'), 1, 19.00, NOW());
INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES ((SELECT o.order_id FROM orders o JOIN users u ON o.user_id=u.user_id WHERE u.username='dave' ORDER BY o.order_id DESC LIMIT 1),
    'VNPay', 'VNP-2025-1004', 78.00, 'PAID', JSON_OBJECT('method','card'), NOW());

-- Reviews for the new products
INSERT IGNORE INTO product_reviews (product_id, user_id, rating, comment, created_at)
VALUES
    ((SELECT product_id FROM products WHERE name='Antivirus Pro Plus'), (SELECT user_id FROM users WHERE username='alice'), 5, 'Quét nhanh, nhẹ máy!', DATE_SUB(NOW(), INTERVAL 13 DAY)),
    ((SELECT product_id FROM products WHERE name='VPN Secure Unlimited'), (SELECT user_id FROM users WHERE username='bob'), 4, 'Ổn định, giá rẻ', DATE_SUB(NOW(), INTERVAL 11 DAY)),
    ((SELECT product_id FROM products WHERE name='Password Manager X'), (SELECT user_id FROM users WHERE username='charlie'), 5, 'Tuyệt vời, auto-fill mượt', DATE_SUB(NOW(), INTERVAL 9 DAY)),
    ((SELECT product_id FROM products WHERE name='Dev Toolkit Ultimate'), (SELECT user_id FROM users WHERE username='alice'), 5, 'Đáng tiền cho dev', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    ((SELECT product_id FROM products WHERE name='Software Lite'), (SELECT user_id FROM users WHERE username='bob'), 3, 'Tính năng cơ bản đủ dùng', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- Licenses for software items (where applicable)
-- Antivirus Pro Plus
INSERT IGNORE INTO product_licenses (order_item_id, user_id, license_key, is_active, activation_date)
SELECT oi.order_item_id, u.user_id, CONCAT('LIC-AVP-', LPAD(oi.order_item_id, 6, '0')), TRUE, oi.created_at
FROM order_items oi
JOIN orders o ON oi.order_id=o.order_id
JOIN users u ON o.user_id=u.user_id
JOIN products p ON oi.product_id=p.product_id
WHERE p.name='Antivirus Pro Plus';

-- Dev Toolkit Ultimate
INSERT IGNORE INTO product_licenses (order_item_id, user_id, license_key, is_active, activation_date)
SELECT oi.order_item_id, u.user_id, CONCAT('LIC-DTK-', LPAD(oi.order_item_id, 6, '0')), TRUE, oi.created_at
FROM order_items oi
JOIN orders o ON oi.order_id=o.order_id
JOIN users u ON o.user_id=u.user_id
JOIN products p ON oi.product_id=p.product_id
WHERE p.name='Dev Toolkit Ultimate';

-- Password Manager X
INSERT IGNORE INTO product_licenses (order_item_id, user_id, license_key, is_active, activation_date)
SELECT oi.order_item_id, u.user_id, CONCAT('LIC-PWM-', LPAD(oi.order_item_id, 6, '0')), TRUE, oi.created_at
FROM order_items oi
JOIN orders o ON oi.order_id=o.order_id
JOIN users u ON o.user_id=u.user_id
JOIN products p ON oi.product_id=p.product_id
WHERE p.name='Password Manager X';


-- testing voucher
-- Insert sample smart vouchers
INSERT IGNORE INTO vouchers (seller_id, code, name, description, discount_type, discount_value, max_usage, valid_from, valid_to, is_auto_generated, auto_generation_type, auto_generation_rule)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'WELCOME10', 'Welcome Discount', 'Giảm giá chào mừng khách hàng mới', 'percentage', 10.00, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), TRUE, 'loyal_customer', '{"min_orders": 1, "min_total_spent": 0}'),

    ((SELECT user_id FROM users WHERE username='seller'), 'ABANDON15', 'Cart Recovery', 'Giảm giá cho khách hàng bỏ giỏ hàng', 'percentage', 15.00, 500, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), TRUE, 'abandoned_cart', '{"abandoned_hours": 24, "max_retries": 1}'),

    ((SELECT user_id FROM users WHERE username='seller'), 'LOYAL20', 'Loyal Customer', 'Ưu đãi khách hàng thân thiết', 'percentage', 20.00, 200, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), TRUE, 'loyal_customer', '{"min_orders": 3, "min_total_spent": 100}');

-- Sample user vouchers for testing
INSERT IGNORE INTO user_vouchers (user_id, voucher_id, sent_reason, context_data)
VALUES
    ((SELECT user_id FROM users WHERE username='alice'),
     (SELECT voucher_id FROM vouchers WHERE code = 'LOYAL20'),
     'loyal_customer',
     '{"total_orders": 4, "total_spent": 188.88}');


-- Indexes for voucher performance
CREATE INDEX idx_vouchers_seller_valid ON vouchers(seller_id, is_active, valid_from, valid_to);
CREATE INDEX idx_vouchers_auto_type ON vouchers(auto_generation_type, is_active);
CREATE INDEX idx_user_vouchers_user_used ON user_vouchers(user_id, is_used);
CREATE INDEX idx_user_vouchers_sent_reason ON user_vouchers(sent_reason, sent_at);
CREATE INDEX idx_shopping_cart_updated ON shopping_cart(updated_at);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);

-- Bảng lưu cấu hình các khu vực (sections) của shop
CREATE TABLE seller_shop_sections (
                                      section_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      seller_id BIGINT NOT NULL,
                                      section_type ENUM('FEATURED', 'BEST_SELLER', 'TOP_RATED', 'NEW_ARRIVALS', 'CUSTOM') NOT NULL,
                                      section_title VARCHAR(255),
                                      sort_order INT NOT NULL DEFAULT 0,
                                      is_active BOOLEAN DEFAULT TRUE,
                                      filter_category_id BIGINT NULL,
                                      filter_price_min DECIMAL(15,2) NULL,
                                      filter_price_max DECIMAL(15,2) NULL,
                                      max_items INT DEFAULT 10,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      FOREIGN KEY (seller_id) REFERENCES users(user_id),
                                      FOREIGN KEY (filter_category_id) REFERENCES categories(category_id)
);

-- Bảng lưu sản phẩm được chọn thủ công cho khu vực FEATURED
CREATE TABLE seller_featured_products (
                                          featured_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          section_id BIGINT NOT NULL,
                                          product_id BIGINT NOT NULL,
                                          sort_order INT NOT NULL DEFAULT 0,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          FOREIGN KEY (section_id) REFERENCES seller_shop_sections(section_id) ON DELETE CASCADE,
                                          FOREIGN KEY (product_id) REFERENCES products(product_id),
                                          UNIQUE KEY ux_section_product (section_id, product_id)
);