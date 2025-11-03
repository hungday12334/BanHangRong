-- MySQL 8+ sample data seeder
-- Inserts >= 20 rows for key entities to enable testing
-- Safe to run multiple times if tables are empty; uses simple ID offsets and ON DUPLICATE KEY where applicable

SET NAMES utf8mb4;
SET time_zone = '+07:00';

-- Generate numbers 1..20
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n+1 FROM seq WHERE n < 20
)
-- Users
INSERT INTO users (user_id, username, email, password, full_name, user_type, avatar_url, phone_number, gender, birth_date, balance, is_email_verified, is_active, last_login, created_at, updated_at)
SELECT 
  1000 + n AS user_id,
  CONCAT('user', n),
  CONCAT('user', n, '@example.com'),
  '$2a$10$z3b4q2WJvFakeHashForLocalTestingOnly1234567890abcdefghijk', -- bcrypt placeholder
  CONCAT('Test User ', n),
  CASE WHEN n % 5 = 0 THEN 'seller' ELSE 'customer' END,
  CONCAT('/img/avatar_', n, '.jpg'),
  CONCAT('090', LPAD(n, 8, '0')),
  CASE WHEN n % 2 = 0 THEN 'male' ELSE 'female' END,
  DATE('1990-01-01') + INTERVAL n DAY,
  1000000 + n * 50000,
  (n % 2 = 0),
  TRUE,
  NOW() - INTERVAL n DAY,
  NOW() - INTERVAL (30 + n) DAY,
  NOW() - INTERVAL n DAY
FROM seq
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Products (tied to user 1000 as seller when not seller assign seller 1005)
INSERT INTO products (product_id, seller_id, name, description, price, sale_price, quantity, download_url, total_sales, average_rating, status, created_at, updated_at)
SELECT 
  2000 + n AS product_id,
  1000 + (CASE WHEN n % 5 = 0 THEN 5 ELSE 1 END) AS seller_id,
  CONCAT('Product ', n),
  CONCAT('Sample description for product ', n),
  200000 + n * 10000,
  CASE WHEN n % 3 = 0 THEN 180000 + n * 8000 ELSE NULL END,
  50 + n,
  CONCAT('https://download.example.com/p/', n),
  n * 3,
  ROUND(3.5 + (n % 10) * 0.1, 1),
  'Public',
  NOW() - INTERVAL (60 + n) DAY,
  NOW() - INTERVAL n DAY
FROM seq
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Product images (primary per product)
INSERT INTO product_images (image_id, product_id, image_url, is_primary, created_at)
SELECT 3000 + n, 2000 + n, CONCAT('https://cdn.example.com/img/p', n, '.jpg'), TRUE, NOW() - INTERVAL n DAY
FROM seq
ON DUPLICATE KEY UPDATE image_url = VALUES(image_url);

-- Vouchers (attach to product n)
INSERT INTO vouchers (voucher_id, seller_id, product_id, code, discount_type, discount_value, min_order, start_at, end_at, max_uses, max_uses_per_user, used_count, status, created_at, updated_at)
SELECT 
  4000 + n,
  1000 + (CASE WHEN n % 5 = 0 THEN 5 ELSE 1 END) AS seller_id,
  2000 + n AS product_id,
  UPPER(CONCAT('SAVE', LPAD(n,2,'0'))),
  CASE WHEN n % 2 = 0 THEN 'AMOUNT' ELSE 'PERCENT' END,
  CASE WHEN n % 2 = 0 THEN 5000 + n*500 ELSE 5 + (n % 20) END,
  CASE WHEN n % 3 = 0 THEN 50000 ELSE NULL END,
  NOW() - INTERVAL 7 DAY,
  NOW() + INTERVAL 30 DAY,
  CASE WHEN n % 4 = 0 THEN 100 ELSE NULL END,
  CASE WHEN n % 3 = 0 THEN 2 ELSE NULL END,
  0,
  'active',
  NOW() - INTERVAL 10 DAY,
  NOW()
FROM seq
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- Orders
INSERT INTO orders (order_id, user_id, seller_id, total_amount, status, created_at, updated_at)
SELECT 5000 + n, 1000 + n, 1001, 300000 + n * 10000, 'completed', NOW() - INTERVAL (15 + n) DAY, NOW() - INTERVAL (14 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- Order items (each order buys product n)
INSERT INTO order_items (order_item_id, order_id, product_id, quantity, price_at_time, created_at)
SELECT 6000 + n, 5000 + n, 2000 + n, 1 + (n % 3), 200000 + n * 10000, NOW() - INTERVAL (15 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- Shopping cart (for user 1000 add product n)
INSERT INTO shopping_cart (cart_id, user_id, product_id, quantity, created_at, updated_at)
SELECT 7000 + n, 1000, 2000 + n, 1 + (n % 2), NOW() - INTERVAL (2 + n) DAY, NOW() - INTERVAL (1 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- Product reviews (simple)
INSERT INTO product_reviews (review_id, product_id, user_id, rating, comment, created_at)
SELECT 8000 + n, 2000 + n, 1000 + n, 4 + (n % 2), CONCAT('Great product ', n), NOW() - INTERVAL (20 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

-- Payment transactions (link to order)
INSERT INTO payment_transactions (transaction_id, order_id, payment_provider, provider_transaction_id, amount, status, payment_data, created_at, updated_at)
SELECT 9000 + n, 5000 + n, 'WALLET', CONCAT('WAL-', 5000 + n), 300000 + n * 10000, 'success', NULL, NOW() - INTERVAL (15 + n) DAY, NOW() - INTERVAL (14 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- Voucher redemptions (link order + voucher)
INSERT INTO voucher_redemptions (redeem_id, voucher_id, order_id, user_id, discount_amount, created_at)
SELECT 9500 + n, 4000 + n, 5000 + n, 1000 + n, CASE WHEN n % 2 = 0 THEN 10000 ELSE 0 END, NOW() - INTERVAL (15 + n) DAY
FROM seq
ON DUPLICATE KEY UPDATE discount_amount = VALUES(discount_amount);

-- Minimal categories and mapping (3 categories reused)
INSERT INTO categories (category_id, category_name, created_at, updated_at)
VALUES (1,'Software', NOW(), NOW()),(2,'Games', NOW(), NOW()),(3,'Keys', NOW(), NOW())
ON DUPLICATE KEY UPDATE category_name=VALUES(category_name);

INSERT INTO categories_products (category_id, product_id)
SELECT CASE WHEN n % 3 = 0 THEN 3 WHEN n % 2 = 0 THEN 2 ELSE 1 END, 2000 + n FROM seq
ON DUPLICATE KEY UPDATE category_id = VALUES(category_id);

-- Product licenses (one per product for demo)
INSERT INTO product_licenses (license_id, product_id, license_key, status, created_at, updated_at)
SELECT 10000 + n, 2000 + n, CONCAT('KEY-', LPAD(2000 + n, 6, '0')), 'available', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 DAY
FROM seq
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Bank accounts (for sellers)
INSERT INTO bank_accounts (bank_account_id, user_id, bank_name, bank_code, account_number, account_holder_name, branch, is_default, created_at, updated_at)
SELECT 11000 + n, 1001, 'ACB', 'ACB', CONCAT('1001', LPAD(n,8,'0')), 'Seller One', 'HCMC', (n=1), NOW(), NOW()
FROM seq
ON DUPLICATE KEY UPDATE bank_name=VALUES(bank_name);

-- Withdrawal requests (for seller)
INSERT INTO withdrawal_requests (withdrawal_request_id, user_id, amount, status, created_at, updated_at)
SELECT 12000 + n, 1001, 100000 + n*10000, 'pending', NOW() - INTERVAL (n) DAY, NOW() - INTERVAL (n-1) DAY
FROM seq
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Chat rooms and messages (minimal)
INSERT INTO chat_room (room_id, buyer_id, seller_id, created_at)
SELECT 13000 + n, 1000 + n, 1001, NOW() - INTERVAL (n) DAY FROM seq
ON DUPLICATE KEY UPDATE seller_id=VALUES(seller_id);

INSERT INTO chat_message (message_id, room_id, sender_id, content, created_at)
SELECT 14000 + n, 13000 + n, 1000 + n, CONCAT('Hello seller, I have a question #', n), NOW() - INTERVAL (n) DAY FROM seq
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- User sessions (mock)
INSERT INTO user_sessions (session_id, user_id, token, expires_at, created_at)
SELECT 15000 + n, 1000 + n, CONCAT('tok_', 15000 + n), NOW() + INTERVAL 7 DAY, NOW() FROM seq
ON DUPLICATE KEY UPDATE token=VALUES(token);

-- Social auth (mock)
INSERT INTO user_social_auth (id, user_id, provider, provider_user_id, created_at)
SELECT 16000 + n, 1000 + n, 'github', CONCAT('gh_', 1000 + n), NOW() FROM seq
ON DUPLICATE KEY UPDATE provider_user_id=VALUES(provider_user_id);

-- Seller shop sections and featured products (minimal)
INSERT INTO seller_shop_sections (section_id, seller_id, title, description, display_order, created_at, updated_at)
SELECT 17000 + n, 1001, CONCAT('Section ', n), 'Demo section', n, NOW(), NOW() FROM seq
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO seller_featured_products (id, seller_id, product_id, featured_at)
SELECT 18000 + n, 1001, 2000 + n, NOW() - INTERVAL n DAY FROM seq
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id);

-- Section types (3 static)
INSERT INTO section_type (section_type_id, name)
VALUES (1,'grid'),(2,'carousel'),(3,'list')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Email token + password reset (mock)
INSERT INTO email_verification_token (id, user_id, token, expires_at, created_at)
SELECT 19000 + n, 1000 + n, CONCAT('emv_', 1000 + n), NOW() + INTERVAL 3 DAY, NOW() FROM seq
ON DUPLICATE KEY UPDATE token=VALUES(token);

INSERT INTO password_reset_token (id, user_id, token, expires_at, created_at)
SELECT 20000 + n, 1000 + n, CONCAT('rst_', 1000 + n), NOW() + INTERVAL 1 DAY, NOW() FROM seq
ON DUPLICATE KEY UPDATE token=VALUES(token);

-- Done
SELECT 'Seed sample completed' AS info;


