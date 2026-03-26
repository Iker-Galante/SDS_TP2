package ar.edu.itba.ss.vicsek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import ar.edu.itba.ss.cim.algorithms.CellIndexMethod;
import ar.edu.itba.ss.cim.models.Particle;

/**
 * Vicsek flocking model simulation.
 * Implements the standard model plus leader variants.
 */
public class VicsekSimulation {

    public enum LeaderType {
        NONE, FIXED, CIRCULAR
    }

    // Simulation parameters
    private final int N;
    private final double L;
    private final double speed; // v0
    private final double rc; // interaction radius
    private final double eta; // noise amplitude
    private final double dt; // time step
    private final LeaderType leaderType;

    // Circular leader params
    private final double circularR; // radius of circular trajectory
    private final double circularCx; // center x
    private final double circularCy; // center y
    private final double circularOmega; // angular velocity

    // State arrays
    private List<Particle> particles;
    private Particle leader;
    private double leaderFixedAngle;
    private double leaderCircularAngle; // current angle on the circle

    private final Random rng;

    // CIM parameter
    private final int M;

    public VicsekSimulation(int N, double L, double speed, double rc, double eta,
            double dt, LeaderType leaderType, long seed) {
        this.N = N;
        this.L = L;
        this.speed = speed;
        this.rc = rc;
        this.eta = eta;
        this.dt = dt;
        this.leaderType = leaderType;
        this.rng = new Random(seed);

        // CIM grid size: L/M > rc + 2*maxRadius (particles are points, maxRadius=0)
        // Need strict inequality, so if L/M == rc, decrease M
        int tempM = (int) (L / rc);
        while (tempM > 1 && L / tempM <= rc) {
            tempM--;
        }
        this.M = tempM;
        if (M < 1)
            throw new IllegalArgumentException("L/rc too small for CIM");

        // Circular leader parameters
        this.circularR = 5.0;
        this.circularCx = L / 2.0;
        this.circularCy = L / 2.0;
        this.circularOmega = speed / circularR; // v_tangential = v0

        // Initialize particles
        this.particles = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            double x = rng.nextDouble() * L;
            double y = rng.nextDouble() * L;
            double theta = rng.nextDouble() * 2.0 * Math.PI;
            particles.add(new Particle(i, x, y, speed, theta, 0, 0, false));

        }

        if (leaderType != LeaderType.NONE){
            leader = particles.get(rng.nextInt(0, N));
            // Leader setup
            if (leaderType == LeaderType.FIXED) {
                leaderFixedAngle = leader.getTheta(); // random angle chosen at init
            } else if (leaderType == LeaderType.CIRCULAR) {
                // Place leader on the circle
                leaderCircularAngle = rng.nextDouble() * 2.0 * Math.PI;
                double rawX = circularCx + circularR * Math.cos(leaderCircularAngle);
                double rawY = circularCy + circularR * Math.sin(leaderCircularAngle);
                leader.setX((rawX + L) % L);
                leader.setY((rawY + L) % L);
                // Tangential direction
                leader.setTheta(leaderCircularAngle + Math.PI / 2.0);
            }
        }
    }

    /**
     * Run one simulation step. Returns the polarization va for this step.
     */
    public List<Particle> step() {
        // Find neighbors using CIM
        CellIndexMethod cim = new CellIndexMethod(M, particles, L, rc, 0.0, true);
        Map<Particle, SortedSet<Particle>> neighborsMap = cim.getNeighbors();

        Map<Particle, Double> newTheta = new HashMap<>();
        for (Particle p : particles) {
            // Leader particles keep their angle
            if (leaderType != LeaderType.NONE && p.equals(leader)) {
                if (leaderType == LeaderType.FIXED) {
                    newTheta.put(p, leaderFixedAngle);
                } else { // CIRCULAR
                    // Will be set below after position update
                    leaderCircularAngle += circularOmega * dt;
                    double rawX = circularCx + circularR * Math.cos(leaderCircularAngle);
                    double rawY = circularCy + circularR * Math.sin(leaderCircularAngle);
                    p.setX((rawX + L) % L);
                    p.setY((rawY + L) % L);
                    // Tangential direction
                    newTheta.put(p, leaderCircularAngle + Math.PI / 2.0);                
                }
                continue;
            }

            // Include self in the average
            double sinSum = Math.sin(p.getTheta());
            double cosSum = Math.cos(p.getTheta());
            Set<Particle> neighbors = neighborsMap.get(p);
            for (Particle p2 : neighbors) {
                sinSum += Math.sin(p2.getTheta());
                cosSum += Math.cos(p2.getTheta());
            }

            double avgAngle = Math.atan2(sinSum, cosSum);
            double noise = eta * (rng.nextDouble() - 0.5); // Uniform(-eta/2, eta/2)
            newTheta.put(p, avgAngle + noise);
        }

        // Update positions
        for (Particle p : particles) {
            p.setTheta(newTheta.get(p));
            if (!(leaderType == LeaderType.CIRCULAR) || !p.equals(leader)) {
                p.move(dt, L);
            }
        }

        return particles;
    }

    /**
     * va = (1 / N*v0) * |sum of velocity vectors|
     */
    public double computePolarization() {
        double sumVx = 0, sumVy = 0;
        for (Particle p : particles) {
            sumVx += speed * Math.cos(p.getTheta());
            sumVy += speed * Math.sin(p.getTheta());
        }
        double magnitude = Math.sqrt(sumVx * sumVx + sumVy * sumVy);
        return magnitude / (N * speed);
    }

    // Getters for output
    public int getN() {
        return N;
    }

    public double getL() {
        return L;
    }

    public double getSpeed() {
        return speed;
    }

    public double getEta() {
        return eta;
    }

    public LeaderType getLeaderType() {
        return leaderType;
    }

    public Particle getLeader() {
        return leader;
    }

    public List<Particle> getParticles(){
        return particles;
    }
}
