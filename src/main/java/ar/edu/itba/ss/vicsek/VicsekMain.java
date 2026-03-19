package ar.edu.itba.ss.vicsek;

import java.io.*;
import java.util.Locale;

/**
 * CLI entry point for a single Vicsek simulation run.
 * Usage: java -jar vicsek.jar <eta> <leaderType> [totalSteps] [outputDir] [seed] [density] [L]
 */
public class VicsekMain {

    // Default Vicsek parameters from Ref [1]
    private static final double DEFAULT_SPEED = 0.03;
    private static final double DEFAULT_RC = 1.0;
    private static final double DEFAULT_DT = 1.0;
    private static final double DEFAULT_L = 10.0;
    private static final double DEFAULT_DENSITY = 4.0;
    private static final int DEFAULT_STEPS = 1000;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: <eta> <leaderType:none|fixed|circular> [totalSteps] [outputDir] [seed] [density] [L]");
            System.exit(1);
        }

        double eta = Double.parseDouble(args[0]);
        VicsekSimulation.LeaderType leaderType = parseLeaderType(args[1]);
        int totalSteps = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_STEPS;
        String outputDir = args.length > 3 ? args[3] : "output";
        long seed = args.length > 4 ? Long.parseLong(args[4]) : System.currentTimeMillis();
        double density = args.length > 5 ? Double.parseDouble(args[5]) : DEFAULT_DENSITY;
        double L = args.length > 6 ? Double.parseDouble(args[6]) : DEFAULT_L;

        int N = (int) (density * L * L);
        double speed = DEFAULT_SPEED;
        double rc = DEFAULT_RC;
        double dt = DEFAULT_DT;

        System.out.printf("Vicsek Simulation: N=%d, L=%.1f, eta=%.2f, leader=%s, steps=%d, seed=%d%n",
                N, L, eta, leaderType, totalSteps, seed);

        // Create output directory
        new File(outputDir).mkdirs();

        VicsekSimulation sim = new VicsekSimulation(N, L, speed, rc, eta, dt, leaderType, seed);

        String scenarioTag = leaderType.name().toLowerCase();
        String etaTag = String.format(Locale.US, "%.2f", eta);

        String dynamicFile = outputDir + "/dynamic_" + scenarioTag + "_eta" + etaTag + ".txt";
        String polarizationFile = outputDir + "/polarization_" + scenarioTag + "_eta" + etaTag + ".txt";

        try (BufferedWriter dynWriter = new BufferedWriter(new FileWriter(dynamicFile));
             BufferedWriter polWriter = new BufferedWriter(new FileWriter(polarizationFile))) {

            // Write initial state (t=0)
            writeFrame(dynWriter, sim, 0);
            double va = sim.computePolarization();
            polWriter.write(String.format(Locale.US, "%d\t%.6f%n", 0, va));

            for (int t = 1; t <= totalSteps; t++) {
                va = sim.step();
                writeFrame(dynWriter, sim, t);
                polWriter.write(String.format(Locale.US, "%d\t%.6f%n", t, va));

                if (t % 100 == 0) {
                    System.out.printf("  Step %d/%d, va=%.4f%n", t, totalSteps, va);
                }
            }

        } catch (IOException e) {
            System.err.println("Error writing output: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Done. Output in " + outputDir);
    }

    private static void writeFrame(BufferedWriter writer, VicsekSimulation sim, int t) throws IOException {
        writer.write(String.valueOf(t));
        writer.newLine();
        int N = sim.getN();
        int leaderIdx = sim.getLeaderIdx();
        VicsekSimulation.LeaderType lt = sim.getLeaderType();
        for (int i = 0; i < N; i++) {
            // Mark leader with isLeader flag (1 or 0)
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
            default -> throw new IllegalArgumentException("Unknown leader type: " + s + ". Use none/fixed/circular.");
        };
    }
}
