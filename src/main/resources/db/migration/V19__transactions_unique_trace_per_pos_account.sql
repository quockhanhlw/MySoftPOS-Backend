-- Flyway V19: allow same trace_number across different POS accounts
-- Root cause: global unique(trace_number) drops valid rows from other accounts.

SET @db_name = DATABASE();

-- Drop legacy global unique index if it exists.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'transactions'
              AND INDEX_NAME = 'uk_transactions_trace_number'
        ),
        'ALTER TABLE transactions DROP INDEX uk_transactions_trace_number',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add composite unique index (trace_number, pos_account_id) for scoped idempotency.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'transactions'
        )
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'transactions'
              AND INDEX_NAME = 'uk_transactions_trace_pos_account'
        ),
        'CREATE UNIQUE INDEX uk_transactions_trace_pos_account ON transactions (trace_number, pos_account_id)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

