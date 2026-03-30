-- Manual backfill helper (idempotent)
-- Use this when database already includes merchants profile columns but rows are still empty.

UPDATE merchants m
JOIN pos_accounts p ON m.owner_user_id = p.id
SET m.full_name = COALESCE(NULLIF(m.full_name, ''), p.full_name, m.full_name),
    m.phone = COALESCE(NULLIF(m.phone, ''), p.phone, m.phone),
    m.email = COALESCE(NULLIF(m.email, ''), p.email, m.email),
    m.dob = COALESCE(NULLIF(m.dob, ''), p.dob, m.dob),
    m.gender = COALESCE(NULLIF(m.gender, ''), p.gender, m.gender);

