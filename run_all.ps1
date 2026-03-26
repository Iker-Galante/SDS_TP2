# Run all Vicsek simulations and generate plots (PowerShell version).
# Usage: .\run_all.ps1 [-OutputDir output] [-Density 4] [-L 10]

param(
    [string]$OutputDir = "output",
    [double]$Density = 4,
    [double]$L = 10
)

$JAR = "target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Build if needed
if (-not (Test-Path $JAR)) {
    Write-Host "Building Java project..."
    mvn clean package -q
}

Write-Host "============================================"
Write-Host " Vicsek Flocking Model - Full Run"
Write-Host " Output dir: $OutputDir"
Write-Host " Density: $Density, L: $L"
Write-Host "============================================"

# Run batch for each scenario
foreach ($scenario in @("none", "fixed", "circular")) {
    Write-Host ""
    Write-Host ">>> Running batch: $scenario"
    java -cp $JAR ar.edu.itba.ss.vicsek.BatchRunner $scenario "$OutputDir/simulation" $Density $L
}

Write-Host ""
Write-Host "============================================"
Write-Host " Generating Plots"
Write-Host "============================================"
uv run python plot_polarization.py "$OutputDir/simulation" $OutputDir --density $Density

Write-Host ""
Write-Host "============================================"
Write-Host " Generating Animations"
Write-Host "============================================"
# Render sample animations for each scenario at low and high noise
foreach ($scenario in @("none", "fixed", "circular")) {
    foreach ($eta in @("0.00", "2.00", "5.00")) {
        $dynFile = "$OutputDir/simulation/dynamic_${scenario}_eta${eta}_s0.txt"
        if (Test-Path $dynFile) {
            $animDir = "$OutputDir/animation/anim_${scenario}_eta${eta}"
            Write-Host ">>> Animating $scenario eta=$eta"
            uv run python animate.py $dynFile --output_dir $animDir --frames 200 --fps 30
        }
    }
}

Write-Host ""
Write-Host "All done! Check $OutputDir for results."
