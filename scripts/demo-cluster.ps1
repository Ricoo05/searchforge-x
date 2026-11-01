param([string]$JavaExecutable)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$workspace = Split-Path (Split-Path $project -Parent) -Parent
if (-not $JavaExecutable) {
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($java) { $JavaExecutable = $java.Source }
    elseif ($env:JAVA_HOME) { $JavaExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe' }
    else {
        $jdk = Get-ChildItem (Join-Path $workspace 'work\toolchains') -Directory | Where-Object Name -Like 'jdk-21*' | Select-Object -First 1
        $JavaExecutable = Join-Path $jdk.FullName 'bin\java.exe'
    }
}
$jar = Join-Path $project 'target\searchforge-x-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build with scripts/verify-local.ps1 first.' }
$run = Join-Path $workspace ('work\cluster-demo-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $run | Out-Null
$processes = [Collections.Generic.List[Diagnostics.Process]]::new()
function Start-Node([string]$Name, [string]$Class, [string[]]$NodeArguments) {
    $arguments = @('-Xmx256m', '-cp', ('"' + $jar + '"'), $Class) + $NodeArguments
    $process = Start-Process -FilePath $JavaExecutable -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $run "$Name.log") -RedirectStandardError (Join-Path $run "$Name.err")
    $processes.Add($process)
    $ready = Join-Path $run "$Name.ready"
    $deadline = [DateTime]::UtcNow.AddSeconds(20)
    do {
        if ($process.HasExited) { throw "$Name exited. Logs: $run" }
        if (Test-Path -LiteralPath $ready) {
            $port = (Get-Content -LiteralPath $ready -Raw).Trim()
            if ($port) { return "http://127.0.0.1:$port" }
        }
        Start-Sleep -Milliseconds 100
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Name startup timeout. Logs: $run"
}
try {
    $urls = @()
    for ($shard = 0; $shard -lt 3; $shard++) {
        $data = Join-Path $run "data$shard"; $ready = Join-Path $run "worker$shard.ready"
        $urls += Start-Node "worker$shard" 'dev.searchforge.distributed.WorkerMain' @("$shard", '3', '0', ('"' + $data + '"'), ('"' + $ready + '"'))
    }
    $ready = Join-Path $run 'coordinator.ready'
    $coordinator = Start-Node 'coordinator' 'dev.searchforge.distributed.CoordinatorMain' (@('0', ('"' + $ready + '"')) + $urls)
    $documents = @(@{ id=1; text='Distributed search uses an inverted index.'; metadata=@{} }, @{ id=2; text='An inverted index maps terms to documents.'; metadata=@{} }, @{ id=3; text='Storage recovery requires careful testing.'; metadata=@{} })
    for ($shard = 0; $shard -lt 3; $shard++) {
        $batch = @{upserts=@($documents | Where-Object { $_.id % 3 -eq $shard }); deletes=@()} | ConvertTo-Json -Depth 8
        Invoke-RestMethod -Uri ($urls[$shard] + '/ingest') -Method Post -ContentType 'application/json' -Body $batch | Out-Null
    }
    $body = @{query='"inverted index"'; offset=0; limit=10; timeoutMillis=5000} | ConvertTo-Json
    $result = Invoke-RestMethod -Uri ($coordinator + '/search') -Method Post -ContentType 'application/json' -Body $body
    $result | ConvertTo-Json -Depth 10
    if ($result.status -ne 'COMPLETE' -or $result.totalHits -ne 2) { throw 'Unexpected cluster demo results' }
    Write-Output "Four-process demo passed. Data and logs retained at $run"
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) { $process.Kill(); $process.WaitForExit() }
        $process.Dispose()
    }
}
