-- Flyway V24: make merchant owner reference resilient when pos_accounts are deleted.
-- Prevents delete failures on fk_merchants_owner_pos_account by setting owner to NULL.

SET @db_name = DATABASE();

-- Ensure owner_user_id can be null.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'owner_user_id'
              AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE merchants MODIFY COLUMN owner_user_id BIGINT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop existing FK if present.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND CONSTRAINT_NAME = 'fk_merchants_owner_pos_account'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'ALTER TABLE merchants DROP FOREIGN KEY fk_merchants_owner_pos_account',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Re-create FK with ON DELETE SET NULL.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'owner_user_id'
        ),
        'ALTER TABLE merchants ADD CONSTRAINT fk_merchants_owner_pos_account FOREIGN KEY (owner_user_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

