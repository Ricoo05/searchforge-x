param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$workspace = Split-Path (Split-Path $project -Parent) -Parent
$tools = Join-Path $workspace 'work\toolchains'
$oldJavaHome = $env:JAVA_HOME
try {
    if (-not $env:JAVA_HOME) {
        $jdk = Get-ChildItem -LiteralPath $tools -Directory -ErrorAction SilentlyContinue | Where-Object Name -Like 'jdk-21*' | Select-Object -First 1
        if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
    }
    $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    $mavenPath = if ($maven) { $maven.Source } else { Join-Path $tools 'apache-maven-3.9.9\bin\mvn.cmd' }
    if (-not (Test-Path -LiteralPath $mavenPath)) { throw 'Install Maven 3.9.x and Java 21, then run mvn clean verify.' }
    $repo = Join-Path $workspace 'work\maven-repository'
    $arguments = @('-f', (Join-Path $project 'pom.xml'), '--batch-mode', '--no-transfer-progress', "-Dmaven.repo.local=$repo", 'clean', 'verify')
    if ($SkipTests) { $arguments += '-DskipTests' }
    & $mavenPath @arguments
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally { $env:JAVA_HOME = $oldJavaHome }
