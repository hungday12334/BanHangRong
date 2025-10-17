
CREATE DATABASE IF NOT EXISTS wap;
USE wap;

-- Users
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

-- Social Authentication
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
CREATE TABLE IF NOT EXISTS categories (
                                          category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          name VARCHAR(100) NOT NULL,
                                          description TEXT,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          CONSTRAINT ux_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Products (no category_id here; many-to-many via categories_products)
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

-- Categories <-> Products (many-to-many)
CREATE TABLE IF NOT EXISTS categories_products (
                                                   category_id BIGINT NOT NULL,
                                                   product_id BIGINT NOT NULL,
                                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   PRIMARY KEY (category_id, product_id),
                                                   CONSTRAINT fk_catprod_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
                                                   CONSTRAINT fk_catprod_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Product Images
CREATE TABLE IF NOT EXISTS product_images (
                                              image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              product_id BIGINT NOT NULL,
                                              image_url VARCHAR(255) NOT NULL,
                                              is_primary BOOLEAN DEFAULT FALSE,
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Shopping Cart
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
CREATE TABLE IF NOT EXISTS orders (
                                      order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      user_id BIGINT NOT NULL,
                                      total_amount DECIMAL(15, 2) NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order Items (license_key removed)
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
INSERT IGNORE INTO products (seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, status)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'E-Book X', 'Cuốn sách hướng dẫn nâng cao', 19.99, 14.99, 100, 'https://cdn.example.com/ebooks/x.pdf', 10, 4.5, 'Public'),
    ((SELECT user_id FROM users WHERE username='seller'), 'Music Pack Vol.1', 'Bộ sample âm nhạc đa thể loại', 9.99, NULL, 200, 'https://cdn.example.com/music/v1.zip', 25, 4.2, 'Pending'),
    ((SELECT user_id FROM users WHERE username='seller'), 'Software Pro', 'Tiện ích chuyên nghiệp cho công việc', 49.00, 39.00, 50, 'https://cdn.example.com/software/pro.exe', 5, 4.8, 'Hidden');

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
INSERT IGNORE INTO products (seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, status)
VALUES
    ((SELECT user_id FROM users WHERE username='seller'), 'Antivirus Pro Plus', 'Bảo vệ máy tính toàn diện', 59.00, 44.90, 80, 'https://cdn.example.com/software/antivirus-pro.exe', 18, 4.6, 'Hidden'),
    ((SELECT user_id FROM users WHERE username='seller'), 'VPN Secure Unlimited', 'VPN tốc độ cao không giới hạn', 11.99, NULL, 300, 'https://cdn.example.com/software/vpn-secure.exe', 40, 4.3, 'Public'),
    ((SELECT user_id FROM users WHERE username='seller'), 'Password Manager X', 'Quản lý mật khẩu an toàn', 29.00, 19.00, 120, 'https://cdn.example.com/software/pwm-x.exe', 22, 4.7, 'Pending'),
    ((SELECT user_id FROM users WHERE username='seller'), 'Dev Toolkit Ultimate', 'Bộ công cụ cho developer', 79.00, 59.00, 40, 'https://cdn.example.com/software/dev-toolkit.exe', 8, 4.9, 'Public'),
    ((SELECT user_id FROM users WHERE username='seller'), 'Software Lite', 'Bản rút gọn tiết kiệm', 19.00, NULL, 150, 'https://cdn.example.com/software/lite.exe', 12, 4.1, 'Pending');

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


-- =============================================
-- PRODUCT SAMPLES WITH ALL RELATED DATA
-- Run this after data_v2.sql to populate rich product catalog
-- This script will CLEAN old product data and import fresh samples
-- =============================================

USE wap;

-- =============================
-- DROP OLD PRODUCT DATA
-- =============================
-- WARNING: This will DELETE ALL product-related data!
-- Only run this if you want to completely reset product catalog
-- Drop in correct order due to foreign key constraints

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;
-- Intentionally delete all rows (no WHERE clause) to reset catalog
DELETE FROM product_licenses;
DELETE FROM product_reviews;
DELETE FROM payment_transactions;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM shopping_cart;
DELETE FROM product_images;
DELETE FROM categories_products;
DELETE FROM products;
DELETE FROM categories;
SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- Reset auto increment (optional, for clean IDs)
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;
ALTER TABLE product_images AUTO_INCREMENT = 1;
ALTER TABLE product_reviews AUTO_INCREMENT = 1;
ALTER TABLE payment_transactions AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE product_licenses AUTO_INCREMENT = 1;

-- =============================
-- CATEGORIES
-- =============================
INSERT IGNORE INTO categories (name, description)
VALUES
    ('Software', 'Phần mềm, công cụ, tiện ích'),
    ('Game Keys', 'Key game, Steam, Epic, Origin'),
    ('Licenses', 'License Windows, Office, Antivirus'),
    ('E-Books', 'Sách điện tử, tài liệu học tập'),
    ('Music & Audio', 'Âm nhạc, sound effects, loops'),
    ('Graphics', 'Template đồ họa, fonts, icons'),
    ('Vouchers', 'Voucher dịch vụ, mã giảm giá');

-- =============================
-- PRODUCTS (20 samples)
-- =============================
INSERT IGNORE INTO products (seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, status)
VALUES
    -- Software & Licenses
    (1, 'Microsoft Office 2021 Pro Plus', 'Bộ Office đầy đủ: Word, Excel, PowerPoint, Outlook. License vĩnh viễn cho 1 máy tính.', 1250000.00, 1100000.00, 150, 'https://download.example.com/office-2021-pro.iso', 520, 4.50, 'Public'),
    (1, 'Adobe Creative Cloud All Apps', 'Trọn bộ ứng dụng Adobe: Photoshop, Illustrator, Premiere Pro, After Effects. 12 tháng subscription.', 850000.00, NULL, 80, 'https://download.example.com/adobe-cc-all.zip', 450, 4.50, 'Public'),
    (1, 'Norton 360 Deluxe 2023', 'Bảo vệ toàn diện: Antivirus, VPN, Password Manager. 5 devices, 12 tháng.', 650000.00, 550000.00, 200, 'https://download.example.com/norton360-deluxe.exe', 410, 4.50, 'Public'),
    (1, 'Windows 11 Pro OEM', 'Windows 11 Professional OEM license key. Kích hoạt trực tuyến, hỗ trợ 24/7.', 900000.00, NULL, 300, 'https://download.example.com/win11-pro-iso.zip', 380, 4.50, 'Public'),
    (1, 'Malwarebytes Premium', 'Anti-malware chuyên nghiệp. 1 năm, 3 devices. Bảo vệ real-time.', 450000.00, 390000.00, 100, 'https://download.example.com/malwarebytes.exe', 350, 4.50, 'Public'),

    -- Game Keys
    (1, 'FIFA 23 - Steam Key', 'FIFA 23 Standard Edition Steam Global Key. Kích hoạt ngay lập tức.', 1100000.00, NULL, 50, 'https://download.example.com/fifa23-steam-key.txt', 280, 4.50, 'Public'),
    (1, 'Minecraft Java Edition', 'Minecraft Java Edition chính hãng. Lifetime access. Full version PC/Mac/Linux.', 650000.00, 580000.00, 120, 'https://download.example.com/minecraft-java.txt', 350, 4.50, 'Public'),
    (1, 'Grand Theft Auto V - Epic', 'GTA V Epic Games Store Key. Hỗ trợ online mode.', 480000.00, 450000.00, 75, 'https://download.example.com/gta5-epic-key.txt', 240, 4.50, 'Public'),
    (1, 'Cyberpunk 2077 GOG Key', 'Cyberpunk 2077 GOG Global Key. DRM-free. Kèm soundtrack.', 950000.00, NULL, 40, 'https://download.example.com/cyberpunk2077-gog.txt', 180, 4.50, 'Public'),
    (1, 'Valorant Points 2000 VP', 'Valorant 2000 VP gift card. Dùng tất cả server. Giao nhanh trong 5 phút.', 420000.00, 380000.00, 200, 'https://download.example.com/valorant-2000vp.txt', 420, 4.50, 'Public'),

    -- E-Books & Music
    (1, 'Web Development Bootcamp 2025', 'E-Book 850 trang học lập trình web từ cơ bản đến nâng cao. HTML, CSS, JS, React, Node.', 199000.00, 149000.00, 500, 'https://download.example.com/web-dev-bootcamp.pdf', 330, 4.50, 'Public'),
    (1, 'Royalty Free Music Pack 1000', 'Bộ 1000 bản nhạc không bản quyền cho video, stream. MP3 320kbps.', 550000.00, NULL, 90, 'https://download.example.com/music-pack-1000.zip', 220, 4.50, 'Public'),

    -- Graphics & Vouchers
    (1, 'Canva Pro 12 Months', 'Canva Pro subscription 12 tháng. Full features: templates, background remover, brand kit.', 380000.00, 320000.00, 150, 'https://download.example.com/canva-pro-12m.txt', 290, 4.50, 'Public'),
    (1, 'Spotify Premium 12 Months', '12 tháng Spotify Premium subscription. Ad-free, offline download, unlimited skips.', 750000.00, 680000.00, 100, 'https://download.example.com/spotify-12m.txt', 450, 4.50, 'Pending'),
    (1, 'Netflix Premium 1 Month', 'Netflix Premium 1 tháng. 4K UHD, 4 screens. Hỗ trợ tất cả thiết bị.', 260000.00, NULL, 300, 'https://download.example.com/netflix-1m.txt', 440, 4.50, 'Pending'),
    -- Extra vouchers / licenses / gaming
    (1, 'Discord Nitro 1 Year', 'Discord Nitro 12 tháng. Boost server, HD streaming.', 799000.00, 699000.00, 80, 'https://download.example.com/discord-nitro-1y.txt', 410, 4.50, 'Pending'),
    (1, 'Steam Gift Card 500K', 'Steam Wallet code 500,000 VND. Nạp trực tiếp.', 500000.00, NULL, 200, 'https://download.example.com/steam-gift-500k.txt', 500, 4.60, 'Pending'),
    (1, 'PlayStation Plus 12 Months', 'PS Plus Essential 12 tháng cho tài khoản Việt Nam.', 890000.00, 830000.00, 120, 'https://download.example.com/ps-plus-12m.txt', 360, 4.40, 'Pending'),
    (1, 'Office 365 Family 12 Months', 'Tối đa 6 người dùng, 1TB OneDrive mỗi người.', 1290000.00, 990000.00, 60, 'https://download.example.com/office365-family.txt', 250, 4.60, 'Cancelled'),
    (1, 'YouTube Premium 3 Months', 'YouTube Premium 3 tháng. Không quảng cáo, phát nền.', 150000.00, NULL, 400, 'https://download.example.com/youtube-premium-3m.txt', 470, 4.50, 'Hidden');
-- =============================
-- CATEGORIES <-> PRODUCTS
-- =============================
INSERT IGNORE INTO categories_products (category_id, product_id)
SELECT c.category_id, p.product_id
FROM categories c, products p
WHERE (c.name = 'Licenses' AND p.name IN ('Microsoft Office 2021 Pro Plus', 'Adobe Creative Cloud All Apps', 'Norton 360 Deluxe 2023', 'Windows 11 Pro OEM', 'Malwarebytes Premium', 'Office 365 Family 12 Months'))
   OR (c.name = 'Game Keys' AND p.name IN ('FIFA 23 - Steam Key', 'Minecraft Java Edition', 'Grand Theft Auto V - Epic', 'Cyberpunk 2077 GOG Key', 'Valorant Points 2000 VP', 'PlayStation Plus 12 Months'))
   OR (c.name = 'E-Books' AND p.name = 'Web Development Bootcamp 2025')
   OR (c.name = 'Music & Audio' AND p.name = 'Royalty Free Music Pack 1000')
   OR (c.name = 'Graphics' AND p.name = 'Canva Pro 12 Months')
   OR (c.name = 'Vouchers' AND p.name IN ('Spotify Premium 12 Months', 'Netflix Premium 1 Month', 'Discord Nitro 1 Year', 'Steam Gift Card 500K', 'YouTube Premium 3 Months'));

-- =============================
-- PRODUCT IMAGES
-- =============================
-- Using verified working image URLs (tested CDN sources)
INSERT IGNORE INTO product_images (product_id, image_url, is_primary)
VALUES
    -- Software & Licenses (using placehold.co for reliable, customizable placeholders)
    ((SELECT product_id FROM products WHERE name='Microsoft Office 2021 Pro Plus'), 'https://placehold.co/600x400/0078D4/white?text=Office+2021', TRUE),
    ((SELECT product_id FROM products WHERE name='Adobe Creative Cloud All Apps'), 'https://placehold.co/600x400/FF0000/white?text=Adobe+CC', TRUE),
    ((SELECT product_id FROM products WHERE name='Norton 360 Deluxe 2023'), 'https://placehold.co/600x400/FFC107/black?text=Norton+360', TRUE),
    ((SELECT product_id FROM products WHERE name='Windows 11 Pro OEM'), 'https://placehold.co/600x400/0078D4/white?text=Windows+11', TRUE),
    ((SELECT product_id FROM products WHERE name='Malwarebytes Premium'), 'https://placehold.co/600x400/FF6D00/white?text=Malwarebytes', TRUE),

    -- Game Keys (using placehold.co with game-themed colors)
    ((SELECT product_id FROM products WHERE name='FIFA 23 - Steam Key'), 'https://placehold.co/600x400/00A86B/white?text=FIFA+23', TRUE),
    ((SELECT product_id FROM products WHERE name='Minecraft Java Edition'), 'https://placehold.co/600x400/8B4513/white?text=Minecraft', TRUE),
    ((SELECT product_id FROM products WHERE name='Grand Theft Auto V - Epic'), 'https://placehold.co/600x400/000000/green?text=GTA+V', TRUE),
    ((SELECT product_id FROM products WHERE name='Cyberpunk 2077 GOG Key'), 'https://placehold.co/600x400/FCEE09/black?text=Cyberpunk+2077', TRUE),
    ((SELECT product_id FROM products WHERE name='Valorant Points 2000 VP'), 'https://placehold.co/600x400/FF4655/white?text=Valorant+VP', TRUE),

    -- E-Books & Music
    ((SELECT product_id FROM products WHERE name='Web Development Bootcamp 2025'), 'https://placehold.co/600x400/6200EA/white?text=Web+Dev+Bootcamp', TRUE),
    ((SELECT product_id FROM products WHERE name='Royalty Free Music Pack 1000'), 'https://placehold.co/600x400/1DB954/white?text=Music+Pack', TRUE),

    -- Graphics & Vouchers
    ((SELECT product_id FROM products WHERE name='Canva Pro 12 Months'), 'https://placehold.co/600x400/00C4CC/white?text=Canva+Pro', TRUE),
    ((SELECT product_id FROM products WHERE name='Spotify Premium 12 Months'), 'https://placehold.co/600x400/1DB954/white?text=Spotify+Premium', TRUE),
    ((SELECT product_id FROM products WHERE name='Netflix Premium 1 Month'), 'https://placehold.co/600x400/E50914/white?text=Netflix+Premium', TRUE),
    ((SELECT product_id FROM products WHERE name='Discord Nitro 1 Year'), 'https://placehold.co/600x400/5865F2/white?text=Discord+Nitro', TRUE),
    ((SELECT product_id FROM products WHERE name='Steam Gift Card 500K'), 'https://placehold.co/600x400/171A21/white?text=Steam+500K', TRUE),
    ((SELECT product_id FROM products WHERE name='PlayStation Plus 12 Months'), 'https://placehold.co/600x400/00439C/white?text=PS+Plus+12M', TRUE),
    ((SELECT product_id FROM products WHERE name='Office 365 Family 12 Months'), 'https://placehold.co/600x400/D83B01/white?text=Office+365+Family', TRUE),
    ((SELECT product_id FROM products WHERE name='YouTube Premium 3 Months'), 'https://placehold.co/600x400/FF0000/white?text=YouTube+Premium', TRUE);

-- Secondary images for some products (optional - using placehold.co variants)
INSERT IGNORE INTO product_images (product_id, image_url, is_primary)
VALUES
    ((SELECT product_id FROM products WHERE name='Microsoft Office 2021 Pro Plus'), 'https://placehold.co/600x400/005A9E/white?text=Office+Screenshot', FALSE),
    ((SELECT product_id FROM products WHERE name='Adobe Creative Cloud All Apps'), 'https://placehold.co/600x400/DA1F26/white?text=Adobe+Apps', FALSE),
    ((SELECT product_id FROM products WHERE name='Minecraft Java Edition'), 'https://placehold.co/600x400/62A564/white?text=Minecraft+World', FALSE);

-- =============================
-- PRODUCT REVIEWS
-- =============================
-- Create reviews from existing users (adjust user_id if needed)
INSERT IGNORE INTO product_reviews (product_id, user_id, rating, comment)
VALUES
    ((SELECT product_id FROM products WHERE name='Microsoft Office 2021 Pro Plus'), 2, 5, 'Key nhận ngay lập tức, kích hoạt thành công. Rất hài lòng!'),
    ((SELECT product_id FROM products WHERE name='Microsoft Office 2021 Pro Plus'), 3, 4, 'Giá tốt, support nhiệt tình. Recommend!'),
    ((SELECT product_id FROM products WHERE name='Adobe Creative Cloud All Apps'), 2, 5, 'Subscription 12 tháng work hoàn hảo. Full app Adobe.'),
    ((SELECT product_id FROM products WHERE name='Norton 360 Deluxe 2023'), 3, 4, 'Bảo vệ tốt, VPN nhanh. Đáng đồng tiền.'),
    ((SELECT product_id FROM products WHERE name='Windows 11 Pro OEM'), 2, 5, 'Kích hoạt vĩnh viễn, không lỗi. Giá rẻ hơn mua chính hãng nhiều.'),
    ((SELECT product_id FROM products WHERE name='FIFA 23 - Steam Key'), 3, 4, 'Key global work, add vào Steam ngon lành.'),
    ((SELECT product_id FROM products WHERE name='Minecraft Java Edition'), 2, 5, 'Con nhỏ nhà mình rất thích. Thanks shop!'),
    ((SELECT product_id FROM products WHERE name='Valorant Points 2000 VP'), 3, 5, 'Nhận code trong 2 phút. Nạp thành công. Sẽ mua lại.'),
    ((SELECT product_id FROM products WHERE name='Web Development Bootcamp 2025'), 2, 4, 'Nội dung chi tiết, dễ hiểu. Recommend cho newbie.'),
    ((SELECT product_id FROM products WHERE name='Spotify Premium 12 Months'), 3, 5, 'Nghe nhạc cả năm không quảng cáo. Tuyệt vời!'),
    ((SELECT product_id FROM products WHERE name='Netflix Premium 1 Month'), 2, 4, 'Xem 4K mượt, chia sẻ 4 người. Giá hợp lý.');

-- =============================
-- SAMPLE ORDERS & ORDER ITEMS
-- =============================
-- Order 1: User 2 (alice) buys Office + Norton
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES (2, 1650000.00, DATE_SUB(NOW(), INTERVAL 10 DAY));

SET @order1_id = (SELECT order_id FROM orders WHERE user_id=2 ORDER BY order_id DESC LIMIT 1);

INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES
    (@order1_id, (SELECT product_id FROM products WHERE name='Microsoft Office 2021 Pro Plus'), 1, 1100000.00, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (@order1_id, (SELECT product_id FROM products WHERE name='Norton 360 Deluxe 2023'), 1, 550000.00, DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES (@order1_id, 'VNPay', CONCAT('VNP-',UNIX_TIMESTAMP()), 1650000.00, 'PAID', JSON_OBJECT('method','card','brand','VISA'), DATE_SUB(NOW(), INTERVAL 10 DAY));

-- Order 2: User 3 (bob) buys Minecraft + Spotify
INSERT IGNORE INTO orders (user_id, total_amount, created_at)
VALUES (3, 1260000.00, DATE_SUB(NOW(), INTERVAL 5 DAY));

SET @order2_id = (SELECT order_id FROM orders WHERE user_id=3 ORDER BY order_id DESC LIMIT 1);

INSERT IGNORE INTO order_items (order_id, product_id, quantity, price_at_time, created_at)
VALUES
    (@order2_id, (SELECT product_id FROM products WHERE name='Minecraft Java Edition'), 1, 580000.00, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (@order2_id, (SELECT product_id FROM products WHERE name='Spotify Premium 12 Months'), 1, 680000.00, DATE_SUB(NOW(), INTERVAL 5 DAY));

INSERT IGNORE INTO payment_transactions (order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at)
VALUES (@order2_id, 'MoMo', CONCAT('MOMO-',UNIX_TIMESTAMP()), 1260000.00, 'PAID', JSON_OBJECT('method','wallet'), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- =============================
-- PRODUCT LICENSES
-- =============================
-- Generate licenses for purchased items
INSERT IGNORE INTO product_licenses (order_item_id, user_id, license_key, is_active, activation_date)
SELECT oi.order_item_id, o.user_id,
       CONCAT('LIC-', UPPER(SUBSTRING(MD5(RAND()), 1, 8)), '-', UPPER(SUBSTRING(MD5(RAND()), 1, 8))),
       TRUE, NOW()
FROM order_items oi
         JOIN orders o ON oi.order_id = o.order_id
WHERE oi.order_item_id IN (
    SELECT order_item_id FROM order_items WHERE order_id = @order1_id
    UNION
    SELECT order_item_id FROM order_items WHERE order_id = @order2_id
);

-- =============================
-- SUMMARY
-- =============================
SELECT 'Product samples imported successfully!' AS status;
SELECT COUNT(*) AS total_products FROM products;
SELECT COUNT(*) AS total_images FROM product_images;
SELECT COUNT(*) AS total_reviews FROM product_reviews;
SELECT COUNT(*) AS total_licenses FROM product_licenses;

