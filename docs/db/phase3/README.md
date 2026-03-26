# Phase 3 - Physical Rename `users` -> `pos_accounts`

This folder contains the **detailed execution plan** for Phase 3. It is intentionally kept outside Flyway active migrations so it can be reviewed, rehearsed, and promoted safely.

## Scope

- Physical table rename target: `users` -> `pos_accounts`
- Keep backward compatibility for 1-2 app versions
- Support dual-read/dual-write transition window
- Provide cutover and rollback scripts/checklists

## Safety Rules

1. Do not copy these SQL files into `src/main/resources/db/migration` until go-live approval.
2. Run precheck and rehearsal on staging with production-like data first.
3. Freeze schema changes during cutover window.
4. Keep DB snapshot backup and rollback operator online during cutover.

## Files

- `flyway-sequence.md`: step-by-step migration sequence and promotion strategy
- `cutover-checklist.md`: operator runbook, go/no-go, and rollback decision gates
- `../../scripts/db/phase3/*.sql`: SQL drafts for precheck, dual window, cutover, rollback

## Promotion Workflow

1. Rehearse scripts from `scripts/db/phase3` on staging
2. Lock final SQL after validation
3. Promote SQL into Flyway `V8+` files in `src/main/resources/db/migration`
4. Deploy backend with dual compatibility enabled
5. Execute production cutover with checklist

