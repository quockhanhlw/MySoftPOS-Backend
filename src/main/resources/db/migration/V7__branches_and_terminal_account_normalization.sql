-- Flyway V7: phase-2 normalization for branch model and terminal-account relationship.
-- Additive + idempotent migration, safe for production rollout.

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS branches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    branch_code VARCHAR(32) NOT NULL,
    branch_name VARCHAR(100) NULL,
    branch_address VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_branches_merchant_code UNIQUE (merchant_id, branch_code)
);

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'branches'
              AND CONSTRAINT_NAME = 'fk_branches_merchant'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE branches ADD CONSTRAINT fk_branches_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON UPDATE RESTRICT ON DELETE CASCADE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'branch_id'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN branch_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND COLUMN_NAME = 'branch_id'
        ),
        'SELECT 1',
        'ALTER TABLE terminals ADD COLUMN branch_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND COLUMN_NAME = 'pos_account_id'
        ),
        'SELECT 1',
        'ALTER TABLE terminals ADD COLUMN pos_account_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND INDEX_NAME = 'idx_users_branch_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_users_branch_id ON users (branch_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND INDEX_NAME = 'idx_terminals_branch_id'
        ),
        'SELECT 1',
        'CREATE INDEX idx_terminals_branch_id ON terminals (branch_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND INDEX_NAME = 'uk_terminals_pos_account_id'
        ),
        'SELECT 1',
        'CREATE UNIQUE INDEX uk_terminals_pos_account_id ON terminals (pos_account_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'users'
              AND CONSTRAINT_NAME = 'fk_users_branch'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON UPDATE RESTRICT ON DELETE SET NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND CONSTRAINT_NAME = 'fk_terminals_branch'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE terminals ADD CONSTRAINT fk_terminals_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON UPDATE RESTRICT ON DELETE SET NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = @db_name
              AND TABLE_NAME = 'terminals'
              AND CONSTRAINT_NAME = 'fk_terminals_pos_account'
              AND CONSTRAINT_TYPE = 'FOREIGN KEY'
        ),
        'SELECT 1',
        'ALTER TABLE terminals ADD CONSTRAINT fk_terminals_pos_account FOREIGN KEY (pos_account_id) REFERENCES users(id) ON UPDATE RESTRICT ON DELETE SET NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO branches (merchant_id, branch_code, branch_name, branch_address)
SELECT m.id,
       'MAIN',
       COALESCE(NULLIF(TRIM(m.merchant_name), ''), CONCAT('BRANCH-', m.id)),
       m.store_address
FROM merchants m
LEFT JOIN branches b
       ON b.merchant_id = m.id
      AND b.branch_code = 'MAIN'
WHERE b.id IS NULL;

UPDATE users u
JOIN branches b
  ON b.merchant_id = u.merchant_id
 AND b.branch_code = 'MAIN'
SET u.branch_id = b.id
WHERE u.branch_id IS NULL
  AND u.merchant_id IS NOT NULL;

UPDATE terminals t
JOIN branches b
  ON b.merchant_id = t.merchant_id
 AND b.branch_code = 'MAIN'
SET t.branch_id = b.id
WHERE t.branch_id IS NULL;

UPDATE terminals t
JOIN users u
  ON u.terminal_id = t.terminal_code
 AND u.merchant_id = t.merchant_id
SET t.pos_account_id = u.id
WHERE t.pos_account_id IS NULL;

