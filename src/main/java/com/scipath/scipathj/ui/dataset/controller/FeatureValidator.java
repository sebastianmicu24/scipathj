package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.ui.dataset.controller.ROICategorizer.ROICategorizedData;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for validating feature extraction results and prerequisites.
 * Follows Single Responsibility Principle - only handles validation logic.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class FeatureValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureValidator.class);

    /**
     * Validate that ROI categorization is suitable for training data extraction.
     *
     * @param categorizedData The categorized ROI data
     * @throws Exception if validation fails
     */
    public void validateCategorizedData(ROICategorizedData categorizedData) throws Exception {
        if (categorizedData == null) {
            throw new Exception("ROI categorized data cannot be null");
        }

        // Check if we have any ROIs at all
        int totalROIs = categorizedData.cells.size() + categorizedData.nuclei.size() + categorizedData.cytoplasm.size();
        if (totalROIs == 0) {
            throw new Exception("No ROIs found for feature extraction. Need at least some Cell, Nucleus, or Cytoplasm ROIs.");
        }

        // Check if we have classified ROIs for training
        int classifiedROIs = categorizedData.classifiedCells.size() + 
                           categorizedData.classifiedNuclei.size() + 
                           categorizedData.classifiedCytoplasms.size();
        if (classifiedROIs == 0) {
            throw new Exception("No manually classified ROIs found for training data extraction. " +
                              "Please classify some ROIs before extracting training data.");
        }

        LOGGER.info("Validation passed: {} total ROIs, {} classified ROIs", totalROIs, classifiedROIs);
    }

    /**
     * Validate that extracted features contain comprehensive analysis results.
     *
     * @param features The extracted features to validate
     * @throws Exception if validation fails
     */
    public void validateComprehensiveFeatures(Map<String, Map<String, Object>> features) throws Exception {
        if (features == null || features.isEmpty()) {
            throw new Exception("VALIDATION FAILED: No features extracted");
        }

        // Required comprehensive features that should be present for good training data
        String[] criticalFeatures = {
            "vessel_distance", "neighbor_count", "hema_mean", "eosin_mean"
        };

        String[] basicMorphologyFeatures = {
            "area", "perimeter", "circularity", "major", "minor"
        };

        // Check a sample of features to ensure extraction worked
        boolean foundComprehensiveFeatures = false;
        boolean foundBasicFeatures = false;
        Map<String, Object> sampleFeatures = null;
        
        for (Map<String, Object> featureSet : features.values()) {
            if (featureSet != null && !featureSet.isEmpty()) {
                sampleFeatures = featureSet;
                
                // Check for comprehensive features
                for (String criticalFeature : criticalFeatures) {
                    if (featureSet.containsKey(criticalFeature)) {
                        foundComprehensiveFeatures = true;
                        break;
                    }
                }
                
                // Check for basic morphology features
                for (String basicFeature : basicMorphologyFeatures) {
                    if (featureSet.containsKey(basicFeature)) {
                        foundBasicFeatures = true;
                        break;
                    }
                }
                
                if (foundComprehensiveFeatures || foundBasicFeatures) break;
            }
        }

        if (!foundBasicFeatures && !foundComprehensiveFeatures) {
            throw new Exception("VALIDATION FAILED: No valid features found in extracted data");
        }

        if (!foundComprehensiveFeatures && sampleFeatures != null) {
            LOGGER.warn("⚠ VALIDATION WARNING: Only basic features found. Missing comprehensive features like vessel_distance, H&E deconvolution, etc.");
            LOGGER.warn("Available features: {}", sampleFeatures.keySet());
            LOGGER.warn("This may result in lower quality training data. Check feature extraction configuration.");
        }

        if (foundComprehensiveFeatures) {
            LOGGER.info("✓ VALIDATION PASSED: Comprehensive features detected including H&E deconvolution and spatial analysis");
        } else {
            LOGGER.info("✓ VALIDATION PASSED: Basic morphological features detected");
        }
    }

    /**
     * Validate organized training data structure.
     *
     * @param organizedData The organized cell-based data
     * @throws Exception if validation fails
     */
    public void validateOrganizedData(Map<String, Map<String, Object>> organizedData) throws Exception {
        if (organizedData == null || organizedData.isEmpty()) {
            throw new Exception("VALIDATION FAILED: No organized data found");
        }

        int cellsWithFeatures = 0;
        int cellsWithoutFeatures = 0;

        // Check that we have properly structured cell data
        for (Map.Entry<String, Map<String, Object>> cellEntry : organizedData.entrySet()) {
            String cellId = cellEntry.getKey();
            Map<String, Object> cellData = cellEntry.getValue();
            
            if (cellData == null || cellData.isEmpty()) {
                LOGGER.debug("Cell {} has no data, skipping", cellId);
                cellsWithoutFeatures++;
                continue;
            }

            // Check for required metadata (but don't fail if missing, just log)
            if (!cellData.containsKey("Class")) {
                LOGGER.debug("Cell {} missing class assignment", cellId);
            }

            if (!cellData.containsKey("Image")) {
                LOGGER.debug("Cell {} missing image information", cellId);
            }

            // Check that we have at least one ROI type with features
            boolean hasROIFeatures = false;
            for (String key : cellData.keySet()) {
                if ("Nucleus".equals(key) || "Cytoplasm".equals(key) || "Cell".equals(key)) {
                    Object roiData = cellData.get(key);
                    if (roiData instanceof Map && !((Map<?, ?>) roiData).isEmpty()) {
                        hasROIFeatures = true;
                        LOGGER.debug("Cell {} has {} features: {}", cellId, key, ((Map<?, ?>) roiData).size());
                        break;
                    }
                }
            }

            if (hasROIFeatures) {
                cellsWithFeatures++;
            } else {
                cellsWithoutFeatures++;
                LOGGER.debug("Cell {} has no ROI features - available keys: {}", cellId, cellData.keySet());
            }
        }

        LOGGER.info("Validation summary: {} cells with features, {} cells without features",
                   cellsWithFeatures, cellsWithoutFeatures);

        if (cellsWithFeatures == 0) {
            throw new Exception("VALIDATION FAILED: No cells have any ROI features. " +
                              "Check feature extraction and organization logic.");
        }

        LOGGER.info("✓ VALIDATION PASSED: Organized data structure is valid for {} cells with features", cellsWithFeatures);
    }

    /**
     * Validate filtered training data.
     *
     * @param filteredData The filtered training data
     * @throws Exception if validation fails
     */
    public void validateFilteredData(Map<String, Map<String, Object>> filteredData) throws Exception {
        if (filteredData == null || filteredData.isEmpty()) {
            throw new Exception("VALIDATION FAILED: No classified cells found after filtering. " +
                              "Please ensure ROIs are properly classified before extracting training data.");
        }

        // Check class distribution
        Map<String, Integer> classDistribution = new HashMap<>();
        for (Map<String, Object> cellData : filteredData.values()) {
            Object classObj = cellData.get("Class");
            if (classObj != null) {
                String className = classObj.toString();
                classDistribution.put(className, classDistribution.getOrDefault(className, 0) + 1);
            }
        }

        if (classDistribution.size() < 2) {
            LOGGER.warn("⚠ VALIDATION WARNING: Only {} unique classes found. " +
                       "For good training results, multiple classes are recommended.", classDistribution.size());
        }

        LOGGER.info("✓ VALIDATION PASSED: {} classified cells with {} classes: {}", 
                   filteredData.size(), classDistribution.size(), classDistribution);
    }

    /**
     * Perform comprehensive validation of the entire training data extraction process.
     *
     * @param categorizedData The categorized ROI data
     * @param extractedFeatures The extracted features
     * @param organizedData The organized cell data
     * @param filteredData The filtered training data
     * @throws Exception if any validation step fails
     */
    public void validateCompleteWorkflow(
            ROICategorizedData categorizedData,
            Map<String, Map<String, Object>> extractedFeatures,
            Map<String, Map<String, Object>> organizedData,
            Map<String, Map<String, Object>> filteredData) throws Exception {
        
        LOGGER.info("Performing comprehensive validation of training data extraction workflow");

        validateCategorizedData(categorizedData);
        validateComprehensiveFeatures(extractedFeatures);
        validateOrganizedData(organizedData);
        validateFilteredData(filteredData);

        LOGGER.info("✓ COMPREHENSIVE VALIDATION PASSED: Training data extraction workflow is valid");
    }
}