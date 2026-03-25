package ar.edu.itba.ss.cim.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ar.edu.itba.ss.cim.models.Particle;

public class OutputWriter {

    /**
     * Writes the neighbors mapping to the output file as described:
     * [id] [neighbor_id1] [neighbor_id2] ...
     */
    public static void writeNeighbors(Map<Particle, List<Particle>> neighbors, String outputFile) throws IOException {
        try {
            if (new java.io.File(outputFile).getParentFile() != null)
                new java.io.File(outputFile).getParentFile().mkdirs();
        } catch (Exception e) {
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (Map.Entry<Particle, List<Particle>> entry : neighbors.entrySet()) {
                StringBuilder line = new StringBuilder();
                line.append("[").append(entry.getKey().getId()).append('\t');
                entry.getValue().sort((p1, p2) -> p1.getId() - p2.getId());
                for (Particle neighbor : entry.getValue()) {
                    line.append(" ").append(neighbor.getId());
                }
                line.append("]");
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    public static void writeStatic(int N, double L, List<Particle> particles, String staticFile) throws IOException {
        try (BufferedWriter staticWriter = new BufferedWriter(new FileWriter(staticFile))) {
            staticWriter.write(String.valueOf(N));
            staticWriter.newLine();
            staticWriter.write(String.valueOf(L));
            staticWriter.newLine();
            for (Particle p : particles) {
                staticWriter.write(p.getRadius() + "\t" + p.getProperty());
                staticWriter.newLine();
            }
        }
    }

    public static TreeMap<Integer, List<Particle>> writeDynamic(TreeMap<Integer, List<Particle>> particles,
            String dynamicFile) throws IOException {
        try (BufferedWriter dynamicWriter = new BufferedWriter(new FileWriter(dynamicFile))) {
            for (Entry<Integer, List<Particle>> snapshot : particles.entrySet()) {
                dynamicWriter.write(String.valueOf(snapshot.getKey()));
                dynamicWriter.newLine();
                for (Particle p : snapshot.getValue()) {
                    dynamicWriter.write(p.getX() + "\t" + p.getY() + "\t" + p.getVx() + "\t" + p.getVy());
                    dynamicWriter.newLine();
                }
            }
        }
        return particles;
    }
}
