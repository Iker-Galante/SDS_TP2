package ar.edu.itba.ss.cim.models;

import java.util.Objects;

public class Particle {
    private final int id; // Id unico
    private double x; // Posicion en x
    private double y; // Posicion en y
    private double vx; // Velocidad en x
    private double vy; // Velocidad en y
    private final double radius; // Radio
    private final int effectRadius;
    private final double property; // Propiedad
    private final double speed; // Magnitud de velocidad (módulo v)
    private double theta; // Ángulo continuo (en radianes)
    private boolean isLeader;

    // Constructor principal para Off-Lattice Vicsek
    public Particle(int id, double x, double y, double vx, double vy, double radius, double property) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.effectRadius = 0;
        this.property = property;
        this.speed = Math.hypot(vx, vy); // Módulo de velocidad
        this.theta = Math.atan2(vy, vx); // Ángulo continuo inicial (-π a π)
        this.isLeader = false;
    }

    // Constructor alternativo usando speed y theta
    public Particle(int id, double x, double y, double speed, double theta, double radius, double property, boolean isLeade, int effectRadius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.theta = theta; // Ángulo continuo
        this.vx = speed * Math.cos(theta);
        this.vy = speed * Math.sin(theta);
        this.radius = radius;
        this.property = property;
        this.isLeader = false;
        this.effectRadius = effectRadius;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
    }

    public double getRadius() {
        return radius;
    }

    public double getProperty() {
        return property;
    }

    public double getSpeed() {
        return speed;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
        updateVelocity();
    }

    private void updateVelocity() {
        this.vx = speed * Math.cos(theta);
        this.vy = speed * Math.sin(theta);
    }

    public void move(double dt) {
        this.x += vx * dt;
        this.y += vy * dt;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void makeLeader() {
        this.isLeader = true;
    }

    /**
     * Calculates the border-to-border distance between this particle and another
     * particle.
     * Takes periodic boundary conditions into account if specified.
     */
    public double borderDistance(Particle other, double L, boolean periodic) {
        double dx = Math.abs(this.x - other.x);
        double dy = Math.abs(this.y - other.y);

        if (periodic) {
            if (dx > L / 2)
                dx = L - dx;
            if (dy > L / 2)
                dy = L - dy;
        }

        double centerDistance = Math.hypot(dx, dy);
        // Border to border distance
        return centerDistance - this.radius - other.radius;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Particle particle = (Particle) o;
        return id == particle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean checkNeighbor(Particle other, double L, boolean periodic, double rc){
        return ((!this.equals(other)) && this.borderDistance(other, L, periodic) < rc);
    }
}
