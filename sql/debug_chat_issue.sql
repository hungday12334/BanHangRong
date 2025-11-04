-- DEBUG SCRIPT: Check products and sellers

-- 1. Check if products have sellerId
SELECT
    product_id,
    name,
    seller_id,
    price,
    status
FROM products
LIMIT 10;

-- 2. Check if sellers exist and have correct user_type
SELECT
    user_id,
    username,
    full_name,
    user_type,
    email
FROM users
WHERE user_type = 'SELLER'
LIMIT 10;

-- 3. Check products without sellers
SELECT
    p.product_id,
    p.name,
    p.seller_id,
    u.username,
    u.user_type
FROM products p
LEFT JOIN users u ON p.seller_id = u.user_id
WHERE p.seller_id IS NULL OR u.user_id IS NULL
LIMIT 10;

-- 4. Check products with invalid sellers (not SELLER type)
SELECT
    p.product_id,
    p.name,
    p.seller_id,
    u.username,
    u.user_type
FROM products p
INNER JOIN users u ON p.seller_id = u.user_id
WHERE u.user_type != 'SELLER'
LIMIT 10;

-- 5. Fix: Update products without seller_id (assign to test_seller1)
-- UNCOMMENT to run:
-- UPDATE products
-- SET seller_id = (SELECT user_id FROM users WHERE username = 'test_seller1' LIMIT 1)
-- WHERE seller_id IS NULL;

-- 6. Fix: Ensure test users exist
-- Check if test_seller1 exists
SELECT user_id, username, user_type
FROM users
WHERE username = 'test_seller1';

-- Check if test_customer1 exists
SELECT user_id, username, user_type
FROM users
WHERE username = 'test_customer1';

