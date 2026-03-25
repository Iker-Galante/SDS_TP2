package ar.edu.itba.ss.vicsek;

import ar.edu.itba.ss.cim.algorithms.CellIndexMethod;
import ar.edu.itba.ss.cim.models.Particle;

import java.util.*;

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
    private double[] x, y, theta;
    private int leaderIdx = 0;
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
        this.x = new double[N];
        this.y = new double[N];
        this.theta = new double[N];

        for (int i = 0; i < N; i++) {
            x[i] = rng.nextDouble() * L;
            y[i] = rng.nextDouble() * L;
            theta[i] = rng.nextDouble() * 2.0 * Math.PI;
        }

        // Leader setup
        if (leaderType == LeaderType.FIXED) {
            leaderFixedAngle = theta[leaderIdx]; // random angle chosen at init
        } else if (leaderType == LeaderType.CIRCULAR) {
            // Place leader on the circle
            leaderCircularAngle = rng.nextDouble() * 2.0 * Math.PI;
            double rawX = circularCx + circularR * Math.cos(leaderCircularAngle);
            double rawY = circularCy + circularR * Math.sin(leaderCircularAngle);
            x[leaderIdx] = ((rawX % L) + L) % L;
            y[leaderIdx] = ((rawY % L) + L) % L;
            // Tangential direction
            theta[leaderIdx] = leaderCircularAngle + Math.PI / 2.0;
        }
    }

    /**
     * Run one simulation step. Returns the polarization va for this step.
     */
    public double step() {
        // Build particle list for CIM (point particles: radius=0)
        List<Particle> particles = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            particles.add(new Particle(i, x[i], y[i],
                    speed * Math.cos(theta[i]), speed * Math.sin(theta[i]),
                    0.0, 0.0));
        }

        // Find neighbors using CIM
        CellIndexMethod cim = new CellIndexMethod(M, particles, L, rc, 0.0, true);
        Map<Particle, List<Particle>> neighborsMap = cim.getNeighbors();

        // Build index-based neighbor lookup
        Map<Integer, List<Integer>> neighborIds = new HashMap<>();
        for (Map.Entry<Particle, List<Particle>> entry : neighborsMap.entrySet()) {
            List<Integer> ids = new ArrayList<>();
            for (Particle p : entry.getValue()) {
                ids.add(p.getId());
            }
            neighborIds.put(entry.getKey().getId(), ids);
        }

        // Compute new angles
        double[] newTheta = new double[N];
        for (int i = 0; i < N; i++) {
            // Leader particles keep their angle
            if (i == leaderIdx && leaderType != LeaderType.NONE) {
                if (leaderType == LeaderType.FIXED) {
                    newTheta[i] = leaderFixedAngle;
                } else { // CIRCULAR
                    // Will be set below after position update
                    newTheta[i] = theta[i]; // placeholder
                }
                continue;
            }

            // Include self in the average
            double sinSum = Math.sin(theta[i]);
            double cosSum = Math.cos(theta[i]);
            List<Integer> neighbors = neighborIds.getOrDefault(i, Collections.emptyList());
            for (int j : neighbors) {
                sinSum += Math.sin(theta[j]);
                cosSum += Math.cos(theta[j]);
            }

            double avgAngle = Math.atan2(sinSum, cosSum);
            double noise = eta * (rng.nextDouble() - 0.5); // Uniform(-eta/2, eta/2)
            newTheta[i] = avgAngle + noise;
        }

        // Update positions
        for (int i = 0; i < N; i++) {
            if (i == leaderIdx && leaderType == LeaderType.CIRCULAR) {
                // Circular leader: advance on circle
                leaderCircularAngle += circularOmega * dt;
                double rawX = circularCx + circularR * Math.cos(leaderCircularAngle);
                double rawY = circularCy + circularR * Math.sin(leaderCircularAngle);
                x[i] = ((rawX % L) + L) % L;
                y[i] = ((rawY % L) + L) % L;
                // Tangential direction
                newTheta[i] = leaderCircularAngle + Math.PI / 2.0;
            } else {
                x[i] = x[i] + speed * Math.cos(newTheta[i]) * dt;
                y[i] = y[i] + speed * Math.sin(newTheta[i]) * dt;
                // Periodic boundary conditions
                x[i] = ((x[i] % L) + L) % L;
                y[i] = ((y[i] % L) + L) % L;
            }
        }

        theta = newTheta;

        // Compute polarization
        return computePolarization();
    }

    /**
     * va = (1 / N*v0) * |sum of velocity vectors|
     */
    public double computePolarization() {
        double sumVx = 0, sumVy = 0;
        for (int i = 0; i < N; i++) {
            sumVx += speed * Math.cos(theta[i]);
            sumVy += speed * Math.sin(theta[i]);
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

    public int getLeaderIdx() {
        return leaderIdx;
    }

    public double getX(int i) {
        return x[i];
    }

    public double getY(int i) {
        return y[i];
    }

    public double getTheta(int i) {
        return theta[i];
    }

    public double getVx(int i) {
        return speed * Math.cos(theta[i]);
    }

    public double getVy(int i) {
        return speed * Math.sin(theta[i]);
    }
}
