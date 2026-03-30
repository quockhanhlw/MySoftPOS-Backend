SET @db_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'test_cases' AND COLUMN_NAME = 'track2'
        ),
        'ALTER TABLE test_cases ADD COLUMN track2 VARCHAR(40) DEFAULT NULL',
        'SELECT 1'
    )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;