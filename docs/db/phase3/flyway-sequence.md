# Flyway Sequence - Phase 3 Rename

## Objective

Rename physical table `users` to `pos_accounts` with compatibility preserved for older app/backend versions.

## Legacy Sunset Policy

- Canonical API endpoint: `/api/pos-accounts`.
- `/api/users` compatibility route has been removed after cutover.
- Removal completed after compatibility window closed.

## Endpoint Documentation Policy

- New API docs and examples must use canonical naming (`pos-account`, `/api/pos-accounts`).
- Legacy `/api/users` may be referenced only in historical migration notes.

## Proposed Sequence

### V8 - Expand (no behavior change)

- Create shadow table `pos_accounts` with same schema as `users`
- Backfill data from `users` into `pos_accounts`
- Add indexes/constraints equivalent to `users`
- No endpoint switch yet

### V9 - Dual-read / Dual-write window

- Add sync triggers `users -> pos_accounts` and `pos_accounts -> users`
- Keep both API routes alive temporarily (`/api/users`, `/api/pos-accounts`)
- Observe metrics and consistency checks for at least 1 release cycle

### V10 - Cutover

- Freeze write traffic briefly
- Final sync `users -> pos_accounts`
- Drop dual-write triggers
- Rebind foreign keys from `users.id` to `pos_accounts.id`
- Rename old table to `users_legacy`
- Create compatibility view `users` over `pos_accounts` for old readers/writers

### V11 - Cleanup (after 1-2 stable versions)

- Remove compatibility view `users`
- Remove `users_legacy` table
- Drop any temporary objects

## Phase 2 - transactions.user_id -> transactions.pos_account_id

### V16 - Expand + Backfill

- Add nullable `transactions.pos_account_id`
- Backfill from `transactions.user_id`
- Add index + FK to `pos_accounts(id)`
- Add dual-write triggers to sync `user_id` <-> `pos_account_id`

### V17 - Cutover (compatibility retained)

- Switch backend mapping to `transactions.pos_account_id`
- Final reconciliation/backfill for mismatched rows
- Keep compatibility triggers during stabilization window

### V18 - Cleanup (planned)

- Remove compatibility triggers
- Drop legacy `transactions.user_id`
- Remove legacy routes/fields after compatibility window closes

## Notes

- Keep `User` entity/table mapping in code behind feature flag until V10 is complete.
- Promote each step only after staging rehearsal and consistency validation.
- If your environment has different FK names, resolve from `information_schema` before execution.

