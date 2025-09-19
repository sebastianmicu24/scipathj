package com.scipath.scipathj.dataset.model;

import com.scipath.scipathj.infrastructure.roi.UserROI;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Immutable record representing a complete training dataset.
 * Contains ROIs, extracted features, class information, and configuration.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public record TrainingDataset(
    Map<String, UserROI> classifiedROIs,
    Map<String, Map<String, Double>> features,
    List<ClassificationClass> classes,
    DatasetConfiguration configuration
) {
    
    /**
     * Validates the dataset parameters.
     */
    public TrainingDataset {
        if (classifiedROIs == null || classifiedROIs.isEmpty()) {
            throw new IllegalArgumentException("Classified ROIs cannot be null or empty");
        }
        if (features == null || features.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be null or empty");
        }
        if (classes == null || classes.isEmpty()) {
            throw new IllegalArgumentException("Classes cannot be null or empty");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        // Validate that all ROIs have corresponding features
        for (String roiKey : classifiedROIs.keySet()) {
            if (!features.containsKey(roiKey)) {
                throw new IllegalArgumentException("Missing features for ROI: " + roiKey);
            }
        }
        
        // Make collections immutable
        classifiedROIs = Collections.unmodifiableMap(classifiedROIs);
        features = Collections.unmodifiableMap(features);
        classes = Collections.unmodifiableList(classes);
    }
    
    /**
     * Returns the number of ROIs in this dataset.
     */
    public int getROICount() {
        return classifiedROIs.size();
    }
    
    /**
     * Returns the number of features per ROI.
     */
    public int getFeatureCount() {
        if (features.isEmpty()) {
            return 0;
        }
        return features.values().iterator().next().size();
    }
    
    /**
     * Returns the number of classification classes.
     */
    public int getClassCount() {
        return classes.size();
    }
    
    /**
     * Returns all feature names from the first ROI.
     */
    public List<String> getFeatureNames() {
        if (features.isEmpty()) {
            return List.of();
        }
        return List.copyOf(features.values().iterator().next().keySet());
    }
    
    /**
     * Gets class distribution statistics.
     */
    public Map<String, Long> getClassDistribution() {
        return classifiedROIs.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                roi -> roi.getAssignedClass() != null ? roi.getAssignedClass() : "Unclassified",
                java.util.stream.Collectors.counting()
            ));
    }
    
    /**
     * Returns a summary string of the dataset.
     */
    public String getSummary() {
        return String.format("TrainingDataset[ROIs=%d, Features=%d, Classes=%d]",
                           getROICount(), getFeatureCount(), getClassCount());
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}