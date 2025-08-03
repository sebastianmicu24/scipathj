package com.scipath.scipathj.data.model;

import java.util.Map;
import java.util.HashMap;

/**
 * Result of cell classification containing predicted class and confidence scores
 */
public class ClassificationResult {
    private final String predictedClass;
    private final double confidence;
    private final Map<String, Double> classScores;
    private final String modelName;
    private final String modelVersion;
    
    public ClassificationResult(String predictedClass, double confidence, 
                              Map<String, Double> classScores, String modelName, String modelVersion) {
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.classScores = new HashMap<>(classScores);
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }
    
    public ClassificationResult(String predictedClass, double confidence) {
        this(predictedClass, confidence, new HashMap<>(), "Unknown", "1.0");
    }
    
    // Getters
    public String getPredictedClass() { return predictedClass; }
    public double getConfidence() { return confidence; }
    public Map<String, Double> getClassScores() { return new HashMap<>(classScores); }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    
    public double getScoreForClass(String className) {
        return classScores.getOrDefault(className, 0.0);
    }
    
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    public String getSecondBestClass() {
        return classScores.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(predictedClass))
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }
    
    @Override
    public String toString() {
        return String.format("ClassificationResult{class='%s', confidence=%.3f}", 
                           predictedClass, confidence);
    }
}