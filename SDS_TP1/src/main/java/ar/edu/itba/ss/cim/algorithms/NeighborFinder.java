package ar.edu.itba.ss.cim.algorithms;

import ar.edu.itba.ss.cim.models.Particle;

import java.util.List;
import java.util.Map;

public interface NeighborFinder {
    Map<Particle, List<Particle>> getNeighbors();
}
