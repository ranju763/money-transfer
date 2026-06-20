CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(16) PRIMARY KEY,
    holder_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    balance DECIMAL(18,2) NOT NULL,
    status ENUM('ACTIVE', 'CLOSED', 'LOCKED') NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_logs (
    id BINARY(16) PRIMARY KEY,
    from_account BIGINT NOT NULL,
    to_account BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    failure_reason VARCHAR(255),
    idempotency_key VARCHAR(36) NOT NULL UNIQUE,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (from_account) REFERENCES accounts(id),
    FOREIGN KEY (to_account) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS rewards (
    id BINARY(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL,
    transaction_id BINARY(16) NOT NULL UNIQUE,
    points INT NOT NULL,
    transaction_amount DECIMAL(18,2) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (transaction_id) REFERENCES transaction_logs(id)
);

CREATE TABLE IF NOT EXISTS promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    coin_cost INT NOT NULL,
    discount_percent INT NOT NULL,
    min_spend INT NOT NULL,
    max_discount INT NOT NULL,
    validity_days INT NOT NULL,
    category VARCHAR(50),
    active BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS redemptions (
    id BINARY(16) PRIMARY KEY,
    account_id VARCHAR(16) NOT NULL,
    promotion_id BIGINT NOT NULL,
    coins_spent INT NOT NULL,
    code VARCHAR(40) NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_on TIMESTAMP NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(id)
);