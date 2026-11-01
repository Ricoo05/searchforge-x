param([ValidateSet('baseline','optimized')][string]$Label = 'optimized', [string]$JavaExecutable = 'java', [int]$Samples = 350)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $project 'target\searchforge-x-0.1.0-SNAPSHOT.jar'
$revision = git -C $project rev-parse HEAD
foreach ($size in @(10000,100000)) {
    foreach ($trial in 1..3) {
        $output = Join-Path $project "evidence\benchmarks\$Label-$size-$trial.json"
        if (Test-Path -LiteralPath $output) { throw "Preserve previous results first: $output already exists" }
        $profile = Join-Path ([IO.Path]::GetTempPath()) "searchforge-$Label-$size-$trial.jfr"
        & $JavaExecutable -Xms256m -Xmx2g '-XX:+UseG1GC' "-XX:StartFlightRecording=filename=$profile,settings=profile,dumponexit=true" -cp $jar dev.searchforge.benchmarks.BenchmarkMain $size $Samples $output "$revision-$Label"
        if ($LASTEXITCODE -ne 0) { throw 'Benchmark failed' }
    }
}
