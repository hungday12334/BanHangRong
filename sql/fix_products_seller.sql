-- QUICK FIX: Ensure all products have valid sellers

-- Step 1: Check if test_seller1 exists
SELECT user_id, username, user_type FROM users WHERE username = 'test_seller1';

-- Step 2: If test_seller1 doesn't exist, create it
-- (Skip if already exists)
INSERT INTO users (username, password, email, full_name, user_type, is_verified)
SELECT 'test_seller1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCa',
       'seller1@test.com', 'Test Seller 1', 'SELLER', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'test_seller1');

-- Step 3: Get seller ID
SET @seller_id = (SELECT user_id FROM users WHERE username = 'test_seller1' LIMIT 1);

-- Step 4: Update all products without seller_id
UPDATE products
SET seller_id = @seller_id
WHERE seller_id IS NULL;

-- Step 5: Update products with invalid sellers (user_type != SELLER)
UPDATE products p
INNER JOIN users u ON p.seller_id = u.user_id
SET p.seller_id = @seller_id
WHERE u.user_type != 'SELLER';

-- Step 6: Verify fix
SELECT
    COUNT(*) as total_products,
    COUNT(DISTINCT seller_id) as unique_sellers,
    SUM(CASE WHEN seller_id IS NULL THEN 1 ELSE 0 END) as products_without_seller
FROM products;

-- Step 7: Show products with their sellers
SELECT
    p.product_id,
    p.name,
    p.seller_id,
    u.username as seller_username,
    u.user_type
FROM products p
LEFT JOIN users u ON p.seller_id = u.user_id
LIMIT 10;

