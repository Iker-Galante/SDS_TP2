package ar.edu.itba.ss.cim;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ar.edu.itba.ss.cim.algorithms.BruteForce;
import ar.edu.itba.ss.cim.algorithms.CellIndexMethod;
import ar.edu.itba.ss.cim.algorithms.NeighborFinder;
import ar.edu.itba.ss.cim.generator.ParticleGenerator;
import ar.edu.itba.ss.cim.models.InputParams;
import ar.edu.itba.ss.cim.models.Particle;
import ar.edu.itba.ss.cim.utils.InputParser;
import ar.edu.itba.ss.cim.utils.OutputWriter;

public class Main {
    private static final int MAX_TIME_STAMPS = 9;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please provide arguments.");
            return;
        }

        String mode = args[0];
        try {
            if ("gen".equals(mode)) {
                // gen N L rMin rMax speed property staticFile dynamicFile
                if (args.length < 9) {
                    System.err.println("Usage: gen N L rMin rMax speed property staticFile dynamicFile");
                    return;
                }
                int N = Integer.parseInt(args[1]);
                double L = Double.parseDouble(args[2]);
                double rMin = Double.parseDouble(args[3]);
                double rMax = Double.parseDouble(args[4]);
                double speed = Double.parseDouble(args[5]);
                double property = Double.parseDouble(args[6]);
                String staticFile = args[7];
                String dynamicFile = args[8];

                try {
                    if (new java.io.File(staticFile).getParentFile() != null)
                        new java.io.File(staticFile).getParentFile().mkdirs();
                    if (new java.io.File(dynamicFile).getParentFile() != null)
                        new java.io.File(dynamicFile).getParentFile().mkdirs();
                } catch (Exception e) {
                }

                List<Particle> initialParticles = ParticleGenerator.generateParticles(N, L, rMin, rMax, speed,
                        property);
                OutputWriter.writeStatic(N, L, initialParticles, staticFile);

                TreeMap<Integer, List<Particle>> particles = new TreeMap<>();
                particles.put(0, initialParticles);
                for (int i = 1; i <= MAX_TIME_STAMPS; i++) {
                    particles.put(i, ParticleGenerator.generateParticles(N, L, rMin, rMax, speed, property));
                }
                particles = OutputWriter.writeDynamic(particles, dynamicFile);
                System.out.println("Generation complete.");

            } else if ("run".equals(mode)) {
                // run staticFile dynamicFile rc M periodic algorithm(cim/bf) outputFile
                // timeFile timestamp
                if (args.length < 10) {
                    System.err.println(
                            "Usage: run staticFile dynamicFile rc M periodic algorithm(cim/bf) outputFile timeFile timestamp");
                    return;
                }

                String staticFile = args[1];
                String dynamicFile = args[2];
                double rc;
                try {
                    rc = Double.parseDouble(args[3]);
                    if (rc < 0)
                        throw new RuntimeException("Radius can't be negative");
                } catch (NumberFormatException e) {
                    System.err.println("Radius of contact must be a number");
                    return;
                }
                int M;
                try {
                    M = Integer.parseInt(args[4]);
                    if (rc < 0)
                        throw new RuntimeException("Grid size can't be negative");
                } catch (NumberFormatException e) {
                    System.err.println("Grid size must be an integer");
                    return;
                }
                boolean periodic;
                try {
                    periodic = Boolean.parseBoolean(args[5]);
                } catch (NumberFormatException e) {
                    System.err.println("Periodic boundary conditions must be true or false");
                    return;
                }
                String algo = args[6];
                String outputFile = args[7];
                String timeFile = args[8];
                double timestamp;
                try {
                    timestamp = Double.parseDouble(args[9]);
                    if (timestamp < 0 || timestamp > 9)
                        throw new RuntimeException("Timestamp can't be negative nor greater than 9");
                } catch (NumberFormatException e) {
                    System.err.println("Timestamp must be a number between 0 and 9");
                    return;
                }

                InputParams params = InputParser.parse(staticFile, dynamicFile, timestamp);

                NeighborFinder finder;
                if ("cim".equalsIgnoreCase(algo)) {
                    finder = new CellIndexMethod(M, params.getParticles(), params.getL(), rc, params.getMaxRadius(),
                            periodic);
                } else {
                    finder = new BruteForce(params.getParticles(), params.getL(), rc, periodic);
                }

                long startTime = System.nanoTime();
                Map<Particle, List<Particle>> neighbors;
                try {
                    neighbors = finder.getNeighbors();
                } catch (RuntimeException e) {
                    System.err.println(e.getMessage());
                    return;
                }
                long endTime = System.nanoTime();
                double execTimeMs = (endTime - startTime) / 1_000_000.0;

                OutputWriter.writeNeighbors(neighbors, outputFile);

                // Write time to timeFile
                try {
                    if (new java.io.File(timeFile).getParentFile() != null)
                        new java.io.File(timeFile).getParentFile().mkdirs();
                } catch (Exception e) {
                }
                try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(timeFile))) {
                    writer.write(String.valueOf(execTimeMs));
                }

                System.out.println("Execution complete in " + execTimeMs + " ms");
            } else {
                System.err.println("Unknown mode: " + mode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
