-- Flyway V18: cleanup after pos_account_id cutover
-- Phase: CLEANUP (drop compatibility triggers + legacy user_id)

SET @db_name = DATABASE();

-- 1) Drop compatibility triggers from V16/V17 if they still exist.
DROP TRIGGER IF EXISTS trg_transactions_sync_pos_account_bi;
DROP TRIGGER IF EXISTS trg_transactions_sync_pos_account_bu;

-- 2) Drop FK(s) on transactions.user_id (idempotent across envs with different FK names).
SET @drop_fk_sql = (
    SELECT IFNULL(
        GROUP_CONCAT(
            CONCAT('ALTER TABLE transactions DROP FOREIGN KEY `', kcu.CONSTRAINT_NAME, '`')
            SEPARATOR '; '
        ),
        'SELECT 1'
    )
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = @db_name
      AND kcu.TABLE_NAME = 'transactions'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
);
PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Drop index(es) on transactions.user_id except PRIMARY (idempotent).
SET @drop_idx_sql = (
    SELECT IFNULL(
        GROUP_CONCAT(
            CONCAT('DROP INDEX `', s.INDEX_NAME, '` ON transactions')
            SEPARATOR '; '
        ),
        'SELECT 1'
    )
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = @db_name
      AND s.TABLE_NAME = 'transactions'
      AND s.COLUMN_NAME = 'user_id'
      AND s.INDEX_NAME <> 'PRIMARY'
);
PREPARE stmt FROM @drop_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Drop legacy transactions.user_id column if present.
SET @drop_user_id_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS c
            WHERE c.TABLE_SCHEMA = @db_name
              AND c.TABLE_NAME = 'transactions'
              AND c.COLUMN_NAME = 'user_id'
        ),
        'ALTER TABLE transactions DROP COLUMN user_id',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

