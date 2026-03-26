-- Final cleanup after 1-2 stable versions
-- Preconditions:
-- 1) no traffic to compatibility path /api/users
-- 2) rollback window is closed

SET @db_name = DATABASE();

DROP VIEW IF EXISTS users;

SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'users_legacy'),
    'DROP TABLE users_legacy',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Optional: rename constraints to final naming convention if needed.

