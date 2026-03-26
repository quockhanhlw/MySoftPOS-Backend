-- Flyway V9: explicit username column for pos_accounts login (phone+i semantics)
-- Keep phone for profile/backward compatibility, but authentication uses username.

SET @db_name = DATABASE();

-- 1) Add username column when missing.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'pos_accounts'
              AND COLUMN_NAME = 'username'
        ),
        'ALTER TABLE pos_accounts ADD COLUMN username VARCHAR(40) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Backfill username from phone for existing rows.
SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE pos_accounts
SET username = phone
WHERE (username IS NULL OR TRIM(username) = '')
  AND phone IS NOT NULL
  AND TRIM(phone) <> '';

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

-- 3) Enforce NOT NULL only after backfill.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'pos_accounts'
              AND COLUMN_NAME = 'username'
              AND IS_NULLABLE = 'YES'
        ),
        'ALTER TABLE pos_accounts MODIFY COLUMN username VARCHAR(40) NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Add unique index on username if missing.
SET @sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'pos_accounts'
              AND INDEX_NAME = 'uq_pos_accounts_username'
        ),
        'CREATE UNIQUE INDEX uq_pos_accounts_username ON pos_accounts(username)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

