package com.scipath.scipathj.ui.analysis;

import com.scipath.scipathj.analysis.algorithms.classification.CellClassification;
import com.scipath.scipathj.infrastructure.roi.DefaultROIService;
import com.scipath.scipathj.infrastructure.roi.ROIService;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis-specific ROI manager that extends the core ROI service with 
 * analysis features like classification results and measurement data.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 */
public class AnalysisROIManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisROIManager.class);
    
    // Core ROI service for basic operations
    private final ROIService roiService;
    
    // Analysis-specific data
    private final Map<String, CellClassification.ClassificationResult> classificationResults = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> measurementData = new ConcurrentHashMap<>();
    
    // Listeners for analysis-specific events
    private final List<AnalysisROIListener> analysisListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Interface for listening to analysis-specific ROI events.
     */
    public interface AnalysisROIListener {
        void onClassificationUpdated(String roiKey, CellClassification.ClassificationResult result);
        void onMeasurementUpdated(String roiKey, Map<String, Double> measurements);
        void onROIValidationChanged(UserROI roi, boolean isValid);
    }
    
    public AnalysisROIManager() {
        this.roiService = new DefaultROIService();
        LOGGER.debug("Created AnalysisROIManager");
    }
    
    // === CORE ROI OPERATIONS (delegated to service) ===
    
    public void addROI(UserROI roi) {
        roiService.addROI(roi);
        LOGGER.debug("Added ROI '{}' to analysis", roi.getName());
    }
    
    public boolean removeROI(String roiId) {
        // Clean up analysis-specific data
        String roiKey = findROIKeyById(roiId);
        if (roiKey != null) {
            classificationResults.remove(roiKey);
            measurementData.remove(roiKey);
        }
        
        boolean removed = roiService.removeROI(roiId);
        if (removed) {
            LOGGER.debug("Removed ROI '{}' from analysis with all associated data", roiId);
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
        // Clean up analysis-specific data for this image
        List<UserROI> rois = roiService.getROIsForImage(imageFileName);
        for (UserROI roi : rois) {
            String roiKey = generateROIKey(imageFileName, roi.getName());
            classificationResults.remove(roiKey);
            measurementData.remove(roiKey);
        }
        
        roiService.clearROIsForImage(imageFileName);
        LOGGER.debug("Cleared all ROIs and analysis data for image '{}'", imageFileName);
    }
    
    public void clearAllROIs() {
        classificationResults.clear();
        measurementData.clear();
        roiService.clearAllROIs();
        LOGGER.debug("Cleared all ROIs and analysis data");
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
    
    // === ANALYSIS-SPECIFIC OPERATIONS ===
    
    /**
     * Set classification results for ROIs.
     */
    public void setClassificationResults(Map<String, CellClassification.ClassificationResult> results) {
        if (results != null) {
            this.classificationResults.putAll(results);
            LOGGER.info("Updated classification results for {} ROIs", results.size());
            
            // Notify listeners
            results.forEach((roiKey, result) -> 
                notifyAnalysisListeners(listener -> listener.onClassificationUpdated(roiKey, result)));
        }
    }
    
    /**
     * Get classification result for a specific ROI.
     */
    public CellClassification.ClassificationResult getClassificationResult(String roiKey) {
        return classificationResults.get(roiKey);
    }
    
    /**
     * Get classification tooltip text for a specific ROI.
     */
    public String getClassificationTooltipText(String roiKey) {
        CellClassification.ClassificationResult result = classificationResults.get(roiKey);
        if (result != null) {
            return String.format("Cell type: %s (confidence: %.1f%%)",
                result.getPredictedClass(),
                result.getConfidence() * 100);
        }
        return "Cell type: (not classified)";
    }
    
    /**
     * Set measurement data for a specific ROI.
     */
    public void setMeasurementData(String roiKey, Map<String, Double> measurements) {
        if (measurements != null) {
            measurementData.put(roiKey, measurements);
            LOGGER.debug("Updated measurements for ROI '{}'", roiKey);
            
            // Notify listeners
            notifyAnalysisListeners(listener -> listener.onMeasurementUpdated(roiKey, measurements));
        }
    }
    
    /**
     * Get measurement data for a specific ROI.
     */
    public Map<String, Double> getMeasurementData(String roiKey) {
        return measurementData.get(roiKey);
    }
    
    /**
     * Get all classification results.
     */
    public Map<String, CellClassification.ClassificationResult> getAllClassificationResults() {
        return new ConcurrentHashMap<>(classificationResults);
    }
    
    /**
     * Get all measurement data.
     */
    public Map<String, Map<String, Double>> getAllMeasurementData() {
        return new ConcurrentHashMap<>(measurementData);
    }
    
    /**
     * Clear all classification results.
     */
    public void clearClassificationResults() {
        classificationResults.clear();
        LOGGER.info("Cleared all classification results");
    }
    
    /**
     * Clear all measurement data.
     */
    public void clearMeasurementData() {
        measurementData.clear();
        LOGGER.info("Cleared all measurement data");
    }
    
    /**
     * Mark an ROI as ignored/valid for analysis.
     */
    public void setROIValidation(UserROI roi, boolean isValid) {
        roi.setIgnored(!isValid);
        LOGGER.debug("Set ROI '{}' validation to: {}", roi.getName(), isValid);
        
        // Notify listeners
        notifyAnalysisListeners(listener -> listener.onROIValidationChanged(roi, isValid));
    }
    
    /**
     * Get ROIs filtered by validation status.
     */
    public List<UserROI> getValidROIsForImage(String imageFileName) {
        return getROIsForImage(imageFileName).stream()
            .filter(roi -> !roi.isIgnored())
            .toList();
    }
    
    /**
     * Get ROIs filtered by classification status.
     */
    public List<UserROI> getClassifiedROIsForImage(String imageFileName) {
        return getROIsForImage(imageFileName).stream()
            .filter(roi -> {
                String roiKey = generateROIKey(imageFileName, roi.getName());
                return classificationResults.containsKey(roiKey);
            })
            .toList();
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
    
    public void addAnalysisListener(AnalysisROIListener listener) {
        if (listener != null) {
            analysisListeners.add(listener);
        }
    }
    
    public void removeAnalysisListener(AnalysisROIListener listener) {
        analysisListeners.remove(listener);
    }
    
    public void addROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.addChangeListener(listener);
    }
    
    public void removeROIChangeListener(ROIService.ROIChangeListener listener) {
        roiService.removeChangeListener(listener);
    }
    
    // === HELPER METHODS ===
    
    private void notifyAnalysisListeners(java.util.function.Consumer<AnalysisROIListener> action) {
        analysisListeners.forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                LOGGER.error("Error notifying analysis ROI listener", e);
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
     * Get statistics about the analysis ROIs.
     */
    public AnalysisStatistics getAnalysisStatistics() {
        int totalROIs = roiService.getTotalROICount();
        int classifiedROIs = classificationResults.size();
        int validROIs = (int) roiService.getAllROIs().stream()
            .filter(roi -> !roi.isIgnored())
            .count();
        
        return new AnalysisStatistics(totalROIs, classifiedROIs, validROIs);
    }
    
    /**
     * Simple record for analysis statistics.
     */
    public record AnalysisStatistics(int totalROIs, int classifiedROIs, int validROIs) {
        public double getClassificationRate() {
            return totalROIs > 0 ? (double) classifiedROIs / totalROIs : 0.0;
        }
        
        public double getValidationRate() {
            return totalROIs > 0 ? (double) validROIs / totalROIs : 0.0;
        }
    }
}