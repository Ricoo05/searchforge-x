$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$workspace=Split-Path (Split-Path $project -Parent) -Parent
$npm=Get-Command npm.cmd -ErrorAction SilentlyContinue
if ($npm) {
    & $npm.Source --prefix (Join-Path $project 'web') ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
    & $npm.Source --prefix (Join-Path $project 'web') run build
} else {
    $npmCli=Join-Path $workspace 'work\tools\package\bin\npm-cli.js'
    if (-not (Test-Path -LiteralPath $npmCli)) { throw 'Install Node.js 24 with npm; see README.md.' }
    & node $npmCli --prefix (Join-Path $project 'web') ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
    & node $npmCli --prefix (Join-Path $project 'web') run build
}
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
& (Join-Path $PSScriptRoot 'verify-local.ps1')
