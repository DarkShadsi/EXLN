# EXLN test runner.
# Run from the project root (the folder that contains src and tests):
#     powershell -ExecutionPolicy Bypass -File .\tests\run_tests.ps1
param([string]$OutDir = "out")

Write-Host "Compiling..."
$sources = (Get-ChildItem -Path "src\exln" -Filter "*.java").FullName
javac -d $OutDir $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compile failed." -ForegroundColor Red
    exit 1
}

$script:passed = 0
$script:failed = 0

function ConvertTo-UnixText([string]$text) {
    return ($text -replace "`r`n", "`n").TrimEnd()
}

function Invoke-Exln([string]$path) {
    $output = & java -cp $OutDir exln.Main $path 2>&1 | Out-String
    return @{ Output = $output; Code = $LASTEXITCODE }
}

function Write-Result([string]$name, [string[]]$problems) {
    if ($problems.Count -eq 0) {
        Write-Host "PASS  $name" -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host "FAIL  $name" -ForegroundColor Red
        foreach ($p in $problems) { Write-Host "        $p" }
        $script:failed++
    }
}

# The whole output must match the .expected file line by line, and the exit code must be 0.
function Test-Exact([string]$dir) {
    foreach ($file in Get-ChildItem $dir -Filter "*.exln") {
        $expectedFile = [System.IO.Path]::ChangeExtension($file.FullName, ".expected")
        $problems = @()
        if (-not (Test-Path $expectedFile)) {
            $problems += "missing .expected file"
        } else {
            $result = Invoke-Exln $file.FullName
            $actual = (ConvertTo-UnixText $result.Output) -split "`n"
            $expected = (ConvertTo-UnixText (Get-Content $expectedFile -Raw)) -split "`n"
            if ($result.Code -ne 0) { $problems += "exit code is $($result.Code), expected 0" }
            $count = [Math]::Max($actual.Count, $expected.Count)
            for ($i = 0; $i -lt $count; $i++) {
                $a = "<no line>"
                $e = "<no line>"
                if ($i -lt $actual.Count) { $a = $actual[$i] }
                if ($i -lt $expected.Count) { $e = $expected[$i] }
                if ($a -cne $e) { $problems += "line $($i + 1): expected '$e' but got '$a'" }
            }
        }
        Write-Result "$dir\$($file.Name)" $problems
    }
}

# Each line of the .expected file is a phrase that must appear in the output (ignoring case).
# A line starting with ! is a phrase that must NOT appear. The exit code must match.
function Test-Errors([string]$dir, [int]$expectedCode) {
    foreach ($file in Get-ChildItem $dir -Filter "*.exln") {
        $expectedFile = [System.IO.Path]::ChangeExtension($file.FullName, ".expected")
        $problems = @()
        $result = Invoke-Exln $file.FullName
        $output = $result.Output.ToLower()
        if ($result.Code -ne $expectedCode) {
            $problems += "exit code is $($result.Code), expected $expectedCode"
        }
        foreach ($line in Get-Content $expectedFile) {
            $phrase = $line.Trim()
            if ($phrase -eq "") { continue }
            if ($phrase.StartsWith("!")) {
                $unwanted = $phrase.Substring(1).ToLower()
                if ($output.Contains($unwanted)) { $problems += "should not contain: $unwanted" }
            } elseif (-not $output.Contains($phrase.ToLower())) {
                $problems += "missing: $phrase"
            }
        }
        Write-Result "$dir\$($file.Name)" $problems
    }
}

# The program must run to the end with exit code 0 and no error message.
function Test-Clean([string]$path) {
    $result = Invoke-Exln $path
    $problems = @()
    if ($result.Code -ne 0) { $problems += "exit code is $($result.Code), expected 0" }
    if ($result.Output.ToLower().Contains("error")) { $problems += "the output contains an error message" }
    Write-Result $path $problems
}

Write-Host ""
Write-Host "--- Regression: exact output ---"
Test-Exact "tests\regression"

Write-Host ""
Write-Host "--- Static errors (exit code 65) ---"
Test-Errors "tests\errors\static" 65

Write-Host ""
Write-Host "--- Runtime errors (exit code 70) ---"
Test-Errors "tests\errors\runtime" 70

Write-Host ""
Write-Host "--- Runs cleanly ---"
Test-Clean "tests\valid.exln"

Write-Host ""
Write-Host "Passed: $script:passed   Failed: $script:failed"
if ($script:failed -gt 0) { exit 1 }
