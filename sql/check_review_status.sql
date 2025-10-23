-- Script kiểm tra trạng thái hệ thống review
USE wap;

-- Kiểm tra bảng product_reviews
SELECT 'Checking product_reviews table...' as status;
DESCRIBE product_reviews;

-- Kiểm tra constraints
SELECT 'Checking constraints...' as status;
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    COLUMN_NAME
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name 
    AND tc.table_schema = kcu.table_schema
WHERE tc.table_schema = 'wap' 
AND tc.table_name = 'product_reviews';

-- Kiểm tra indexes
SELECT 'Checking indexes...' as status;
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.statistics 
WHERE table_schema = 'wap' 
AND table_name = 'product_reviews'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- Kiểm tra foreign keys
SELECT 'Checking foreign keys...' as status;
SELECT 
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.key_column_usage 
WHERE table_schema = 'wap' 
AND table_name = 'product_reviews' 
AND referenced_table_name IS NOT NULL;

-- Kiểm tra triggers
SELECT 'Checking triggers...' as status;
SELECT 
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    ACTION_TIMING
FROM information_schema.triggers 
WHERE table_schema = 'wap' 
AND table_name = 'product_reviews';

-- Kiểm tra procedures
SELECT 'Checking procedures...' as status;
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.routines 
WHERE table_schema = 'wap' 
AND ROUTINE_NAME LIKE '%review%' OR ROUTINE_NAME LIKE '%rating%';

-- Kiểm tra functions
SELECT 'Checking functions...' as status;
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE
FROM information_schema.routines 
WHERE table_schema = 'wap' 
AND ROUTINE_NAME LIKE '%Product%' OR ROUTINE_NAME LIKE '%Review%';

-- Kiểm tra views
SELECT 'Checking views...' as status;
SELECT 
    TABLE_NAME,
    VIEW_DEFINITION
FROM information_schema.views 
WHERE table_schema = 'wap' 
AND table_name LIKE '%review%' OR table_name LIKE '%rating%';

-- Kiểm tra cột rating trong products
SELECT 'Checking products table for rating columns...' as status;
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.columns 
WHERE table_schema = 'wap' 
AND table_name = 'products' 
AND (column_name LIKE '%rating%' OR column_name LIKE '%review%');

-- Kiểm tra dữ liệu mẫu
SELECT 'Checking sample data...' as status;
SELECT COUNT(*) as total_reviews FROM product_reviews;
SELECT COUNT(*) as total_products_with_rating FROM products WHERE average_rating > 0;

SELECT 'Review system status check completed!' as result;
