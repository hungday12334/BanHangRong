-- Script kiểm tra trạng thái hệ thống review
USE wap;

-- Kiểm tra bảng product_reviews
SELECT 'Checking product_reviews table...' as status;
DESCRIBE product_reviews;

-- Kiểm tra constraints
SELECT 'Checking constraints...' as status;
SELECT 
    tc.CONSTRAINT_NAME,
    tc.CONSTRAINT_TYPE,
    kcu.COLUMN_NAME
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    AND tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
    AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
    AND tc.TABLE_NAME = kcu.TABLE_NAME
WHERE tc.TABLE_SCHEMA = 'wap'
AND tc.TABLE_NAME = 'product_reviews';

-- Kiểm tra indexes
SELECT 'Checking indexes...' as status;
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.statistics 
WHERE TABLE_SCHEMA = 'wap'
AND TABLE_NAME = 'product_reviews'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- Kiểm tra foreign keys
SELECT 'Checking foreign keys...' as status;
SELECT 
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.key_column_usage 
WHERE TABLE_SCHEMA = 'wap'
AND TABLE_NAME = 'product_reviews'
AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Kiểm tra triggers
SELECT 'Checking triggers...' as status;
SELECT 
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    ACTION_TIMING
FROM information_schema.triggers 
WHERE TRIGGER_SCHEMA = 'wap'
AND EVENT_OBJECT_TABLE = 'product_reviews';

-- Kiểm tra procedures
SELECT 'Checking procedures...' as status;
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.routines 
WHERE ROUTINE_SCHEMA = 'wap'
AND (ROUTINE_NAME LIKE '%review%' OR ROUTINE_NAME LIKE '%rating%');

-- Kiểm tra functions
SELECT 'Checking functions...' as status;
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.routines 
WHERE ROUTINE_SCHEMA = 'wap'
AND (ROUTINE_NAME LIKE '%Product%' OR ROUTINE_NAME LIKE '%Review%');

-- Kiểm tra views
SELECT 'Checking views...' as status;
SELECT 
    TABLE_NAME,
    VIEW_DEFINITION
FROM information_schema.views 
WHERE TABLE_SCHEMA = 'wap'
AND (TABLE_NAME LIKE '%review%' OR TABLE_NAME LIKE '%rating%');

-- Kiểm tra cột rating trong products
SELECT 'Checking products table for rating columns...' as status;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.columns 
WHERE TABLE_SCHEMA = 'wap'
AND TABLE_NAME = 'products'
AND (COLUMN_NAME LIKE '%rating%' OR COLUMN_NAME LIKE '%review%');

-- Kiểm tra dữ liệu mẫu
SELECT 'Checking sample data...' as status;
SELECT COUNT(*) as total_reviews FROM product_reviews;
SELECT COUNT(*) as total_products_with_rating FROM products WHERE average_rating > 0;

SELECT 'Review system status check completed!' as result;
