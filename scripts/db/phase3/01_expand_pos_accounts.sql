-- Phase 3 Step 1: Expand
-- Create shadow table pos_accounts and backfill from users.

SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = @db_name AND table_name = 'pos_accounts'
    ),
    'SELECT 1',
    'CREATE TABLE pos_accounts LIKE users'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO pos_accounts
SELECT *
FROM users u
WHERE NOT EXISTS (
  SELECT 1 FROM pos_accounts p WHERE p.id = u.id
);

-- Sanity check
SELECT
  (SELECT COUNT(*) FROM users) AS users_count,
  (SELECT COUNT(*) FROM pos_accounts) AS pos_accounts_count;

