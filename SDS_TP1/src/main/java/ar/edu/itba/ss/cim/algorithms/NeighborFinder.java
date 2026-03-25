package ar.edu.itba.ss.cim.algorithms;

import ar.edu.itba.ss.cim.models.Particle;

import java.util.Map;
import java.util.SortedSet;

public interface NeighborFinder {
    Map<Particle, SortedSet<Particle>> getNeighbors();
}
