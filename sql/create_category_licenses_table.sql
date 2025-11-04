-- Create category_licenses table to track which licenses are assigned to each category
-- This allows sellers to manage licenses per category without deleting them from the database

CREATE TABLE IF NOT EXISTS category_licenses (
    category_license_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    shop_license_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_category_licenses_category
        FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_category_licenses_shop_license
        FOREIGN KEY (shop_license_id) REFERENCES shop_licenses(shop_license_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_category_licenses_seller
        FOREIGN KEY (seller_id) REFERENCES users(user_id)
        ON DELETE CASCADE,

    -- Unique constraint to prevent duplicate assignments
    CONSTRAINT uk_category_shop_license
        UNIQUE (category_id, shop_license_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_category_licenses_category_id ON category_licenses(category_id);
CREATE INDEX idx_category_licenses_shop_license_id ON category_licenses(shop_license_id);
CREATE INDEX idx_category_licenses_seller_id ON category_licenses(seller_id);

-- Add comment
ALTER TABLE category_licenses COMMENT = 'Tracks which licenses are assigned to each category for seller management';

