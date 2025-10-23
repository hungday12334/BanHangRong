USE wap;

-- Bank Accounts for withdrawals
CREATE TABLE IF NOT EXISTS bank_accounts (
    bank_account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20),
    account_number VARCHAR(64) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    branch VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ux_user_account UNIQUE (user_id, account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Withdrawal requests with 2% default fee stored per record
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    withdrawal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_account_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    fee_percent DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    fee_amount DECIMAL(15,2) NOT NULL,
    net_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    note VARCHAR(255),
    payout_provider VARCHAR(50),
    provider_reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    CONSTRAINT fk_withdraw_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_withdraw_bank FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(bank_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
