package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.ui.dataset.controller.ROICategorizer.ROICategorizedData;
import ij.ImagePlus;

import java.util.Map;

/**
 * Interface defining feature extraction capabilities.
 * Follows Interface Segregation Principle - clients only depend on methods they use.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public interface FeatureExtractionEngine {
    
    /**
     * Extract features from categorized ROI data.
     * 
     * @param categorizedData The categorized ROI data
     * @param imagePlus The image data for feature extraction
     * @return Map of feature keys to feature data
     * @throws Exception if feature extraction fails
     */
    Map<String, Map<String, Object>> extractFeatures(
            ROICategorizedData categorizedData, 
            ImagePlus imagePlus) throws Exception;
    
    /**
     * Check if this engine is available and properly initialized.
     * 
     * @return true if the engine is ready to extract features
     */
    boolean isAvailable();
    
    /**
     * Get the name of this feature extraction engine.
     * 
     * @return A descriptive name for this engine
     */
    String getEngineName();
    
    /**
     * Validate that the extracted features contain required fields.
     * 
     * @param features The extracted features to validate
     * @throws Exception if validation fails
     */
    void validateFeatures(Map<String, Map<String, Object>> features) throws Exception;
}