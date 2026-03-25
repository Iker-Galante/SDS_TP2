package ar.edu.itba.ss.cim.models;

import java.util.List;

public class InputParams {
    private final double L;
    private final int N;
    private final double t0;
    private final double maxRadius;
    private final List<Particle> particles;

    public InputParams(double L, int N, double t0, double maxRadius, List<Particle> particles){
        this.L = L;
        this.N = N;
        this.t0 = t0;
        this.maxRadius = maxRadius;
        this.particles = particles;
    }

    public double getL(){
        return L;
    }

    public int getN(){
        return N;
    }

    public double gett0(){
        return t0;
    }

    public List<Particle> getParticles(){
        return particles;
    }

    public double getMaxRadius(){
        return maxRadius;
    }
}
