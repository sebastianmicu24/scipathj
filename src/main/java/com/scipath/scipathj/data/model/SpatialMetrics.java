package com.scipath.scipathj.data.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Spatial metrics for cell analysis including neighbor relationships
 */
public class SpatialMetrics {
    private final List<String> neighborIds;
    private final double nearestNeighborDistance;
    private final double averageNeighborDistance;
    private final int neighborCount;
    private final double localDensity;
    private final double clusteringCoefficient;
    
    public SpatialMetrics(List<String> neighborIds, double nearestNeighborDistance, 
                         double averageNeighborDistance, double localDensity) {
        this.neighborIds = new ArrayList<>(neighborIds);
        this.nearestNeighborDistance = nearestNeighborDistance;
        this.averageNeighborDistance = averageNeighborDistance;
        this.neighborCount = neighborIds.size();
        this.localDensity = localDensity;
        this.clusteringCoefficient = calculateClusteringCoefficient();
    }
    
    private double calculateClusteringCoefficient() {
        // Simplified clustering coefficient calculation
        if (neighborCount < 2) return 0.0;
        return Math.min(1.0, neighborCount / 6.0); // Hexagonal packing assumption
    }
    
    // Getters
    public List<String> getNeighborIds() { return new ArrayList<>(neighborIds); }
    public double getNearestNeighborDistance() { return nearestNeighborDistance; }
    public double getAverageNeighborDistance() { return averageNeighborDistance; }
    public int getNeighborCount() { return neighborCount; }
    public double getLocalDensity() { return localDensity; }
    public double getClusteringCoefficient() { return clusteringCoefficient; }
    
    public boolean hasNeighbor(String cellId) {
        return neighborIds.contains(cellId);
    }
    
    public boolean isIsolated() {
        return neighborCount == 0;
    }
    
    public boolean isHighlyConnected(int threshold) {
        return neighborCount >= threshold;
    }
    
    @Override
    public String toString() {
        return String.format("SpatialMetrics{neighbors=%d, nearestDist=%.2f, density=%.3f}", 
                           neighborCount, nearestNeighborDistance, localDensity);
    }
}