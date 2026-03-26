-- Flyway V8: physical rename users -> pos_accounts with backward compatibility.
-- Safe for environments where rename may already be applied.

SET @db_name = DATABASE();

-- Rename users table to pos_accounts when needed.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'users'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts'
        ),
        'RENAME TABLE users TO pos_accounts',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Rebind merchant owner FK to pos_accounts.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND CONSTRAINT_NAME = 'fk_merchants_owner_user'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'ALTER TABLE merchants DROP FOREIGN KEY fk_merchants_owner_user',
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
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'owner_user_id'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND CONSTRAINT_NAME = 'fk_merchants_owner_pos_account'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'ALTER TABLE merchants ADD CONSTRAINT fk_merchants_owner_pos_account FOREIGN KEY (owner_user_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE RESTRICT',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Rebind terminal -> pos account FK.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND CONSTRAINT_NAME = 'fk_terminals_pos_account'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'ALTER TABLE terminals DROP FOREIGN KEY fk_terminals_pos_account',
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
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND COLUMN_NAME = 'pos_account_id'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND CONSTRAINT_NAME = 'fk_terminals_pos_account'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'ALTER TABLE terminals ADD CONSTRAINT fk_terminals_pos_account FOREIGN KEY (pos_account_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- No legacy users compatibility view in this phase: database must expose pos_accounts only.

