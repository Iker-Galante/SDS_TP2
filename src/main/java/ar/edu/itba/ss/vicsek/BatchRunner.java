package ar.edu.itba.ss.vicsek;

import java.io.*;
import java.util.*;

/**
 * Batch runner: sweeps over eta values and multiple seeds for each scenario.
 * Usage: java -cp vicsek.jar ar.edu.itba.ss.vicsek.BatchRunner <leaderType> [outputDir] [density] [L]
 *
 * Produces:
 *  - dynamic/polarization files for each (eta, seed) pair
 *  - summary_<scenario>.csv with eta, va_mean, va_std
 */
public class BatchRunner {

    private static final double DEFAULT_SPEED = 0.03;
    private static final double DEFAULT_RC = 1.0;
    private static final double DEFAULT_DT = 1.0;
    private static final double DEFAULT_L = 10.0;
    private static final double DEFAULT_DENSITY = 4.0;

    // Simulation parameters
    private static final int TOTAL_STEPS = 2000;
    private static final int STEADY_STATE_START = 1000; // ignore first 1000 steps for averaging
    private static final int NUM_SEEDS = 5;

    // Eta values to sweep (matching Vicsek paper range)
    private static final double[] ETA_VALUES = {
        0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0
    };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <leaderType:none|fixed|circular> [outputDir] [density] [L]");
            System.exit(1);
        }

        String leaderTypeStr = args[0];
        VicsekSimulation.LeaderType leaderType = parseLeaderType(leaderTypeStr);
        String outputDir = args.length > 1 ? args[1] : "output";
        double density = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_DENSITY;
        double L = args.length > 3 ? Double.parseDouble(args[3]) : DEFAULT_L;

        int N = (int) (density * L * L);
        String scenarioTag = leaderType.name().toLowerCase();

        new File(outputDir).mkdirs();

        String summaryFile = outputDir + "/summary_" + scenarioTag + "_rho" + String.format(Locale.US, "%.0f", density) + ".csv";

        System.out.printf("Batch run: scenario=%s, N=%d, L=%.1f, density=%.1f%n", scenarioTag, N, L, density);
        System.out.printf("Eta values: %s%n", Arrays.toString(ETA_VALUES));
        System.out.printf("Seeds per eta: %d, Total steps: %d, Steady state from: %d%n",
                NUM_SEEDS, TOTAL_STEPS, STEADY_STATE_START);

        try (BufferedWriter summaryWriter = new BufferedWriter(new FileWriter(summaryFile))) {
            summaryWriter.write("eta,va_mean,va_std");
            summaryWriter.newLine();

            for (double eta : ETA_VALUES) {
                double[] vaValues = new double[NUM_SEEDS];
                String etaTag = String.format(Locale.US, "%.2f", eta);

                for (int s = 0; s < NUM_SEEDS; s++) {
                    long seed = 42 + s * 1000;
                    System.out.printf("  eta=%.2f, seed=%d (run %d/%d)...%n", eta, seed, s + 1, NUM_SEEDS);

                    VicsekSimulation sim = new VicsekSimulation(N, L, DEFAULT_SPEED, DEFAULT_RC,
                            eta, DEFAULT_DT, leaderType, seed);

                    String dynamicFile = outputDir + "/dynamic_" + scenarioTag + "_eta" + etaTag + "_s" + s + ".txt";
                    String polarizationFile = outputDir + "/polarization_" + scenarioTag + "_eta" + etaTag + "_s" + s + ".txt";

                    // Only write dynamic files for first seed (to keep disk usage down)
                    boolean writeDynamic = (s == 0);

                    BufferedWriter dynWriter = writeDynamic ? new BufferedWriter(new FileWriter(dynamicFile)) : null;

                    try (BufferedWriter polWriter = new BufferedWriter(new FileWriter(polarizationFile))) {
                        // Initial state
                        if (writeDynamic) writeFrame(dynWriter, sim, 0);
                        double va = sim.computePolarization();
                        polWriter.write(String.format(Locale.US, "%d\t%.6f%n", 0, va));

                        double vaSum = 0;
                        int vaCount = 0;

                        for (int t = 1; t <= TOTAL_STEPS; t++) {
                            va = sim.step();
                            if (writeDynamic) writeFrame(dynWriter, sim, t);
                            polWriter.write(String.format(Locale.US, "%d\t%.6f%n", t, va));

                            if (t >= STEADY_STATE_START) {
                                vaSum += va;
                                vaCount++;
                            }
                        }

                        vaValues[s] = vaSum / vaCount;
                    }

                    if (dynWriter != null) dynWriter.close();
                }

                // Compute mean and std
                double mean = 0;
                for (double v : vaValues) mean += v;
                mean /= NUM_SEEDS;

                double variance = 0;
                for (double v : vaValues) variance += (v - mean) * (v - mean);
                variance /= NUM_SEEDS;
                double std = Math.sqrt(variance);

                summaryWriter.write(String.format(Locale.US, "%.2f,%.6f,%.6f", eta, mean, std));
                summaryWriter.newLine();
                summaryWriter.flush();

                System.out.printf("  eta=%.2f => va_mean=%.4f, va_std=%.4f%n", eta, mean, std);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Batch complete. Summary: " + summaryFile);
    }

    private static void writeFrame(BufferedWriter writer, VicsekSimulation sim, int t) throws IOException {
        writer.write(String.valueOf(t));
        writer.newLine();
        int N = sim.getN();
        int leaderIdx = sim.getLeaderIdx();
        VicsekSimulation.LeaderType lt = sim.getLeaderType();
        for (int i = 0; i < N; i++) {
            int isLeader = (lt != VicsekSimulation.LeaderType.NONE && i == leaderIdx) ? 1 : 0;
            writer.write(String.format(Locale.US, "%.6f\t%.6f\t%.6f\t%.6f\t%d",
                    sim.getX(i), sim.getY(i), sim.getVx(i), sim.getVy(i), isLeader));
            writer.newLine();
        }
    }

    private static VicsekSimulation.LeaderType parseLeaderType(String s) {
        return switch (s.toLowerCase()) {
            case "none" -> VicsekSimulation.LeaderType.NONE;
            case "fixed" -> VicsekSimulation.LeaderType.FIXED;
            case "circular" -> VicsekSimulation.LeaderType.CIRCULAR;
            default -> throw new IllegalArgumentException("Unknown leader type: " + s);
        };
    }
}
