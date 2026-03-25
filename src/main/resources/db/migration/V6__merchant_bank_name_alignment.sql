-- Flyway V6: add merchants.bank_name for DE43 bank segment source.
-- Idempotent for environments that may already have the column.

SET @db_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'merchants'
              AND COLUMN_NAME = 'bank_name'
        ),
        'SELECT 1',
        'ALTER TABLE merchants ADD COLUMN bank_name VARCHAR(22) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

