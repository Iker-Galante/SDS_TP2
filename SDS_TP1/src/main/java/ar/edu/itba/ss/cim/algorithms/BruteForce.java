package ar.edu.itba.ss.cim.algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import ar.edu.itba.ss.cim.models.Particle;

public class BruteForce implements NeighborFinder {
    private final double L;
    private final double rc;
    private final boolean periodic;
    private final List<Particle> particles;

    public BruteForce(List<Particle> particles, double L, double rc, boolean periodic){
        this.L = L;
        this.rc = rc;
        this.periodic = periodic;
        this.particles = particles;
    }

    @Override
    public Map<Particle, SortedSet<Particle>> getNeighbors() {
        Map<Particle, SortedSet<Particle>> neighbors = new HashMap<>(particles.size());
        particles.forEach(p -> neighbors.put(p, new TreeSet<>()));

        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                if (p1.checkNeighbor(p2, L, periodic, rc)){
                    neighbors.get(p1).add(p2);
                    neighbors.get(p2).add(p1);
                }
            }
        }

        return neighbors;
    }
}
