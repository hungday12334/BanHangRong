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

