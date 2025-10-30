-- Check products status by category
-- This will show all products with their status

USE smiledev_wap;

-- Show all products grouped by category with their status
SELECT 
    c.category_id,
    c.name AS category_name,
    p.product_id,
    p.name AS product_name,
    p.status,
    p.quantity,
    p.created_at
FROM categories c
LEFT JOIN products p ON c.category_id = p.category_id
WHERE c.category_id IN (
    SELECT DISTINCT category_id 
    FROM products 
    WHERE category_id IS NOT NULL
)
ORDER BY c.category_id, p.status, p.product_id;

-- Count products by category and status
SELECT 
    c.category_id,
    c.name AS category_name,
    p.status,
    COUNT(*) AS product_count
FROM categories c
LEFT JOIN products p ON c.category_id = p.category_id
WHERE p.product_id IS NOT NULL
GROUP BY c.category_id, c.name, p.status
ORDER BY c.category_id, p.status;

-- Show category with exactly 3 products (any status)
SELECT 
    c.category_id,
    c.name AS category_name,
    COUNT(p.product_id) AS total_products,
    SUM(CASE WHEN p.status = 'Public' THEN 1 ELSE 0 END) AS public_products,
    SUM(CASE WHEN p.status = 'Draft' THEN 1 ELSE 0 END) AS draft_products,
    SUM(CASE WHEN p.status = 'Private' THEN 1 ELSE 0 END) AS private_products
FROM categories c
LEFT JOIN products p ON c.category_id = p.category_id
GROUP BY c.category_id, c.name
HAVING total_products = 3
ORDER BY c.category_id;

