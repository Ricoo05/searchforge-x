param([int]$Port=8090)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$workspace=Split-Path (Split-Path $project -Parent) -Parent
$java=Get-Command java.exe -ErrorAction SilentlyContinue
$javaPath=if ($java) { $java.Source } elseif ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else {
    $jdk=Get-ChildItem (Join-Path $workspace 'work\toolchains') -Directory | Where-Object Name -Like 'jdk-21*' | Select-Object -First 1
    Join-Path $jdk.FullName 'bin\java.exe'
}
$jar=Join-Path $project 'target\searchforge-x-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Run scripts/build.ps1 first.' }
& $javaPath -Xmx256m -cp $jar dev.searchforge.distributed.ClusterMain (Join-Path $project 'data\local') $Port
if ($LASTEXITCODE -ne 0) { throw 'Cluster exited unexpectedly; see data/local logs.' }
