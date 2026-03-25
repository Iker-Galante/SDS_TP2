package ar.edu.itba.ss.cim.generator;

import ar.edu.itba.ss.cim.models.Particle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleGenerator {

    /**
     * Generates particles without overlap.
     */
    public static List<Particle> generateParticles(int N, double L, double rMin, double rMax, double speed,
            double property) {
        List<Particle> particles = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < N; i++) {
            boolean validPosition = false;
            double radius = rMin + (rMax - rMin) * random.nextDouble();
            double x = 0;
            double y = 0;

            int attempts = 0;
            while (!validPosition) {
                if (attempts++ > 10000) {
                    throw new RuntimeException("Could not place particle " + i
                            + " after 10000 attempts. Density may be too high for L=" + L + ", N=" + N);
                }

                // El centro no debe ir fuera del area de lado L asi el borde no se va afuera
                // por eso se hizo desde [radio,L-radio] para el centro
                // Las distancias se miden borde a borde
                x = radius + (L - 2 * radius) * random.nextDouble();
                y = radius + (L - 2 * radius) * random.nextDouble();

                validPosition = true;
                // Check overlaps
                for (Particle p : particles) {
                    double distSq = Math.pow(x - p.getX(), 2) + Math.pow(y - p.getY(), 2);
                    double minSumSq = Math.pow(radius + p.getRadius(), 2);
                    if (distSq <= minSumSq) {
                        validPosition = false;
                        break;
                    }
                }
            }

            double angle = 2 * Math.PI * random.nextDouble();
            double vx = speed * Math.cos(angle);
            double vy = speed * Math.sin(angle);

            particles.add(new Particle(i, x, y, vx, vy, radius, property));
        }

        return particles;
    }

    public static void generateFiles(int N, double L, double rMin, double rMax, double speed, double property,
            String staticFile, String dynamicFile) throws IOException {
        List<Particle> particles = generateParticles(N, L, rMin, rMax, speed, property);

        try (BufferedWriter staticWriter = new BufferedWriter(new FileWriter(staticFile));
                BufferedWriter dynamicWriter = new BufferedWriter(new FileWriter(dynamicFile))) {

            staticWriter.write(String.valueOf(N));
            staticWriter.newLine();
            staticWriter.write(String.valueOf(L));
            staticWriter.newLine();

            dynamicWriter.write("0"); // tiempo inicial
            dynamicWriter.newLine();

            for (Particle p : particles) {
                // Static: radio y propiedad
                staticWriter.write(p.getRadius() + " " + p.getProperty());
                staticWriter.newLine();

                // Dynamic: posicion y velocidad
                dynamicWriter.write(p.getX() + " " + p.getY() + " " + p.getVx() + " " + p.getVy());
                dynamicWriter.newLine();
            }
        }
    }
}
