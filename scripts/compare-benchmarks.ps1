param([Parameter(Mandatory)][string]$BaselineJar, [Parameter(Mandatory)][string]$OptimizedJar, [string]$JavaExecutable='java', [string]$OutputDirectory='evidence/benchmarks', [string]$ProfileDirectory='profiles')
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force $OutputDirectory,$ProfileDirectory | Out-Null
foreach ($size in @(10000,100000)) {
    foreach ($trial in 1..3) {
        # Alternate ordering to reduce a simple ordering/warm-machine bias.
        $labels = if ($trial % 2 -eq 1) { @('baseline','optimized') } else { @('optimized','baseline') }
        foreach ($label in $labels) {
            $jar = if ($label -eq 'baseline') { $BaselineJar } else { $OptimizedJar }
            $output=Join-Path $OutputDirectory "$label-$size-$trial.json"
            if (Test-Path -LiteralPath $output) { throw "Preserve existing results before rerunning: $output" }
            $profile=Join-Path $ProfileDirectory "$label-$size-$trial.jfr"
            $sha=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLower()
            & $JavaExecutable -Xms256m -Xmx2g '-XX:+UseG1GC' "-XX:StartFlightRecording=filename=$profile,settings=profile,dumponexit=true" -cp $jar dev.searchforge.benchmarks.BenchmarkMain $size 350 $output "$label-jar-sha256:$sha"
            if ($LASTEXITCODE -ne 0) { throw 'Comparison run failed' }
        }
    }
}
