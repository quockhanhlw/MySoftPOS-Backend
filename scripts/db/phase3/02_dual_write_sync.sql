-- Phase 3 Step 2: Dual-read / Dual-write window
-- Create bidirectional triggers with guard variable.
-- NOTE: validate column names before running in production.

DROP TRIGGER IF EXISTS trg_users_ai_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_users_au_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_users_ad_sync_pos_accounts;
DROP TRIGGER IF EXISTS trg_pos_accounts_ai_sync_users;
DROP TRIGGER IF EXISTS trg_pos_accounts_au_sync_users;
DROP TRIGGER IF EXISTS trg_pos_accounts_ad_sync_users;

DELIMITER $$

CREATE TRIGGER trg_users_ai_sync_pos_accounts
AFTER INSERT ON users
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    REPLACE INTO pos_accounts VALUES (
      NEW.id, NEW.password_hash, NEW.role, NEW.full_name, NEW.phone, NEW.email,
      NEW.dob, NEW.gender, NEW.phone_verified, NEW.admin_id, NEW.merchant_id,
      NEW.branch_id, NEW.terminal_id, NEW.active, NEW.server_ip, NEW.server_port,
      NEW.failed_login_attempts, NEW.locked_until, NEW.created_at, NEW.last_active_at,
      NEW.forgot_password_code_hash, NEW.forgot_password_code_expires_at,
      NEW.forgot_password_code_verified_at, NEW.forgot_password_code_attempts
    );
    SET @sync_guard = NULL;
  END IF;
END$$

CREATE TRIGGER trg_users_au_sync_pos_accounts
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    REPLACE INTO pos_accounts VALUES (
      NEW.id, NEW.password_hash, NEW.role, NEW.full_name, NEW.phone, NEW.email,
      NEW.dob, NEW.gender, NEW.phone_verified, NEW.admin_id, NEW.merchant_id,
      NEW.branch_id, NEW.terminal_id, NEW.active, NEW.server_ip, NEW.server_port,
      NEW.failed_login_attempts, NEW.locked_until, NEW.created_at, NEW.last_active_at,
      NEW.forgot_password_code_hash, NEW.forgot_password_code_expires_at,
      NEW.forgot_password_code_verified_at, NEW.forgot_password_code_attempts
    );
    SET @sync_guard = NULL;
  END IF;
END$$

CREATE TRIGGER trg_users_ad_sync_pos_accounts
AFTER DELETE ON users
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    DELETE FROM pos_accounts WHERE id = OLD.id;
    SET @sync_guard = NULL;
  END IF;
END$$

CREATE TRIGGER trg_pos_accounts_ai_sync_users
AFTER INSERT ON pos_accounts
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    REPLACE INTO users VALUES (
      NEW.id, NEW.password_hash, NEW.role, NEW.full_name, NEW.phone, NEW.email,
      NEW.dob, NEW.gender, NEW.phone_verified, NEW.admin_id, NEW.merchant_id,
      NEW.branch_id, NEW.terminal_id, NEW.active, NEW.server_ip, NEW.server_port,
      NEW.failed_login_attempts, NEW.locked_until, NEW.created_at, NEW.last_active_at,
      NEW.forgot_password_code_hash, NEW.forgot_password_code_expires_at,
      NEW.forgot_password_code_verified_at, NEW.forgot_password_code_attempts
    );
    SET @sync_guard = NULL;
  END IF;
END$$

CREATE TRIGGER trg_pos_accounts_au_sync_users
AFTER UPDATE ON pos_accounts
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    REPLACE INTO users VALUES (
      NEW.id, NEW.password_hash, NEW.role, NEW.full_name, NEW.phone, NEW.email,
      NEW.dob, NEW.gender, NEW.phone_verified, NEW.admin_id, NEW.merchant_id,
      NEW.branch_id, NEW.terminal_id, NEW.active, NEW.server_ip, NEW.server_port,
      NEW.failed_login_attempts, NEW.locked_until, NEW.created_at, NEW.last_active_at,
      NEW.forgot_password_code_hash, NEW.forgot_password_code_expires_at,
      NEW.forgot_password_code_verified_at, NEW.forgot_password_code_attempts
    );
    SET @sync_guard = NULL;
  END IF;
END$$

CREATE TRIGGER trg_pos_accounts_ad_sync_users
AFTER DELETE ON pos_accounts
FOR EACH ROW
BEGIN
  IF @sync_guard IS NULL THEN
    SET @sync_guard = 1;
    DELETE FROM users WHERE id = OLD.id;
    SET @sync_guard = NULL;
  END IF;
END$$

DELIMITER ;

-- Consistency quick check
SELECT
  (SELECT COUNT(*) FROM users) AS users_count,
  (SELECT COUNT(*) FROM pos_accounts) AS pos_accounts_count;

