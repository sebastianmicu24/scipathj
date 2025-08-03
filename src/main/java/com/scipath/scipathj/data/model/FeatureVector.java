package com.scipath.scipathj.data.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Feature vector containing extracted features for a cell
 */
public class FeatureVector {
    private final Map<String, Double> features;
    
    public FeatureVector() {
        this.features = new HashMap<>();
    }
    
    public FeatureVector(Map<String, Double> features) {
        this.features = new HashMap<>(features);
    }
    
    public void setFeature(String name, double value) {
        features.put(name, value);
    }
    
    public double getFeature(String name) {
        return features.getOrDefault(name, 0.0);
    }
    
    public boolean hasFeature(String name) {
        return features.containsKey(name);
    }
    
    public Set<String> getFeatureNames() {
        return features.keySet();
    }
    
    public Map<String, Double> getAllFeatures() {
        return new HashMap<>(features);
    }
    
    public int getFeatureCount() {
        return features.size();
    }
    
    public double[] toArray() {
        return features.values().stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    public double[] toArray(String[] featureOrder) {
        double[] result = new double[featureOrder.length];
        for (int i = 0; i < featureOrder.length; i++) {
            result[i] = getFeature(featureOrder[i]);
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "FeatureVector{" + features.size() + " features}";
    }
}