-- Precheck before applying Flyway migration that adds:
--   - UNIQUE (owner_user_id)
--   - FK owner_user_id -> users.id
--   - indexes on admin_id and business_type
--
-- Run this on production/staging first. If any count > 0, clean data before deploy.

-- 1) Null owner_user_id (allowed technically, but should be reviewed)
SELECT COUNT(*) AS null_owner_user_id_count
FROM merchants
WHERE owner_user_id IS NULL;

-- 2) Duplicate non-null owner_user_id (will break UNIQUE index creation)
SELECT owner_user_id, COUNT(*) AS row_count
FROM merchants
WHERE owner_user_id IS NOT NULL
GROUP BY owner_user_id
HAVING COUNT(*) > 1;

-- 3) Orphan owner_user_id (will break FK creation)
SELECT m.id AS merchant_id, m.owner_user_id
FROM merchants m
LEFT JOIN users u ON u.id = m.owner_user_id
WHERE m.owner_user_id IS NOT NULL
  AND u.id IS NULL;

-- 4) Quick profile quality checks
SELECT COUNT(*) AS missing_merchant_code
FROM merchants
WHERE merchant_code IS NULL OR TRIM(merchant_code) = '';

SELECT COUNT(*) AS missing_merchant_name
FROM merchants
WHERE merchant_name IS NULL OR TRIM(merchant_name) = '';

-- 5) Optional inspect list (limit)
SELECT id, merchant_code, merchant_name, owner_user_id, admin_id, business_type
FROM merchants
ORDER BY id DESC
LIMIT 100;

-- -------------------------------------------------------------------
-- Optional cleanup templates (review manually before executing)
-- -------------------------------------------------------------------
-- Keep the latest merchant row per owner_user_id and nullify the others:
-- UPDATE merchants m
-- JOIN (
--   SELECT owner_user_id, MAX(id) AS keep_id
--   FROM merchants
--   WHERE owner_user_id IS NOT NULL
--   GROUP BY owner_user_id
-- ) k ON k.owner_user_id = m.owner_user_id
-- SET m.owner_user_id = NULL
-- WHERE m.owner_user_id IS NOT NULL
--   AND m.id <> k.keep_id;

-- Nullify orphan owner_user_id rows:
-- UPDATE merchants m
-- LEFT JOIN users u ON u.id = m.owner_user_id
-- SET m.owner_user_id = NULL
-- WHERE m.owner_user_id IS NOT NULL
--   AND u.id IS NULL;

