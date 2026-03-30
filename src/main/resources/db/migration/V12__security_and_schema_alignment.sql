-- Flyway V12: security + app/backend schema alignment
-- 1) Rename transactions_summary -> transactions
-- 2) Add transactions.card_id, terminal_id and test_cases req/res paths
-- 3) Drop denormalized/legacy columns requested for cleanup

SET @db_name = DATABASE();

-- Rename transactions_summary -> transactions when needed
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions_summary'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        ),
        'RENAME TABLE transactions_summary TO transactions',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure table exists (fresh env safety)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trace_number VARCHAR(20) NOT NULL,
    amount VARCHAR(20) NULL,
    status VARCHAR(20) NULL,
    masked_pan VARCHAR(25) NULL,
    card_scheme VARCHAR(20) NULL,
    request_hex TEXT NULL,
    response_hex TEXT NULL,
    processing_code VARCHAR(6) NULL,
    currency_code VARCHAR(3) NULL,
    rrn VARCHAR(12) NULL,
    user_id BIGINT NULL,
    device_id VARCHAR(50) NULL,
    txn_timestamp DATETIME NULL,
    synced_at DATETIME NULL,
    terminal_id BIGINT NULL,
    card_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_trace_number (trace_number)
);

-- Add test_cases request/response file paths
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'req_file_path'
        ),
        'ALTER TABLE test_cases ADD COLUMN req_file_path VARCHAR(500) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'res_file_path'
        ),
        'ALTER TABLE test_cases ADD COLUMN res_file_path VARCHAR(500) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add transactions.foreign key columns
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'terminal_id'
        ),
        'ALTER TABLE transactions ADD COLUMN terminal_id BIGINT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'card_id'
        ),
        'ALTER TABLE transactions ADD COLUMN card_id BIGINT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove legacy transactions columns
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'owner_username'
        ),
        'ALTER TABLE transactions DROP COLUMN owner_username',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'terminal_code'
        ),
        'ALTER TABLE transactions DROP COLUMN terminal_code',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove merchants denormalized counters
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'branch_count'
        ),
        'ALTER TABLE merchants DROP COLUMN branch_count',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'branch_addresses'
        ),
        'ALTER TABLE merchants DROP COLUMN branch_addresses',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'account_count'
        ),
        'ALTER TABLE merchants DROP COLUMN account_count',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove server endpoint from pos_accounts (belongs to terminals)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'server_ip'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN server_ip',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'server_port'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN server_port',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Useful indexes for new FK-style columns
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_transactions_terminal_id'
        ),
        'CREATE INDEX idx_transactions_terminal_id ON transactions (terminal_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_transactions_card_id'
        ),
        'CREATE INDEX idx_transactions_card_id ON transactions (card_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

