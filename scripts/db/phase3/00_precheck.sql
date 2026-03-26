-- Phase 3 precheck (MySQL)
-- Run before any rename/cutover.

SELECT DATABASE() AS current_db;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('users', 'pos_accounts', 'users_legacy');

SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'users'
ORDER BY ordinal_position;

SELECT constraint_name, table_name, referenced_table_name
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE()
  AND (referenced_table_name = 'users' OR table_name = 'users');

SELECT COUNT(*) AS users_count FROM users;

-- Optional: check existing shadow table consistency if present
SELECT
  (SELECT COUNT(*) FROM users) AS users_count,
  (SELECT COUNT(*) FROM pos_accounts) AS pos_accounts_count
WHERE EXISTS (
  SELECT 1 FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'pos_accounts'
);

