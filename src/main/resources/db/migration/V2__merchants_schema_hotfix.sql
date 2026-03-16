-- Flyway V2 hotfix: ensure merchants schema matches entity mapping.
-- Safe for environments that accidentally baselined at version 1 and skipped V1 script.

SET @db_name = DATABASE();

-- Ensure required columns exist.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'owner_user_id'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN owner_user_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'business_type'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN business_type VARCHAR(4) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'store_address'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN store_address VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure indexes exist.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND INDEX_NAME = 'idx_merchants_admin_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_merchants_admin_id ON merchants (admin_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND INDEX_NAME = 'idx_merchants_business_type'
        ),
        'SELECT 1',
        'CREATE INDEX idx_merchants_business_type ON merchants (business_type)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND INDEX_NAME = 'uq_merchants_owner_user_id'
        ),
        'SELECT 1',
        'CREATE UNIQUE INDEX uq_merchants_owner_user_id ON merchants (owner_user_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure FK exists.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND CONSTRAINT_NAME = 'fk_merchants_owner_user'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD CONSTRAINT fk_merchants_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id) ON UPDATE RESTRICT ON DELETE RESTRICT'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

