-- H2 Compatible SQL for BanHangRong
-- Users
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(512) NOT NULL,
    user_type VARCHAR(20),
    avatar_url VARCHAR(255),
    phone_number VARCHAR(20),
    gender VARCHAR(10),
    birth_date DATE,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_users_username UNIQUE (username),
    CONSTRAINT ux_users_email UNIQUE (email)
);

-- Social Authentication
CREATE TABLE IF NOT EXISTS user_social_auth (
    auth_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_social_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_user_social_provider UNIQUE (provider, provider_user_id)
);

-- Password Reset Tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Email Verification Tokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(category_id)
);

-- Products
CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    stock_quantity INTEGER DEFAULT 0,
    seller_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(user_id)
);

-- Sample Data
INSERT INTO users (username, email, password, user_type, phone_number, gender, balance, is_email_verified, is_active, last_login)
VALUES
    ('nguyenhung1401', 'hung@example.com', '123456', 'ADMIN', '0900000000', 'male', 1000.00, TRUE, TRUE, CURRENT_TIMESTAMP),
    ('admin', 'admin@example.com', 'admin123', 'ADMIN', '0900000001', 'other', 500.00, TRUE, TRUE, CURRENT_TIMESTAMP),
    ('testuser', 'test@example.com', 'test123', 'USER', '0900000002', 'female', 50.00, TRUE, TRUE, CURRENT_TIMESTAMP),
    ('seller', 'seller@example.com', 'seller123', 'SELLER', '0900000003', 'other', 2000.00, TRUE, TRUE, CURRENT_TIMESTAMP);

INSERT INTO categories (name, description, is_active)
VALUES
    ('Điện thoại', 'Các loại điện thoại di động', TRUE),
    ('Laptop', 'Máy tính xách tay', TRUE),
    ('Phụ kiện', 'Phụ kiện điện tử', TRUE);

INSERT INTO products (name, description, price, stock_quantity, seller_id, is_active)
VALUES
    ('iPhone 15', 'Điện thoại iPhone 15 mới nhất', 25000000.00, 10, 4, TRUE),
    ('Samsung Galaxy S24', 'Điện thoại Samsung Galaxy S24', 20000000.00, 15, 4, TRUE),
    ('MacBook Pro M3', 'Laptop MacBook Pro M3 chip', 45000000.00, 5, 4, TRUE);
