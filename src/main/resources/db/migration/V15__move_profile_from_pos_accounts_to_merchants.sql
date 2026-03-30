-- Flyway V15: move profile/contact fields from pos_accounts to merchants
-- Goal:
-- - pos_accounts keeps only POS account data (username/password/role/...)
-- - merchants owns registration profile (full_name/phone/email/dob/gender)

SET @db_name = DATABASE();

-- 1) Expand merchants with profile columns (nullable for safe rollout)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'full_name'
        ),
        'ALTER TABLE merchants ADD COLUMN full_name VARCHAR(200) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'phone'
        ),
        'ALTER TABLE merchants ADD COLUMN phone VARCHAR(20) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'email'
        ),
        'ALTER TABLE merchants ADD COLUMN email VARCHAR(100) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'dob'
        ),
        'ALTER TABLE merchants ADD COLUMN dob VARCHAR(20) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants'
        )
        AND NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND COLUMN_NAME = 'gender'
        ),
        'ALTER TABLE merchants ADD COLUMN gender VARCHAR(20) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Backfill merchants profile from owner pos_account when legacy columns still exist
SET @has_owner_profile_cols = (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'full_name'
        )
         OR EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'phone'
        )
         OR EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'email'
        )
         OR EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'dob'
        )
         OR EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'gender'
        )
        THEN 1 ELSE 0 END
);

SET @sql = (
    SELECT IF(
        @has_owner_profile_cols = 1,
        'UPDATE merchants m JOIN pos_accounts p ON m.owner_user_id = p.id
         SET m.full_name = COALESCE(NULLIF(m.full_name, ''''), p.full_name, m.full_name),
             m.phone = COALESCE(NULLIF(m.phone, ''''), p.phone, m.phone),
             m.email = COALESCE(NULLIF(m.email, ''''), p.email, m.email),
             m.dob = COALESCE(NULLIF(m.dob, ''''), p.dob, m.dob),
             m.gender = COALESCE(NULLIF(m.gender, ''''), p.gender, m.gender)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Enforce uniqueness for merchant contact identifiers (nullable safe)
SET @sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND INDEX_NAME = 'uk_merchants_phone'
        ),
        'CREATE UNIQUE INDEX uk_merchants_phone ON merchants(phone)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'merchants' AND INDEX_NAME = 'uk_merchants_email'
        ),
        'CREATE UNIQUE INDEX uk_merchants_email ON merchants(email)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Drop profile columns from pos_accounts (account table cleanup)
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'full_name'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN full_name',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'phone'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN phone',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'email'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN email',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'dob'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN dob',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'pos_accounts' AND COLUMN_NAME = 'gender'
        ),
        'ALTER TABLE pos_accounts DROP COLUMN gender',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

