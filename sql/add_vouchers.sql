-- Vouchers feature tables (MySQL compatible)

CREATE TABLE IF NOT EXISTS vouchers (
  voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  discount_type VARCHAR(16) NOT NULL, -- PERCENT | AMOUNT
  discount_value DECIMAL(12,2) NOT NULL DEFAULT 0,
  min_order DECIMAL(12,2) DEFAULT NULL,
  start_at DATETIME DEFAULT NULL,
  end_at DATETIME DEFAULT NULL,
  max_uses INT DEFAULT NULL,
  max_uses_per_user INT DEFAULT NULL,
  used_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ux_vouchers_seller_product_code UNIQUE (seller_id, product_id, code),
  INDEX ix_vouchers_product (product_id, status),
  CONSTRAINT fk_vouchers_seller FOREIGN KEY (seller_id) REFERENCES users(user_id),
  CONSTRAINT fk_vouchers_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS voucher_redemptions (
  redeem_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  voucher_id BIGINT NOT NULL,
  order_id BIGINT DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  discount_amount DECIMAL(12,2) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX ix_voucher_redemptions_voucher (voucher_id, created_at),
  CONSTRAINT fk_redemptions_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
  CONSTRAINT fk_redemptions_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
  CONSTRAINT fk_redemptions_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================
-- SAMPLE DATA (safe-ish): will bind vouchers to the first product of seller_id = 1 if exists
-- You can change @seller to another ID as needed

SET @seller := 1;
SET @p1 := (SELECT p.product_id FROM products p WHERE p.seller_id = @seller ORDER BY p.product_id LIMIT 1);

-- Insert two vouchers for that product (if the product exists)
INSERT INTO vouchers (seller_id, product_id, code, discount_type, discount_value, min_order, start_at, end_at, max_uses, max_uses_per_user, status)
SELECT @seller, @p1, 'FALL10', 'PERCENT', 10.00, NULL, NOW(), NOW() + INTERVAL 30 DAY, 100, 1, 'active'
WHERE @p1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM vouchers v WHERE v.seller_id = @seller AND v.product_id = @p1 AND v.code = 'FALL10'
);

INSERT INTO vouchers (seller_id, product_id, code, discount_type, discount_value, min_order, start_at, end_at, max_uses, max_uses_per_user, status)
SELECT @seller, @p1, 'SAVE5', 'AMOUNT', 5.00, 20.00, NOW(), NOW() + INTERVAL 14 DAY, 50, NULL, 'active'
WHERE @p1 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM vouchers v WHERE v.seller_id = @seller AND v.product_id = @p1 AND v.code = 'SAVE5'
);

-- Create one redemption for FALL10 if there is an order for that product; otherwise create a redemption without order/user
SET @v1 := (SELECT voucher_id FROM vouchers WHERE seller_id = @seller AND product_id = @p1 AND code = 'FALL10' LIMIT 1);

INSERT INTO voucher_redemptions (voucher_id, order_id, user_id, discount_amount, created_at)
SELECT @v1, oi.order_id, o.user_id, 10.00, NOW()
FROM order_items oi
JOIN orders o ON o.order_id = oi.order_id
WHERE oi.product_id = @p1
ORDER BY o.created_at DESC
LIMIT 1;

-- Fallback redemption if no order found
INSERT INTO voucher_redemptions (voucher_id, discount_amount, created_at)
SELECT @v1, 10.00, NOW()
WHERE @v1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM voucher_redemptions r WHERE r.voucher_id = @v1);

-- Sync used_count for the two vouchers
SET @v2 := (SELECT voucher_id FROM vouchers WHERE seller_id = @seller AND product_id = @p1 AND code = 'SAVE5' LIMIT 1);
UPDATE vouchers v
SET v.used_count = (SELECT COUNT(*) FROM voucher_redemptions r WHERE r.voucher_id = v.voucher_id)
WHERE v.voucher_id IN (@v1, @v2) AND v.voucher_id IS NOT NULL;
