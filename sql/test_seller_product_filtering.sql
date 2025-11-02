-- =====================================================
-- Test Script: Verify Seller-Specific Product Filtering
-- =====================================================

-- 1. Show all sellers and their product counts
SELECT
    u.user_id,
    u.username,
    u.email,
    u.user_type,
    COUNT(DISTINCT p.product_id) as total_products,
    COUNT(DISTINCT cp.category_id) as categories_used
FROM users u
LEFT JOIN products p ON u.user_id = p.seller_id
LEFT JOIN categories_products cp ON p.product_id = cp.product_id
WHERE u.user_type = 'seller'
GROUP BY u.user_id, u.username, u.email, u.user_type
ORDER BY total_products DESC;

-- 2. Products distribution per category per seller
SELECT
    u.user_id as seller_id,
    u.username as seller_name,
    c.category_id,
    c.name as category_name,
    COUNT(p.product_id) as product_count,
    GROUP_CONCAT(p.name ORDER BY p.name SEPARATOR ' | ') as product_names
FROM users u
CROSS JOIN categories c
LEFT JOIN products p ON u.user_id = p.seller_id
LEFT JOIN categories_products cp ON p.product_id = cp.product_id AND cp.category_id = c.category_id
WHERE u.user_type = 'seller'
GROUP BY u.user_id, u.username, c.category_id, c.name
HAVING product_count > 0
ORDER BY u.username, c.name;

-- 3. Detailed product info with categories for each seller
SELECT
    u.username as seller_name,
    p.product_id,
    p.name as product_name,
    p.price,
    p.quantity as stock,
    p.status,
    p.total_sales,
    GROUP_CONCAT(DISTINCT c.name ORDER BY c.name SEPARATOR ', ') as categories
FROM users u
JOIN products p ON u.user_id = p.seller_id
LEFT JOIN categories_products cp ON p.product_id = cp.product_id
LEFT JOIN categories c ON cp.category_id = c.category_id
WHERE u.user_type = 'seller'
GROUP BY u.username, p.product_id, p.name, p.price, p.quantity, p.status, p.total_sales
ORDER BY u.username, p.name;

-- 4. Test specific seller (change @seller_id to test different sellers)
SET @seller_id = 1; -- Change this to test different sellers

SELECT
    CONCAT('Testing Seller ID: ', @seller_id) as test_info;

SELECT
    u.user_id,
    u.username,
    u.email,
    COUNT(DISTINCT p.product_id) as my_total_products
FROM users u
LEFT JOIN products p ON u.user_id = p.seller_id
WHERE u.user_id = @seller_id
GROUP BY u.user_id, u.username, u.email;

-- Show products per category for this seller
SELECT
    c.category_id,
    c.name as category_name,
    COUNT(p.product_id) as product_count,
    GROUP_CONCAT(p.name ORDER BY p.name SEPARATOR ', ') as products
FROM categories c
LEFT JOIN categories_products cp ON c.category_id = cp.category_id
LEFT JOIN products p ON cp.product_id = p.product_id AND p.seller_id = @seller_id
GROUP BY c.category_id, c.name
ORDER BY c.name;

-- 5. Compare: Show same category for different sellers
SELECT
    c.category_id,
    c.name as category_name,
    u.user_id as seller_id,
    u.username as seller_name,
    COUNT(p.product_id) as product_count,
    GROUP_CONCAT(p.name ORDER BY p.name SEPARATOR ', ') as products
FROM categories c
CROSS JOIN users u
LEFT JOIN products p ON p.seller_id = u.user_id
LEFT JOIN categories_products cp ON cp.product_id = p.product_id AND cp.category_id = c.category_id
WHERE u.user_type = 'seller'
GROUP BY c.category_id, c.name, u.user_id, u.username
HAVING product_count > 0
ORDER BY c.name, u.username;

-- 6. Verify no data leakage: Products should ONLY belong to ONE seller
SELECT
    p.product_id,
    p.name,
    COUNT(DISTINCT p.seller_id) as seller_count
FROM products p
GROUP BY p.product_id, p.name
HAVING seller_count > 1;
-- Expected: Empty result (no products belong to multiple sellers)

-- 7. Check products without category assignment
SELECT
    u.username as seller_name,
    p.product_id,
    p.name as product_name,
    'No category assigned' as issue
FROM products p
JOIN users u ON p.seller_id = u.user_id
LEFT JOIN categories_products cp ON p.product_id = cp.product_id
WHERE cp.product_id IS NULL
AND u.user_type = 'seller'
ORDER BY u.username, p.name;

-- 8. Summary statistics per seller
SELECT
    u.user_id,
    u.username,
    COUNT(DISTINCT p.product_id) as total_products,
    COUNT(DISTINCT cp.category_id) as categories_with_products,
    SUM(p.quantity) as total_stock,
    SUM(COALESCE(p.total_sales, 0)) as total_sales,
    ROUND(AVG(p.price), 2) as avg_product_price,
    MAX(p.price) as max_price,
    MIN(p.price) as min_price
FROM users u
LEFT JOIN products p ON u.user_id = p.seller_id
LEFT JOIN categories_products cp ON p.product_id = cp.product_id
WHERE u.user_type = 'seller'
GROUP BY u.user_id, u.username
ORDER BY total_products DESC;

-- 9. Expected behavior test: What each seller SHOULD see
-- Run this and compare with actual UI display

-- Seller A view (user_id = 1)
SET @test_seller = 1;
SELECT
    CONCAT('=== View for Seller: ', u.username, ' (ID: ', @test_seller, ') ===') as display_title
FROM users u WHERE u.user_id = @test_seller;

SELECT
    c.name as category_name,
    COUNT(p.product_id) as product_count,
    'This is what seller should see in UI' as note
FROM categories c
LEFT JOIN categories_products cp ON c.category_id = cp.category_id
LEFT JOIN products p ON cp.product_id = p.product_id AND p.seller_id = @test_seller
GROUP BY c.category_id, c.name
ORDER BY c.name;

-- 10. Test queries that backend is using
-- These should match what the application returns

-- Test countByCategoryIdAndSellerId
SET @test_category = 1;
SET @test_seller = 1;

SELECT
    CONCAT('Testing: countByCategoryIdAndSellerId(', @test_category, ', ', @test_seller, ')') as query_info;

SELECT COUNT(p.product_id) as result
FROM products p
WHERE p.seller_id = @test_seller
AND EXISTS (
    SELECT 1 FROM categories_products cp
    WHERE cp.product_id = p.product_id
    AND cp.category_id = @test_category
);

-- Test findByCategoryIdAndSellerId
SELECT
    CONCAT('Testing: findByCategoryIdAndSellerId(', @test_category, ', ', @test_seller, ')') as query_info;

SELECT
    p.product_id,
    p.name,
    p.price,
    p.quantity,
    p.status,
    p.total_sales
FROM products p
WHERE p.seller_id = @test_seller
AND EXISTS (
    SELECT 1 FROM categories_products cp
    WHERE cp.product_id = p.product_id
    AND cp.category_id = @test_category
)
ORDER BY p.name;

-- =====================================================
-- MANUAL TESTING CHECKLIST
-- =====================================================
--
-- [ ] Run all queries above
-- [ ] Note the product counts per seller
-- [ ] Login as Seller A → verify UI matches query results
-- [ ] Login as Seller B → verify UI matches query results
-- [ ] Check that Seller A CANNOT see Seller B's products
-- [ ] Check that product counts are correct per category
-- [ ] Verify API responses match database queries
--
-- =====================================================

