CREATE TABLE IF NOT EXISTS cards (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pan_masked VARCHAR(25) NOT NULL,
  bin VARCHAR(12) NULL,
  last4 VARCHAR(4) NULL,
  scheme VARCHAR(30) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cards_pan_masked (pan_masked)
);

