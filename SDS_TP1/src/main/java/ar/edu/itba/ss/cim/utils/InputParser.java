package ar.edu.itba.ss.cim.utils;

import ar.edu.itba.ss.cim.models.Particle;
import ar.edu.itba.ss.cim.models.InputParams;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class InputParser {

    /**
     * Parses the static and dynamic files to create the list of particles.
     * 
     * @param staticFile  path to the static file
     * @param dynamicFile path to the dynamic file
     * @return List of particles
     */
    public static InputParams parse(String staticFile, String dynamicFile, double timestamp) throws IOException {
        List<Particle> particles = new ArrayList<>();

        try (BufferedReader staticReader = new BufferedReader(new FileReader(staticFile));
                BufferedReader dynamicReader = new BufferedReader(new FileReader(dynamicFile))) {

            // Static file heading
            String nStr = staticReader.readLine();
            int N = Integer.parseInt(nStr.trim());

            String lStr = staticReader.readLine();
            double L = Double.parseDouble(lStr.trim()); // Not used here directly, but useful to validate

            String timeString;
            /*
             * timeString = dynamicReader.readLine(); // Ignore t0 if not needed
             * t0 = Double.parseDouble(timeString.trim());
             */
            // Dynamic file heading
            for (int i = 0; i < ((int) timestamp) * N; i++) {
                dynamicReader.readLine();
            }

            for (int i = (int) timestamp; i >= 0; i--) {
                timeString = dynamicReader.readLine();
            }

            double maxRadius = 0;
            for (int i = 0; i < N; i++) {
                // Static
                String staticLine = staticReader.readLine();
                if (staticLine == null)
                    throw new IOException("Unexpected end of static file.");
                StringTokenizer stStatic = new StringTokenizer(staticLine);
                double radius = Double.parseDouble(stStatic.nextToken());
                double property = Double.parseDouble(stStatic.nextToken());
                maxRadius = Math.max(radius, maxRadius);
                // Dynamic
                String dynamicLine = dynamicReader.readLine();
                if (dynamicLine == null)
                    throw new IOException("Unexpected end of dynamic file.");
                StringTokenizer stDynamic = new StringTokenizer(dynamicLine);
                double x = Double.parseDouble(stDynamic.nextToken());
                double y = Double.parseDouble(stDynamic.nextToken());
                double vx = Double.parseDouble(stDynamic.nextToken());
                double vy = Double.parseDouble(stDynamic.nextToken());

                particles.add(new Particle(i, x, y, vx, vy, radius, property));
            }
            return new InputParams(L, N, timestamp, maxRadius, particles);
        }
    }
}
