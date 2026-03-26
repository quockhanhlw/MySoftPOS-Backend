# Cutover Checklist - `users` -> `pos_accounts`

## 1) Pre-cutover (T-24h to T-1h)

- [ ] Confirm staging rehearsal passed with same SQL bundle
- [ ] Confirm backup snapshot + point-in-time restore available
- [ ] Confirm no pending Flyway migrations except approved Phase 3 set
- [ ] Confirm app/backend release with dual compatibility is already running
- [ ] Confirm rollback owner and DBA owner are online

## 2) Go/No-Go Gate (T-15m)

- [ ] Error rate baseline normal
- [ ] DB replication lag normal (if any)
- [ ] Last consistency check between `users` and `pos_accounts` = 0 mismatch
- [ ] Business owner approval recorded

## 3) Cutover Execution (T0)

1. [ ] Enable maintenance mode or block new writes briefly
2. [ ] Execute `03_cutover_swap.sql`
3. [ ] Run post-cutover verification queries
4. [ ] Disable maintenance mode

## 4) Post-cutover Verification (T+5m)

- [ ] Login ADMIN works
- [ ] Login USER/POS account works
- [ ] `/api/pos-accounts` CRUD works
- [ ] `/api/users` compatibility endpoint still works
- [ ] Merchant/branch/account mapping still intact
- [ ] Terminal mapping (`pos_account_id`) still valid

## 5) Rollback Triggers

Rollback immediately if any of the following happens within the stabilization window:

- [ ] Authentication failure spike > threshold
- [ ] API error spike > threshold
- [ ] FK violations or data mismatch appears
- [ ] Critical flows blocked (register/login/admin management)

## 6) Rollback Procedure

1. [ ] Re-enable maintenance mode
2. [ ] Execute `rollback_from_cutover.sql`
3. [ ] Run validation queries
4. [ ] Disable maintenance mode
5. [ ] Announce rollback and capture incident report

## 7) Cleanup Gate (after 1-2 stable versions)

- [ ] No traffic to `/api/users` compatibility path
- [ ] No old app versions requiring legacy table/view
- [ ] Execute cleanup script (`rollback_cleanup.sql` is no longer needed after this point)

