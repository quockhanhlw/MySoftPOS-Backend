-- Flyway V13: final column-name alignment + PCI cleanup
-- - Drop test_cases.track2
-- - Ensure canonical names: full_name, merchant_name, txn_timestamp, created_at

SET @db_name = DATABASE();

-- pos_accounts.display_name -> full_name (legacy drift guard)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'display_name'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'full_name'
        ),
        'ALTER TABLE pos_accounts CHANGE COLUMN display_name full_name VARCHAR(200) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- merchants.merchant_name_location -> merchant_name (legacy drift guard)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'merchant_name_location'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'merchant_name'
        ),
        'ALTER TABLE merchants CHANGE COLUMN merchant_name_location merchant_name VARCHAR(100) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- transactions.timestamp -> txn_timestamp (legacy drift guard)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'timestamp'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'txn_timestamp'
        ),
        'ALTER TABLE transactions CHANGE COLUMN timestamp txn_timestamp DATETIME NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- test_cases.timestamp -> created_at (legacy drift guard)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'timestamp'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'created_at'
        ),
        'ALTER TABLE test_cases CHANGE COLUMN timestamp created_at DATETIME NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PCI cleanup: remove raw track2 column from backend persistence
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'track2'
        ),
        'ALTER TABLE test_cases DROP COLUMN track2',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

