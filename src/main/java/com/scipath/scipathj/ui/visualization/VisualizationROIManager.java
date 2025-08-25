package com.scipath.scipathj.ui.visualization;

import com.scipath.scipathj.infrastructure.roi.DefaultROIService;
import com.scipath.scipathj.infrastructure.roi.ROIService;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visualization-specific ROI manager that extends the core ROI service with 
 * visualization features like custom coloring schemes and statistical display.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class VisualizationROIManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationROIManager.class);
    
    // Core ROI service for basic operations
    private final ROIService roiService;
    
    // Visualization-specific data
    private final Map<String, Map<String, Object>> roiProperties = new ConcurrentHashMap<>();
    private final Map<String, Color> customColorMapping = new ConcurrentHashMap<>();
    private Function<UserROI, Color> colorSchemeFunction;
    
    // Feature visualization state
    private String activeFeature;
    private double featureMinValue = Double.MAX_VALUE;
    private double featureMaxValue = Double.MIN_VALUE;
    private ColorScheme activeColorScheme = ColorScheme.DEFAULT;
    
    // Listeners for visualization-specific events
    private final List<VisualizationROIListener> visualizationListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Available color schemes for visualization.
     */
    public enum ColorScheme {
        DEFAULT("Default"),
        HEAT_MAP("Heat Map"),
        CLASSIFICATION("Classification"),
        FEATURE_BASED("Feature Based"),
        CUSTOM("Custom");
        
        private final String displayName;
        
        ColorScheme(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Interface for listening to visualization-specific ROI events.
     */
    public interface VisualizationROIListener {
        void onColorSchemeChanged(ColorScheme scheme);
        void onFeatureVisualizationChanged(String feature);
        void onCustomColorApplied(String roiKey, Color color);
        void onVisualizationDataUpdated();
    }
    
    public VisualizationROIManager() {
        this.roiService = new DefaultROIService();
        setupDefaultColorScheme();
        LOGGER.debug("Created VisualizationROIManager");
    }
    
    // === CORE ROI OPERATIONS (delegated to service) ===
    
    public void addROI(UserROI roi) {
        roiService.addROI(roi);
        LOGGER.debug("Added ROI '{}' to visualization", roi.getName());
    }
    
    public boolean removeROI(String roiId) {
        // Clean up visualization-specific data
        String roiKey = findROIKeyById(roiId);
        if (roiKey != null) {
            roiProperties.remove(roiKey);
            customColorMapping.remove(roiKey);
        }
        
        boolean removed = roiService.removeROI(roiId);
        if (removed) {
            LOGGER.debug("Removed ROI '{}' from visualization with all properties", roiId);
        }
        return removed;
    }
    
    public List<UserROI> getROIsForImage(String imageFileName) {
        return roiService.getROIsForImage(imageFileName);
    }
    
    public List<UserROI> getAllROIs() {
        return roiService.getAllROIs();
    }
    
    public void clearROIsForImage(String imageFileName) {
        // Clean up visualization-specific data for this image
        List<UserROI> rois = roiService.getROIsForImage(imageFileName);
        for (UserROI roi : rois) {
            String roiKey = generateROIKey(imageFileName, roi.getName());
            roiProperties.remove(roiKey);
            customColorMapping.remove(roiKey);
        }
        
        roiService.clearROIsForImage(imageFileName);
        LOGGER.debug("Cleared all ROIs and visualization data for image '{}'", imageFileName);
    }
    
    public void clearAllROIs() {
        roiProperties.clear();
        customColorMapping.clear();
        roiService.clearAllROIs();
        LOGGER.debug("Cleared all ROIs and visualization data");
    }
    
    public int getROICount(String imageFileName) {
        return roiService.getROICount(imageFileName);
    }
    
    public boolean hasROIs(String imageFileName) {
        return roiService.hasROIs(imageFileName);
    }
    
    public UserROI findROIById(String roiId) {
        return roiService.findROIById(roiId);
    }
    
    // === VISUALIZATION-SPECIFIC OPERATIONS ===
    
    /**
     * Set properties for an ROI (measurements, features, etc.).
     */
    public void setROIProperties(String roiKey, Map<String, Object> properties) {
        if (properties != null) {
            roiProperties.put(roiKey, new HashMap<>(properties));
            updateFeatureRange(properties);
            notifyVisualizationListeners(listener -> listener.onVisualizationDataUpdated());
            LOGGER.debug("Updated properties for ROI '{}'", roiKey);
        }
    }
    
    /**
     * Get properties for an ROI.
     */
    public Map<String, Object> getROIProperties(String roiKey) {
        return roiProperties.getOrDefault(roiKey, Collections.emptyMap());
    }
    
    /**
     * Set custom color for an ROI.
     */
    public void setCustomColor(String roiKey, Color color) {
        if (color != null) {
            customColorMapping.put(roiKey, color);
            notifyVisualizationListeners(listener -> listener.onCustomColorApplied(roiKey, color));
            LOGGER.debug("Applied custom color to ROI '{}'", roiKey);
        }
    }
    
    /**
     * Get custom color for an ROI.
     */
    public Color getCustomColor(String roiKey) {
        return customColorMapping.get(roiKey);
    }
    
    /**
     * Set active color scheme.
     */
    public void setColorScheme(ColorScheme scheme) {
        this.activeColorScheme = scheme;
        updateColorSchemeFunction();
        notifyVisualizationListeners(listener -> listener.onColorSchemeChanged(scheme));
        LOGGER.debug("Changed color scheme to: {}", scheme);
    }
    
    /**
     * Get active color scheme.
     */
    public ColorScheme getActiveColorScheme() {
        return activeColorScheme;
    }
    
    /**
     * Set feature for visualization.
     */
    public void setActiveFeature(String feature) {
        this.activeFeature = feature;
        updateFeatureRange();
        notifyVisualizationListeners(listener -> listener.onFeatureVisualizationChanged(feature));
        LOGGER.debug("Changed active feature to: {}", feature);
    }
    
    /**
     * Get active feature.
     */
    public String getActiveFeature() {
        return activeFeature;
    }
    
    /**
     * Get color for ROI based on current scheme.
     */
    public Color getROIColor(UserROI roi) {
        String roiKey = generateROIKey(roi.getImageFileName(), roi.getName());
        
        if (colorSchemeFunction != null) {
            return colorSchemeFunction.apply(roi);
        }
        
        return roi.getDisplayColor();
    }
    
    /**
     * Get available features for visualization.
     */
    public Set<String> getAvailableFeatures() {
        return roiProperties.values().stream()
            .flatMap(props -> props.keySet().stream())
            .filter(key -> roiProperties.values().stream()
                .anyMatch(props -> props.get(key) instanceof Number))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get statistics for a feature.
     */
    public FeatureStatistics getFeatureStatistics(String feature) {
        if (feature == null) {
            return null;
        }
        
        List<Double> values = roiProperties.values().stream()
            .map(props -> props.get(feature))
            .filter(value -> value instanceof Number)
            .map(value -> ((Number) value).doubleValue())
            .toList();
        
        if (values.isEmpty()) {
            return null;
        }
        
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return new FeatureStatistics(feature, min, max, mean, values.size());
    }
    
    // === FILE OPERATIONS ===
    
    public void saveROIsToFile(String imageFileName, File outputFile) throws IOException {
        roiService.saveROIsToFile(imageFileName, outputFile);
    }
    
    public void saveAllROIsToMasterZip(File outputFile) throws IOException {
        roiService.saveAllROIsToMasterZip(outputFile);
    }
    
    public List<UserROI> loadROIsFromFile(File inputFile, String imageFileName) throws IOException {
        return roiService.loadROIsFromFile(inputFile, imageFileName);
    }
    
    // === LISTENER MANAGEMENT ===
    
    public void addVisualizationListener(VisualizationROIListener listener) {
        if (listener != null) {
            visualizationListeners.add(listener);
        }
    }
    
    public void removeVisualizationListener(VisualizationROIListener listener) {
        visualizationListeners.remove(listener);
    }
    
    public void addROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.addChangeListener(listener);
    }
    
    public void removeROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.removeChangeListener(listener);
    }
    
    // === HELPER METHODS ===
    
    private void setupDefaultColorScheme() {
        colorSchemeFunction = roi -> roi.getDisplayColor();
    }
    
    private void updateColorSchemeFunction() {
        switch (activeColorScheme) {
            case HEAT_MAP:
                colorSchemeFunction = this::getHeatMapColor;
                break;
            case FEATURE_BASED:
                colorSchemeFunction = this::getFeatureBasedColor;
                break;
            case CUSTOM:
                colorSchemeFunction = this::getCustomColorOrDefault;
                break;
            case CLASSIFICATION:
                colorSchemeFunction = this::getClassificationColor;
                break;
            default:
                setupDefaultColorScheme();
                break;
        }
    }
    
    private Color getHeatMapColor(UserROI roi) {
        if (activeFeature == null) {
            return roi.getDisplayColor();
        }
        
        String roiKey = generateROIKey(roi.getImageFileName(), roi.getName());
        Map<String, Object> props = roiProperties.get(roiKey);
        if (props == null) {
            return roi.getDisplayColor();
        }
        
        Object value = props.get(activeFeature);
        if (!(value instanceof Number)) {
            return roi.getDisplayColor();
        }
        
        double normalizedValue = normalizeValue(((Number) value).doubleValue());
        return createHeatMapColor(normalizedValue);
    }
    
    private Color getFeatureBasedColor(UserROI roi) {
        // Similar to heat map but with different color mapping
        return getHeatMapColor(roi);
    }
    
    private Color getCustomColorOrDefault(UserROI roi) {
        String roiKey = generateROIKey(roi.getImageFileName(), roi.getName());
        Color customColor = customColorMapping.get(roiKey);
        return customColor != null ? customColor : roi.getDisplayColor();
    }
    
    private Color getClassificationColor(UserROI roi) {
        // This would use classification results if available
        return roi.getDisplayColor();
    }
    
    private Color createHeatMapColor(double normalizedValue) {
        // Create heat map color from blue (low) to red (high)
        int red = (int) (255 * normalizedValue);
        int blue = (int) (255 * (1.0 - normalizedValue));
        return new Color(red, 0, blue, 150);
    }
    
    private double normalizeValue(double value) {
        if (featureMaxValue == featureMinValue) {
            return 0.5; // Middle value if no range
        }
        return (value - featureMinValue) / (featureMaxValue - featureMinValue);
    }
    
    private void updateFeatureRange() {
        updateFeatureRange(null);
    }
    
    private void updateFeatureRange(Map<String, Object> newProperties) {
        if (activeFeature == null) {
            return;
        }
        
        // Reset range
        featureMinValue = Double.MAX_VALUE;
        featureMaxValue = Double.MIN_VALUE;
        
        // Calculate range from all properties
        for (Map<String, Object> props : roiProperties.values()) {
            Object value = props.get(activeFeature);
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                featureMinValue = Math.min(featureMinValue, numValue);
                featureMaxValue = Math.max(featureMaxValue, numValue);
            }
        }
        
        // Include new properties if provided
        if (newProperties != null) {
            Object value = newProperties.get(activeFeature);
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                featureMinValue = Math.min(featureMinValue, numValue);
                featureMaxValue = Math.max(featureMaxValue, numValue);
            }
        }
    }
    
    private void notifyVisualizationListeners(java.util.function.Consumer<VisualizationROIListener> action) {
        visualizationListeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying visualization ROI listener", e);
            }
        });
    }
    
    private String generateROIKey(String imageFileName, String roiName) {
        return imageFileName + "_" + roiName;
    }
    
    private String findROIKeyById(String roiId) {
        UserROI roi = roiService.findROIById(roiId);
        if (roi != null) {
            return generateROIKey(roi.getImageFileName(), roi.getName());
        }
        return null;
    }
    
    /**
     * Simple record for feature statistics.
     */
    public record FeatureStatistics(String feature, double min, double max, double mean, int count) {
        public double getRange() {
            return max - min;
        }
    }
}