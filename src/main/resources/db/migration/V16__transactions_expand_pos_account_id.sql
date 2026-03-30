-- Flyway V16: expand/backfill for transactions user_id -> pos_account_id (non-downtime)
-- Phase: EXPAND + BACKFILL + DUAL-WRITE COMPATIBILITY
-- Strategy:
-- 1) Add transactions.pos_account_id (nullable)
-- 2) Backfill from legacy transactions.user_id
-- 3) Add index + FK to pos_accounts(id)
-- 4) Add dual-write triggers to keep user_id and pos_account_id in sync during rolling upgrades

SET @db_name = DATABASE();

-- 1) Expand: add new canonical column
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        ),
        'ALTER TABLE transactions ADD COLUMN pos_account_id BIGINT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Backfill canonical column from legacy column
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'user_id'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        ),
        'UPDATE transactions SET pos_account_id = user_id WHERE pos_account_id IS NULL AND user_id IS NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2b) Reverse backfill (safety for environments already writing pos_account_id)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'user_id'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        ),
        'UPDATE transactions SET user_id = pos_account_id WHERE user_id IS NULL AND pos_account_id IS NOT NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Add index on canonical column
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_transactions_pos_account_id'
        ),
        'CREATE INDEX idx_transactions_pos_account_id ON transactions (pos_account_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3b) Add FK from transactions.pos_account_id -> pos_accounts.id (idempotent)
SET @fk_exists = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'transactions'
              AND COLUMN_NAME = 'pos_account_id'
              AND REFERENCED_TABLE_NAME = 'pos_accounts'
              AND REFERENCED_COLUMN_NAME = 'id'
        ) THEN 1 ELSE 0 END
);
SET @sql = (
    SELECT IF(
        @fk_exists = 0
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        ),
        'ALTER TABLE transactions ADD CONSTRAINT fk_transactions_pos_account FOREIGN KEY (pos_account_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Dual-write triggers for zero-downtime rolling upgrades
DROP TRIGGER IF EXISTS trg_transactions_sync_pos_account_bi;
DROP TRIGGER IF EXISTS trg_transactions_sync_pos_account_bu;

DELIMITER $$
CREATE TRIGGER trg_transactions_sync_pos_account_bi
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    IF NEW.pos_account_id IS NULL AND NEW.user_id IS NOT NULL THEN
        SET NEW.pos_account_id = NEW.user_id;
    END IF;

    IF NEW.user_id IS NULL AND NEW.pos_account_id IS NOT NULL THEN
        SET NEW.user_id = NEW.pos_account_id;
    END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_transactions_sync_pos_account_bu
BEFORE UPDATE ON transactions
FOR EACH ROW
BEGIN
    IF NEW.pos_account_id IS NULL AND NEW.user_id IS NOT NULL THEN
        SET NEW.pos_account_id = NEW.user_id;
    END IF;

    IF NEW.user_id IS NULL AND NEW.pos_account_id IS NOT NULL THEN
        SET NEW.user_id = NEW.pos_account_id;
    END IF;
END $$
DELIMITER ;

