#!/bin/bash
# Run all Vicsek simulations and generate plots.
# Usage: ./run_all.sh [outputDir] [density] [L]

OUTPUT_DIR="${1:-output}"
DENSITY="${2:-4}"
L="${3:-10}"

JAR="target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Build if needed
if [ ! -f "$JAR" ]; then
    echo "Building Java project..."
    mvn clean package -q
fi

echo "============================================"
echo " Vicsek Flocking Model - Full Run"
echo " Output dir: $OUTPUT_DIR"
echo " Density: $DENSITY, L: $L"
echo "============================================"

# Run batch for each scenario
for SCENARIO in none fixed circular; do
    echo ""
    echo ">>> Running batch: $SCENARIO"
    java -cp "$JAR" ar.edu.itba.ss.vicsek.BatchRunner "$SCENARIO" "$OUTPUT_DIR" "$DENSITY" "$L"
done

echo ""
echo "============================================"
echo " Generating Plots"
echo "============================================"
uv run python plot_polarization.py "$OUTPUT_DIR" --density "$DENSITY"

echo ""
echo "============================================"
echo " Generating Animations (sample frames)"
echo "============================================"
# Render a few sample frames for each scenario at low and high noise
for SCENARIO in none fixed circular; do
    for ETA in 0.00 2.00 5.00; do
        DYNFILE="$OUTPUT_DIR/dynamic_${SCENARIO}_eta${ETA}_s0.txt"
        if [ -f "$DYNFILE" ]; then
            ANIM_DIR="$OUTPUT_DIR/anim_${SCENARIO}_eta${ETA}"
            echo ">>> Animating $SCENARIO eta=$ETA"
            uv run python animate.py "$DYNFILE" --output_dir "$ANIM_DIR" --skip 50 --frames 200
        fi
    done
done

echo ""
echo "All done! Check $OUTPUT_DIR for results."
