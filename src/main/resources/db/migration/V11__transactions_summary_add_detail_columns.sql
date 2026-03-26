ALTER TABLE transactions_summary ADD COLUMN request_hex TEXT NULL;
ALTER TABLE transactions_summary ADD COLUMN response_hex TEXT NULL;
ALTER TABLE transactions_summary ADD COLUMN processing_code VARCHAR(6) NULL;
ALTER TABLE transactions_summary ADD COLUMN currency_code VARCHAR(3) NULL;
ALTER TABLE transactions_summary ADD COLUMN rrn VARCHAR(12) NULL;
ALTER TABLE transactions_summary ADD COLUMN owner_username VARCHAR(64) NULL;

