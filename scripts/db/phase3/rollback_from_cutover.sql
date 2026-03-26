-- Rollback script for Phase 3 cutover.
-- Restores physical `users` table as primary source if post-cutover validation fails.

SET @db_name = DATABASE();

-- 1) Remove compatibility view if present
DROP VIEW IF EXISTS users;

-- 2) Sync latest writes from pos_accounts back to users_legacy
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'users_legacy')
    AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'pos_accounts'),
    'REPLACE INTO users_legacy SELECT * FROM pos_accounts',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Rebind FK merchants back to users_legacy
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_schema = @db_name
              AND table_name = 'merchants'
              AND constraint_name = 'fk_merchants_owner_pos_account'),
    'ALTER TABLE merchants DROP FOREIGN KEY fk_merchants_owner_pos_account',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name
              AND table_name = 'merchants'
              AND column_name = 'owner_user_id'),
    'ALTER TABLE merchants ADD CONSTRAINT fk_merchants_owner_user FOREIGN KEY (owner_user_id) REFERENCES users_legacy(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Rebind FK terminals back to users_legacy
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_schema = @db_name
              AND table_name = 'terminals'
              AND constraint_name = 'fk_terminals_pos_account'),
    'ALTER TABLE terminals DROP FOREIGN KEY fk_terminals_pos_account',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name
              AND table_name = 'terminals'
              AND column_name = 'pos_account_id'),
    'ALTER TABLE terminals ADD CONSTRAINT fk_terminals_pos_account FOREIGN KEY (pos_account_id) REFERENCES users_legacy(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Rebind transactions_summary if present
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_schema = @db_name
              AND table_name = 'transactions_summary'
              AND constraint_name = 'fk_txn_summary_pos_account'),
    'ALTER TABLE transactions_summary DROP FOREIGN KEY fk_txn_summary_pos_account',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name
              AND table_name = 'transactions_summary'
              AND column_name = 'user_id'),
    'ALTER TABLE transactions_summary ADD CONSTRAINT fk_txn_summary_user FOREIGN KEY (user_id) REFERENCES users_legacy(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6) Rename users_legacy back to users
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'users_legacy')
    AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'users'),
    'RENAME TABLE users_legacy TO users',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) AS users_count FROM users;

