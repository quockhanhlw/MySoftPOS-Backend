-- Flyway V4: align merchant-account model for app/backend contract.
-- Adds merchant metadata + users.merchant_id relation for multi-account merchants.

SET @db_name = DATABASE();

-- users.merchant_id
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'merchant_id'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN merchant_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- merchants.branch_count
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'branch_count'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN branch_count INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- merchants.branch_addresses
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'branch_addresses'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN branch_addresses VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- merchants.account_count
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'account_count'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN account_count INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.merchant_id index
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND INDEX_NAME = 'idx_users_merchant_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_users_merchant_id ON users (merchant_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.merchant_id FK
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND CONSTRAINT_NAME = 'fk_users_merchant'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD CONSTRAINT fk_users_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON UPDATE RESTRICT ON DELETE SET NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill relation from owner mapping for existing rows.
UPDATE users u
JOIN merchants m ON m.owner_user_id = u.id
SET u.merchant_id = m.id
WHERE u.merchant_id IS NULL;

-- Normalize merchant account_count for older rows.
UPDATE merchants
SET account_count = 1
WHERE account_count IS NULL OR account_count < 1;

