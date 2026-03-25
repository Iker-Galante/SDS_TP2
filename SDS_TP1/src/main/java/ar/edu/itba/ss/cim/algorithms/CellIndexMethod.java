package ar.edu.itba.ss.cim.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import ar.edu.itba.ss.cim.models.Particle;

public class CellIndexMethod implements NeighborFinder {
    private final int M;
    private final double L;
    private final double rc;
    private final boolean periodic;
    private final double maxRadius;
    private final List<Particle> particles;
    private final List<Particle>[][] grid;

    @SuppressWarnings("unchecked")
    public CellIndexMethod(int M, List<Particle> particles, double L, double rc, double maxRadius, boolean periodic) {
        this.M = M;
        this.L = L;
        this.rc = rc;
        this.periodic = periodic;
        this.particles = particles;
        this.maxRadius = maxRadius;
        // Initialize grid
        this.grid = new ArrayList[M][M];
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                grid[i][j] = new ArrayList<>();
            }
        }

        // Assign particles to cells
        for (Particle p : particles) {
            int col = (int) (p.getX() * M / L);
            int row = (int) (p.getY() * M / L);
            // Handle edge case x or y == L
            if (col >= M)
                col = M - 1;
            if (row >= M)
                row = M - 1;
            // Handle out of bounds if not strictly [0, L]
            if (col < 0)
                col = 0;
            if (row < 0)
                row = 0;

            grid[row][col].add(p);
        }
    }

    @Override
    public Map<Particle, SortedSet<Particle>> getNeighbors() {
        // Validate constraint: L/M > rc + 2 * r_max
        if (L / M <= rc + 2 * maxRadius) {
            throw new RuntimeException("Grid size too small, cannot guarantee precision");
        }

        Map<Particle, SortedSet<Particle>> neighbors = new HashMap<>(particles.size());
        for (Particle p : particles) {
            neighbors.put(p, new TreeSet<>());
        }

        // Check neighbors
        for (int row = 0; row < M; row++) {
            for (int col = 0; col < M; col++) {
                List<Particle> cell = grid[row][col];
                for (int i = 0; i < cell.size(); i++) {
                    Particle p1 = cell.get(i);
                    // Same cell, subsequent particles
                    for (int j = i + 1; j < cell.size(); j++) {
                        Particle p2 = cell.get(j);
                        if (p1.borderDistance(p2, L, periodic) < rc) {
                            neighbors.get(p1).add(p2);
                            neighbors.get(p2).add(p1);
                        }
                    }

                    // Top cell (row + 1, col)
                    checkNeighborCell(row + 1, col, p1, neighbors);
                    // Top Right cell (row + 1, col + 1)
                    checkNeighborCell(row + 1, col + 1, p1, neighbors);
                    // Right cell (row, col + 1)
                    checkNeighborCell(row, col + 1, p1, neighbors);
                    // Bottom Right cell (row - 1, col + 1)
                    checkNeighborCell(row - 1, col + 1, p1, neighbors);
                }
            }
        }
        return neighbors;
    }

    private void checkNeighborCell(int nRow, int nCol, Particle p1, Map<Particle, SortedSet<Particle>> neighborSets) {
        if (!periodic) {
            // Check bounds strictly if not periodic
            if (nRow < 0 || nRow >= M || nCol < 0 || nCol >= M)
                return;
        }

        int wrappedRow = (nRow + M) % M;
        int wrappedCol = (nCol + M) % M;

        if (!periodic && (wrappedRow != nRow || wrappedCol != nCol)) {
            return;
        }

        List<Particle> nCell = grid[wrappedRow][wrappedCol];
        for (Particle p2 : nCell) {
            if (p1.getId() == p2.getId())
                continue;

            if (neighborSets.get(p1).contains(p2))
                continue;

            if (p1.borderDistance(p2, L, periodic) < rc) {
                neighborSets.get(p1).add(p2);
                neighborSets.get(p2).add(p1);
            }
        }
    }
}
