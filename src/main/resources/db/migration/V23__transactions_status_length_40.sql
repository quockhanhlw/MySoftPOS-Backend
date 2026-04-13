-- Flyway V23: allow longer transaction status values (timeout/reversal statuses)
-- Prevents sync insert failures when status exceeds VARCHAR(20).

SET @db_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'transactions'
              AND COLUMN_NAME = 'status'
        ),
        'ALTER TABLE transactions MODIFY COLUMN status VARCHAR(40) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

