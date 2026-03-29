package ar.edu.itba.ss.vicsek;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ar.edu.itba.ss.cim.models.Particle;

public class VicsekUtils {
    public static void writeFrame(BufferedWriter writer, VicsekSimulation sim, int t) throws IOException {
        writer.write(String.valueOf(t));
        writer.newLine();
        List<Particle> particles = sim.getParticles();
        Particle leader = sim.getLeader();
        VicsekSimulation.LeaderType lt = sim.getLeaderType();
        
        for (Particle p : particles) {
            // Mark leader with isLeader flag (1 or 0)
            int isLeader = (lt != VicsekSimulation.LeaderType.NONE && p.equals(leader)) ? 1 : 0;
            writer.write(String.format(Locale.US, "%.6f\t%.6f\t%.6f\t%.6f\t%d",
                    p.getX(), p.getY(), p.getVx(), p.getVy(), isLeader));
            writer.newLine();
        }
    }

    public static void writePolarization(BufferedWriter polWriter, VicsekSimulation sim, int t) throws IOException{
        polWriter.write(String.format(Locale.US, "%d\t%.6f\t%.6f\t%.6f%n", 
            t, sim.computePolarization(), sim.computeAvgAngle(), sim.getLeader() != null ? sim.getLeader().getTheta() : 0));
    }

    public static VicsekSimulation.LeaderType parseLeaderType(String s) {
        return switch (s.toLowerCase()) {
            case "none" -> VicsekSimulation.LeaderType.NONE;
            case "fixed" -> VicsekSimulation.LeaderType.FIXED;
            case "circular" -> VicsekSimulation.LeaderType.CIRCULAR;
            default -> throw new IllegalArgumentException("Unknown leader type: " + s + ". Use none/fixed/circular.");
        };
    }
}
