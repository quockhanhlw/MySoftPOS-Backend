[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$AdminUsername,

    [Parameter(Mandatory = $true)]
    [string]$AdminPassword,

    [Parameter(Mandatory = $true)]
    [long]$MerchantId,

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body,
        [hashtable]$Headers
    )

    if ($DryRun) {
        Write-Host "[DRY-RUN] $Method $Url"
        if ($null -ne $Body) {
            Write-Host ("[DRY-RUN] Body: " + ($Body | ConvertTo-Json -Depth 6 -Compress))
        }
        return $null
    }

    $params = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ContentType = "application/json"
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    return Invoke-RestMethod @params
}

$root = $BaseUrl.TrimEnd('/')
Write-Host "Base URL: $root"

# 1) Login
$loginBody = @{
    username = $AdminUsername
    password = $AdminPassword
}
$loginResp = Invoke-Api -Method "POST" -Url "$root/api/auth/login" -Body $loginBody -Headers @{}

$token = $null
if (-not $DryRun) {
    $token = $loginResp.accessToken
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "Login succeeded but accessToken is empty."
    }
}
$authHeaders = @{}
if (-not $DryRun) {
    $authHeaders["Authorization"] = "Bearer $token"
}

# 2) Get branches for merchant
$branches = Invoke-Api -Method "GET" -Url "$root/api/merchants/$MerchantId/branches" -Body $null -Headers $authHeaders
$branchId = $null
if (-not $DryRun) {
    if ($null -eq $branches -or $branches.Count -eq 0) {
        throw "No branches found for merchantId=$MerchantId"
    }
    $branchId = [long]$branches[0].id
    Write-Host "Selected branchId=$branchId"
}

# 3) Get accounts in selected branch
if ($DryRun) {
    Invoke-Api -Method "GET" -Url "$root/api/merchants/$MerchantId/branches/BRANCH_ID/accounts" -Body $null -Headers $authHeaders | Out-Null
} else {
    $accounts = Invoke-Api -Method "GET" -Url "$root/api/merchants/$MerchantId/branches/$branchId/accounts" -Body $null -Headers $authHeaders
    if ($null -eq $accounts -or $accounts.Count -eq 0) {
        throw "No accounts found for merchantId=$MerchantId, branchId=$branchId"
    }
    $selectedAccount = $accounts[0]
    $posAccountId = [long]$selectedAccount.id
    Write-Host "Selected posAccountId=$posAccountId"

    # 4) Find existing terminal for merchant
    $terminals = Invoke-Api -Method "GET" -Url "$root/api/terminals" -Body $null -Headers $authHeaders
    $merchantTerminals = @($terminals | Where-Object { $_.merchant -and $_.merchant.id -eq $MerchantId })

    if ($merchantTerminals.Count -eq 0) {
        $suffix = (Get-Random -Minimum 1000 -Maximum 9999)
        $terminalCode = "T$suffix".PadRight(8, '0').Substring(0, 8)
        $createBody = @{
            terminalCode = $terminalCode
            merchantId   = "$MerchantId"
            branchId     = "$branchId"
            posAccountId = "$posAccountId"
            serverIp     = "10.10.10.10"
            serverPort   = "8583"
        }
        $created = Invoke-Api -Method "POST" -Url "$root/api/terminals" -Body $createBody -Headers $authHeaders
        $terminalId = [long]$created.id
        Write-Host "Created terminal id=$terminalId code=$terminalCode"
    } else {
        $target = $merchantTerminals[0]
        $terminalId = [long]$target.id
        $updateBody = @{
            branchId     = "$branchId"
            posAccountId = "$posAccountId"
        }
        Invoke-Api -Method "PUT" -Url "$root/api/terminals/$terminalId" -Body $updateBody -Headers $authHeaders | Out-Null
        Write-Host "Updated terminal id=$terminalId with branch/account mapping"
    }

    # 5) Verify mapping
    $verifyTerminals = Invoke-Api -Method "GET" -Url "$root/api/terminals" -Body $null -Headers $authHeaders
    $mapped = $verifyTerminals | Where-Object {
        $_.id -eq $terminalId -and $_.branchId -eq $branchId -and $_.posAccountId -eq $posAccountId
    }

    if ($null -eq $mapped) {
        throw "Mapping verification failed for terminalId=$terminalId"
    }

    Write-Host "Smoke test passed: branch/account/terminal mapping is valid."
}

if ($DryRun) {
    Write-Host "Dry-run completed."
}

