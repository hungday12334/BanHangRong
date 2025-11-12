-- Add unique constraint to prevent duplicate product names per seller (case-insensitive depends on collation)
-- Ensure your 'products.name' column uses a case-insensitive collation if you need case-insensitive uniqueness
ALTER TABLE products
ADD CONSTRAINT uq_products_seller_name UNIQUE (seller_id, name);
