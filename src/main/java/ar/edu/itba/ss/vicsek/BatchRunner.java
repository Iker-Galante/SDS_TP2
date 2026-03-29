package ar.edu.itba.ss.vicsek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ar.edu.itba.ss.vicsek.VicsekSimulation.LeaderType;

/**
 * Batch runner: sweeps over eta values and multiple seeds for each scenario.
 * Usage: java -cp vicsek.jar ar.edu.itba.ss.vicsek.BatchRunner <leaderType>
 * [outputDir] [density] [L]
 *
 * Produces:
 * - dynamic/polarization files for each (eta, seed) pair
 * - summary_<scenario>.csv with eta, va_mean, va_std
 */
public class BatchRunner {

    private static final double DEFAULT_SPEED = 0.03;
    private static final double DEFAULT_RC = 1.0;
    private static final double DEFAULT_DT = 1.0;
    private static final double DEFAULT_L = 10.0;
    private static final double DEFAULT_DENSITY = 4.0;

    // Simulation parameters
    private static final int TOTAL_STEPS = 2000;

    private static final int NUM_SEEDS = 1;

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
        VicsekSimulation.LeaderType leaderType = VicsekUtils.parseLeaderType(leaderTypeStr);
        String outputDir = args.length > 1 ? args[1] : "output";
        double density = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_DENSITY;
        double L = args.length > 3 ? Double.parseDouble(args[3]) : DEFAULT_L;

        int N = (int) (density * L * L);
        String scenarioTag = leaderType.name().toLowerCase();

        new File(outputDir).mkdirs();

        String summaryFile = outputDir + "/summary_" + scenarioTag + "_rho" + String.format(Locale.US, "%.0f", density)
                + ".csv";

        System.out.printf("Batch run: scenario=%s, N=%d, L=%.1f, density=%.1f%n", scenarioTag, N, L, density);
        System.out.printf("Eta values: %s%n", Arrays.toString(ETA_VALUES));
        System.out.printf("Seeds per eta: %d, Total steps: %d%n",
                NUM_SEEDS, TOTAL_STEPS);

        ExecutorService batchExecutor = Executors.newFixedThreadPool(ETA_VALUES.length);
        ExecutorService simulationExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (BufferedWriter summaryWriter = new BufferedWriter(new FileWriter(summaryFile))) {
            summaryWriter.write("eta,va_mean,va_std");
            summaryWriter.newLine();

            for (double eta : ETA_VALUES) {
                batchExecutor.submit(new RunSimulationBatch(simulationExecutor, eta, N, L, outputDir, scenarioTag,
                        leaderType, summaryWriter));
            }
            try {
                batchExecutor.shutdown();
                batchExecutor.awaitTermination(100, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                batchExecutor.shutdownNow();
            }
            simulationExecutor.shutdown();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Batch complete. Summary: " + summaryFile);
    }

    private static class RunSimulationBatch implements Runnable {

        private final ExecutorService executor;
        private final double eta;
        private final int N;
        private final double L;
        private final String outputDir;
        private final String scenarioTag;
        private final LeaderType leaderType;
        private final BufferedWriter summaryWriter;

        public RunSimulationBatch(ExecutorService executor, double eta, int N, double L, String outputDir,
                String scenarioTag, LeaderType leaderType, BufferedWriter summaryWriter) {
            this.executor = executor;
            this.eta = eta;
            this.N = N;
            this.L = L;
            this.outputDir = outputDir;
            this.scenarioTag = scenarioTag;
            this.leaderType = leaderType;
            this.summaryWriter = summaryWriter;
        }

        @Override
        public void run() {
            List<Future<List<Double>>> vaFutures = new ArrayList<>(NUM_SEEDS);
            String etaTag = String.format(Locale.US, "%.2f", eta);

            for (int s = 0; s < NUM_SEEDS; s++) {
                vaFutures.add(
                        executor.submit(new RunSimulation(s, eta, N, L, etaTag, outputDir, scenarioTag, leaderType)));
            }

            List<Double> vaValues = new ArrayList<>();
            for (Future<List<Double>> v : vaFutures) {
                boolean success = false;
                do {
                    try {
                        vaValues.addAll(v.get());
                        success = true;
                    } catch (Exception e) {
                    }
                } while (!success);
            }

            // Compute mean and std globally over all steady-state ticks from all seeds
            double mean = 0;
            for (double v : vaValues)
                mean += v;
            if (!vaValues.isEmpty())
                mean /= vaValues.size();

            double variance = 0;
            for (double v : vaValues)
                variance += Math.pow((v - mean), 2);
            if (!vaValues.isEmpty())
                variance /= vaValues.size();
            double std = Math.sqrt(variance);

            try {
                summaryWriter.write(String.format(Locale.US, "%.2f,%.6f,%.6f", eta, mean, std));
                summaryWriter.newLine();
                summaryWriter.flush();
            } catch (Exception e) {

            }

            System.out.printf("  eta=%.2f => va_mean=%.4f, va_std=%.4f (n=%d)%n", eta, mean, std, vaValues.size());
        }
    }

    private static class RunSimulation implements Callable<List<Double>> {
        private final int s;
        private final double eta;
        private final int N;
        private final double L;
        private final String etaTag;
        private final String outputDir;
        private final String scenarioTag;
        private final LeaderType leaderType;

        public RunSimulation(int s, double eta, int N, double L, String etaTag, String outputDir, String scenarioTag,
                LeaderType leaderType) {
            this.s = s;
            this.eta = eta;
            this.N = N;
            this.L = L;
            this.etaTag = etaTag;
            this.outputDir = outputDir;
            this.scenarioTag = scenarioTag;
            this.leaderType = leaderType;
        }

        @Override
        public List<Double> call() {
            long seed = 42 + s * 1000;
            // System.out.printf(" eta=%.2f, seed=%d (run %d/%d)...%n", eta, seed, s + 1,
            // NUM_SEEDS);

            VicsekSimulation sim = new VicsekSimulation(N, L, DEFAULT_SPEED, DEFAULT_RC,
                    eta, DEFAULT_DT, leaderType, seed);

            String dynamicFile = outputDir + "/dynamic_" + scenarioTag + "_eta" + etaTag + "_s" + s + ".txt";
            String polarizationFile = outputDir + "/polarization_" + scenarioTag + "_eta" + etaTag + "_s" + s + ".txt";

            boolean writeDynamic = (s == 0);

            List<Double> vaHistory = new ArrayList<>(TOTAL_STEPS);

            try (BufferedWriter polWriter = new BufferedWriter(new FileWriter(polarizationFile));
                    BufferedWriter dynWriter = writeDynamic ? new BufferedWriter(new FileWriter(dynamicFile)) : null) {
                if (writeDynamic)
                    VicsekUtils.writeFrame(dynWriter, sim, 0);
                double va = sim.computePolarization();
                vaHistory.add(va);
                polWriter.write(String.format(Locale.US, "%d\t%.6f%n", 0, va));

                for (int t = 1; t < TOTAL_STEPS; t++) {
                    sim.step();
                    va = sim.computePolarization();
                    vaHistory.add(va);
                    if (writeDynamic)
                        VicsekUtils.writeFrame(dynWriter, sim, t);
                    polWriter.write(String.format(Locale.US, "%d\t%.6f%n", t, va));
                }
            } catch (IOException e) {
                // Error writting, ignore
            }

            int steadyStateStart = detectSteadyState(vaHistory);
            return vaHistory.subList(steadyStateStart, vaHistory.size());
            // for (int t = steadyStateStart; t < TOTAL_STEPS; t++) {
            //     steadyValues.add(vaHistory[t]);
            // }
            // return steadyValues;
        }

        private int detectSteadyState(List<Double> va) {
            int n = va.size();
            int window = 200;

            for (int t = 0; t <= n - window * 2; t += 50) {
                double m1 = windowAvg(va, t, t + window);
                double m2 = windowAvg(va, t + window, t + window * 2);
                if (Math.abs(m1 - m2) < 0.02) {
                    return t + window;
                }
            }
            return n / 2; // fallback
        }

        private double windowAvg(List<Double> arr, int start, int end) {
            double sum = 0;
            for (double i : arr.subList(start, end))
                sum += i;
            return sum / (end - start);
        }
    }
}
