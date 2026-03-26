-- Phase 3 Step 3: Cutover
-- Goal: make pos_accounts the physical source of truth while keeping compatibility view `users`.

-- 1) Final sync from users -> pos_accounts (safety)
REPLACE INTO pos_accounts SELECT * FROM users;

-- 2) Stop dual-write triggers
DROP TRIGGER IF EXISTS trg_users_ai_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_users_au_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_users_ad_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_pos_accounts_ai_sync_users;
DROP TRIGGER IF EXISTS trg_pos_accounts_au_sync_users;
DROP TRIGGER IF EXISTS trg_pos_accounts_ad_sync_users;

-- 3) Rename users table to legacy backup
SET @db_name = DATABASE();
SET @sql = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @db_name AND table_name = 'users_legacy'),
    'SELECT 1',
    'RENAME TABLE users TO users_legacy'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Rebind foreign keys to pos_accounts (known relations in this project)
-- merchants.owner_user_id -> pos_accounts.id
SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE constraint_schema = @db_name
        AND table_name = 'merchants'
        AND constraint_name = 'fk_merchants_owner_user'
    ),
    'ALTER TABLE merchants DROP FOREIGN KEY fk_merchants_owner_user',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'merchants'
        AND column_name = 'owner_user_id'
    ),
    'ALTER TABLE merchants ADD CONSTRAINT fk_merchants_owner_pos_account FOREIGN KEY (owner_user_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- terminals.pos_account_id -> pos_accounts.id
SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE constraint_schema = @db_name
        AND table_name = 'terminals'
        AND constraint_name = 'fk_terminals_pos_account'
    ),
    'ALTER TABLE terminals DROP FOREIGN KEY fk_terminals_pos_account',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'terminals'
        AND column_name = 'pos_account_id'
    ),
    'ALTER TABLE terminals ADD CONSTRAINT fk_terminals_pos_account FOREIGN KEY (pos_account_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- transactions_summary.user_id -> pos_accounts.id (if table/column exists)
SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.table_constraints
      WHERE constraint_schema = @db_name
        AND table_name = 'transactions_summary'
        AND constraint_name = 'fk_txn_summary_user'
    ),
    'ALTER TABLE transactions_summary DROP FOREIGN KEY fk_txn_summary_user',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'transactions_summary'
        AND column_name = 'user_id'
    ),
    'ALTER TABLE transactions_summary ADD CONSTRAINT fk_txn_summary_pos_account FOREIGN KEY (user_id) REFERENCES pos_accounts(id) ON UPDATE RESTRICT ON DELETE SET NULL',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Compatibility view for old code path (/api/users)
DROP VIEW IF EXISTS users;
CREATE VIEW users AS SELECT * FROM pos_accounts;

-- 6) Verify
SELECT
  (SELECT COUNT(*) FROM pos_accounts) AS pos_accounts_count,
  (SELECT COUNT(*) FROM users_legacy) AS users_legacy_count;

