-- Flyway V17: cutover to transactions.pos_account_id as source of truth
-- Phase: CUTOVER (compatibility retained)
-- Notes:
-- - App code now reads/writes pos_account_id.
-- - user_id remains temporarily for rollback compatibility and old readers.
-- - Triggers are kept to mirror values during stabilization window.

SET @db_name = DATABASE();

-- Final reconciliation before cutover stabilization.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'pos_account_id'
        )
        AND EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'user_id'
        ),
        'UPDATE transactions SET pos_account_id = COALESCE(pos_account_id, user_id), user_id = COALESCE(pos_account_id, user_id) WHERE (pos_account_id IS NULL AND user_id IS NOT NULL) OR (user_id IS NULL AND pos_account_id IS NOT NULL) OR (pos_account_id <> user_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure cutover triggers exist and prioritize canonical pos_account_id while still tolerating legacy writes.
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

    IF NEW.pos_account_id IS NOT NULL THEN
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

    IF NEW.pos_account_id IS NOT NULL THEN
        SET NEW.user_id = NEW.pos_account_id;
    END IF;
END $$
DELIMITER ;

