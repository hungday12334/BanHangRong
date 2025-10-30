-- =============================================
-- CHECK AND FIX PRODUCT STATUS
-- =============================================

USE smiledev_wap;

-- 1. Show all products with their category and current status
SELECT 
    p.product_id,
    p.name AS product_name,
    p.status AS current_status,
    p.quantity,
    p.price,
    c.category_id,
    c.name AS category_name,
    p.created_at
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
ORDER BY c.category_id, p.product_id;

-- 2. Show products that are NOT Public (these won't show on customer pages)
SELECT 
    p.product_id,
    p.name AS product_name,
    p.status AS current_status,
    c.name AS category_name,
    CASE 
        WHEN p.status = 'Draft' THEN '⚠️ Draft - Not visible to customers'
        WHEN p.status = 'Private' THEN '🔒 Private - Not visible to customers'
        WHEN p.status IS NULL THEN '❌ NULL - Not visible to customers'
        ELSE '✅ Public - Visible to customers'
    END AS visibility_note
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
WHERE p.status != 'Public' OR p.status IS NULL
ORDER BY c.category_id, p.product_id;

-- 3. Count products by category and status
SELECT 
    c.category_id,
    c.name AS category_name,
    COUNT(*) AS total_products,
    SUM(CASE WHEN p.status = 'Public' THEN 1 ELSE 0 END) AS public_count,
    SUM(CASE WHEN p.status = 'Draft' THEN 1 ELSE 0 END) AS draft_count,
    SUM(CASE WHEN p.status = 'Private' THEN 1 ELSE 0 END) AS private_count,
    SUM(CASE WHEN p.status IS NULL THEN 1 ELSE 0 END) AS null_status_count
FROM categories c
INNER JOIN products p ON c.category_id = p.category_id
GROUP BY c.category_id, c.name
ORDER BY c.category_id;

-- 4. FIX: Update all non-Public products to Public
-- ⚠️ WARNING: Only run this if you want to make ALL products visible!
-- Uncomment the lines below to execute:

-- UPDATE products 
-- SET status = 'Public' 
-- WHERE status != 'Public' OR status IS NULL;

-- SELECT CONCAT('✅ Updated ', ROW_COUNT(), ' products to Public status') AS result;

-- 5. Alternative: Update only products with NULL or Draft status to Public
-- (keeps Private products as Private)
-- Uncomment to execute:

-- UPDATE products 
-- SET status = 'Public' 
-- WHERE status = 'Draft' OR status IS NULL;

-- SELECT CONCAT('✅ Updated ', ROW_COUNT(), ' products to Public status') AS result;

-- 6. Check results after update
-- Uncomment to see updated counts:

-- SELECT 
--     status,
--     COUNT(*) AS count
-- FROM products
-- GROUP BY status
-- ORDER BY status;

