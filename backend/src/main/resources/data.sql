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