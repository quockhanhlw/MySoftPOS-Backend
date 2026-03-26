# API Smoke Test (Render)

Quick smoke test for:

- branch listing (`/api/merchants/{merchantId}/branches`)
- branch account listing (`/api/merchants/{merchantId}/branches/{branchId}/accounts`)
- terminal mapping (`branchId`, `posAccountId`)

## Script

- `render-branch-terminal-smoke.ps1`

## Dry-run (no network calls)

```powershell
Set-Location "C:\Users\Laptop\AndroidStudioProjects\MySoftPOS\mysoftpos-backend"
.\scripts\smoke\render-branch-terminal-smoke.ps1 `
  -BaseUrl "https://mysoftpos-backend.onrender.com" `
  -AdminUsername "admin_phone_or_email" `
  -AdminPassword "admin_password" `
  -MerchantId 1 `
  -DryRun
```

## Real run

```powershell
Set-Location "C:\Users\Laptop\AndroidStudioProjects\MySoftPOS\mysoftpos-backend"
.\scripts\smoke\render-branch-terminal-smoke.ps1 `
  -BaseUrl "https://mysoftpos-backend.onrender.com" `
  -AdminUsername "admin_phone_or_email" `
  -AdminPassword "admin_password" `
  -MerchantId 1
```

## Expected result

- Script prints selected branch/account IDs
- Script creates or updates one terminal mapping
- Script prints `Smoke test passed: branch/account/terminal mapping is valid.`

