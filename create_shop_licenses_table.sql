-- Create shop_licenses table for managing seller shop licenses
CREATE TABLE IF NOT EXISTS shop_licenses (
    shop_license_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    license_name VARCHAR(100) NOT NULL,
    license_type VARCHAR(50),
    license_number VARCHAR(100),
    description TEXT,
    issue_date DATETIME,
    expiry_date DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    document_url VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_is_active (is_active)
);

-- Insert sample data for testing (optional)
-- Uncomment if you want to test with sample data
/*
INSERT INTO shop_licenses (seller_id, license_name, license_type, license_number, description, is_active, status)
VALUES
(1, 'Giấy phép kinh doanh thực phẩm', 'FOOD_LICENSE', 'TP-2024-001', 'Giấy phép kinh doanh thực phẩm chức năng', TRUE, 'ACTIVE'),
(1, 'Giấy chứng nhận vệ sinh ATTP', 'FOOD_SAFETY', 'VS-2024-123', 'Chứng nhận đủ điều kiện vệ sinh an toàn thực phẩm', TRUE, 'ACTIVE'),
(1, 'Giấy phép bán hàng điện tử', 'ELECTRONICS', 'DT-2024-456', 'Giấy phép kinh doanh thiết bị điện tử', TRUE, 'ACTIVE');
*/

