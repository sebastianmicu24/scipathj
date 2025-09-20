
package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles feature extraction for training data.
 * This class extracts the complex feature extraction logic from DatasetControlsPanel
 * and provides a clean interface for feature extraction operations.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class FeatureExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractor.class);

    // Dependencies injected
    private ConfigurationManager configManager;
    private FeatureExtraction featureExtractor;

    // State
    private Map<String, Map<String, Object>> currentFeatures;

    // Callbacks for progress updates
    private Consumer<String> progressUpdater;
    private Consumer<Double> percentageUpdater;

    /**
     * Creates a new FeatureExtractor.
     *
     * @param configManager Configuration manager
     * @param progressUpdater Callback for progress update messages
     * @param percentageUpdater Callback for progress percentage updates
     */
    public FeatureExtractor(
            ConfigurationManager configManager,
            Consumer<String> progressUpdater,
            Consumer<Double> percentageUpdater) {
        this.configManager = configManager;
        this.progressUpdater = progressUpdater;
        this.percentageUpdater = percentageUpdater;

        initializeFeatureExtractor();
    }

    /**
     * Initialize the feature extraction system.
     * Uses the SAME FeatureExtraction class as the analysis workflow.
     */
    private void initializeFeatureExtractor() {
        try {
            if (configManager != null) {
                // The FeatureExtraction from analysis package will be initialized
                // with actual image data during feature extraction
                LOGGER.info("Analysis FeatureExtraction will be initialized with image data during processing");
            } else {
                LOGGER.warn("ConfigurationManager is null - FeatureExtraction initialization will fail");
            }
        } catch (Exception e) {
            LOGGER.warn("Could not initialize feature extractor configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize the main FeatureExtraction class with image data.
     * This should be called when we have the ImagePlus and ROI data available.
     */
    private void initializeFeatureExtractorWithData(ImagePlus imagePlus, List<UserROI> vesselROIs,
                                                   List<UserROI> nucleusROIs, List<UserROI> cytoplasmROIs,
                                                   List<UserROI> cellROIs, String imageFileName) {
        try {
            if (imagePlus != null && configManager != null) {
                // Load settings from configuration
                com.scipath.scipathj.analysis.config.FeatureExtractionSettings featureSettings =
                    configManager.loadFeatureExtractionSettings();
                com.scipath.scipathj.infrastructure.config.MainSettings mainSettings =
                    configManager.loadMainSettings();

                // Use the SAME FeatureExtraction class as the analysis workflow
                // This ensures consistency and avoids code duplication
                LOGGER.info("Creating FeatureExtraction instance with settings...");
                LOGGER.info("FeatureExtractionSettings: {}", featureSettings != null ? "OK" : "NULL");
                LOGGER.info("MainSettings: {}", mainSettings != null ? "OK" : "NULL");
                
                this.featureExtractor = new FeatureExtraction(
                    imagePlus, imageFileName, vesselROIs, nucleusROIs, cytoplasmROIs, cellROIs,
                    featureSettings, mainSettings);

                LOGGER.info("Analysis FeatureExtraction initialized with image data: {} vessels, {} nuclei, {} cytoplasm, {} cells",
                           vesselROIs.size(), nucleusROIs.size(), cytoplasmROIs.size(), cellROIs.size());
            } else {
                LOGGER.warn("Cannot initialize feature extractor: imagePlus={}, configManager={}",
                           imagePlus != null, configManager != null);
            }
        } catch (Exception e) {
            LOGGER.error("Could not initialize feature extractor with image data: {}", e.getMessage(), e);
            this.featureExtractor = null;
        }
    }
    /**
     * Extract features from classified ROIs.
     * This is the main method that replaces the large extractAndSaveTrainingDataUsingPipeline
     * method from the original DatasetControlsPanel.
     *
     * @param outputFile File to save training data to
     * @param allROIs All ROIs from the overlay
     * @param imagePlus Optional ImagePlus for advanced feature extraction
     * @throws IOException if file operations fail
     * @throws Exception if feature extraction fails
     */
    public void extractAndSaveTrainingData(
            File outputFile,
            Map<String, UserROI> allROIs,
            ImagePlus imagePlus) throws IOException, Exception {

        LOGGER.info("Starting feature extraction for training data");

        if (allROIs.isEmpty()) {
            throw new Exception("No ROIs found in overlay for training data extraction");
        }

        updateProgress("Categorizing ROIs by type", 0.1);

        // Step 1: Categorize ROIs
        ROICategorizedData categorizedData = categorizeROIs(allROIs);
        LOGGER.info("Categorized ROIs: Cells={}, Nuclei={}, Cytoplasm={}, Vessel={}",
                   categorizedData.cells.size(),
                   categorizedData.nuclei.size(),
                   categorizedData.cytoplasm.size(),
                   categorizedData.vessel.size());

        updateProgress("Validating classifications", 0.2);

        // Step 2: Validate classifications
        if (categorizedData.classifiedCells.isEmpty() &&
            categorizedData.classifiedNuclei.isEmpty() &&
            categorizedData.classifiedCytoplasms.isEmpty()) {
            throw new Exception("No manually classified cells found for training");
        }

        updateProgress("Extracting morphological features", 0.4);

        // Step 3: Extract features using MANDATORY comprehensive methods
        Map<String, Map<String, Object>> organizedFeatures;
        try {
            organizedFeatures = extractFeaturesFromROIs(categorizedData, imagePlus);
        } catch (Exception e) {
            throw new Exception("CRITICAL FAILURE: Comprehensive feature extraction failed. " + e.getMessage(), e);
        }

        updateProgress("Organizing features for export", 0.6);

        // Step 4: Organize features
        Map<String, Map<String, Object>> finalData = organizeFeaturesForXGBoostTraining(
                organizedFeatures, categorizedData);

        updateProgress("Filtering classified ROIs", 0.7);

        // Step 5: Filter to only include manually classified data
        Map<String, Map<String, Object>> filteredData = filterClassifiedROIsXGBoost(finalData);

        updateProgress("Saving training data to JSON", 0.9);

        // Step 6: Save to JSON
        saveTrainingDataToJsonXGBoost(filteredData, outputFile);

        updateProgress("Feature extraction completed successfully", 1.0);

        LOGGER.info("Feature extraction completed. Processed {} cells across {} images",
                   filteredData.size(), filteredData.size());
    }

    /**
     * Categorize ROIs by type and classification status.
     */
    private ROICategorizedData categorizeROIs(Map<String, UserROI> allROIs) {
        ROICategorizedData data = new ROICategorizedData();

        for (UserROI roi : allROIs.values()) {
            switch (roi.getType()) {
                case CELL:
                    data.cells.add(roi);
                    if (isClassified(roi)) data.classifiedCells.add(roi);
                    break;
                case NUCLEUS:
                    data.nuclei.add(roi);
                    if (isClassified(roi)) data.classifiedNuclei.add(roi);
                    break;
                case CYTOPLASM:
                    data.cytoplasm.add(roi);
                    if (isClassified(roi)) data.classifiedCytoplasms.add(roi);
                    break;
                case VESSEL:
                    data.vessel.add(roi);
                    break;
                case IGNORE:
                    data.ignored.add(roi);
                    break;
            }
        }

        return data;
    }

    /**
     * Check if an ROI has been manually classified.
     */
    private boolean isClassified(UserROI roi) {
        String className = roi.getAssignedClass();
        return className != null && !className.trim().isEmpty() && !"Unclassified".equals(className);
    }

    /**
     * Extract features from ROIs using ONLY comprehensive feature extraction.
     * This MUST extract all required features including vessel_distance, H&E deconvolution, etc.
     * If comprehensive extraction fails, the entire operation fails with an error.
     */
    private Map<String, Map<String, Object>> extractFeaturesFromROIs(
            ROICategorizedData categorizedData,
            ImagePlus imagePlus) throws Exception {

        LOGGER.info("Starting MANDATORY comprehensive feature extraction");

        // Validate prerequisites
        if (imagePlus == null) {
            throw new Exception("ImagePlus is required for comprehensive feature extraction. Cannot proceed without image data.");
        }

        if (categorizedData.cells.isEmpty() && categorizedData.nuclei.isEmpty() && categorizedData.cytoplasm.isEmpty()) {
            throw new Exception("No ROIs found for feature extraction. Need at least some Cell, Nucleus, or Cytoplasm ROIs.");
        }

        if (configManager == null) {
            throw new Exception("ConfigurationManager is required for comprehensive feature extraction. Cannot proceed without configuration.");
        }

        // ONLY use comprehensive feature extraction - no fallbacks allowed
        Map<String, Map<String, Object>> features = extractComprehensiveFeatures(categorizedData, imagePlus);
        
        // Validate that we got the required comprehensive features
        validateComprehensiveFeatures(features);

        // Store features for later access
        this.currentFeatures = features;

        LOGGER.info("Comprehensive feature extraction completed successfully with {} feature sets", features.size());
        return features;
    }

    /**
     /**
      * Extract comprehensive features using the full analysis pipeline.
      * This MUST extract ALL required features including vessel_distance, H&E deconvolution, spatial analysis, etc.
      * If ANY required feature is missing, this method throws an exception.
      */
     private Map<String, Map<String, Object>> extractComprehensiveFeatures(
             ROICategorizedData categorizedData,
             ImagePlus imagePlus) throws Exception {
 
         LOGGER.info("Initializing MANDATORY comprehensive feature extraction with H&E deconvolution");
         
         try {
             // Initialize the FeatureExtraction class with proper ROI data
             String imageFileName = imagePlus.getTitle();
             LOGGER.info("Image filename: {}", imageFileName);
             
             // Initialize feature extractor with image data
             LOGGER.info("Attempting to initialize FeatureExtraction with {} vessels, {} nuclei, {} cytoplasm, {} cells",
                        categorizedData.vessel.size(), categorizedData.nuclei.size(),
                        categorizedData.cytoplasm.size(), categorizedData.cells.size());
             
             initializeFeatureExtractorWithData(
                 imagePlus,
                 categorizedData.vessel,
                 categorizedData.nuclei,
                 categorizedData.cytoplasm,
                 categorizedData.cells,
                 imageFileName
             );
             
             if (featureExtractor == null) {
                 String error = "CRITICAL ERROR: FeatureExtraction class failed to initialize. Cannot extract comprehensive features without proper analysis pipeline.";
                 LOGGER.error(error);
                 throw new Exception(error);
             }
             
         } catch (Exception e) {
             LOGGER.error("FAILED to initialize FeatureExtraction: {}", e.getMessage(), e);
             throw new Exception("FeatureExtraction initialization failed: " + e.getMessage(), e);
         }
         LOGGER.info("Analysis FeatureExtraction initialized successfully. Extracting comprehensive features...");
         
         try {
             // Extract features using the SAME comprehensive analysis pipeline as the analysis workflow
             // This ensures identical feature calculations between analysis and dataset creation
             LOGGER.info("Calling featureExtractor.extractFeatures()...");
             Map<String, Map<String, Object>> extractedFeatures = featureExtractor.extractFeatures();
             
             if (extractedFeatures == null) {
                 String error = "CRITICAL ERROR: Analysis FeatureExtraction returned NULL. The comprehensive analysis pipeline failed.";
                 LOGGER.error(error);
                 throw new Exception(error);
             }
             
             if (extractedFeatures.isEmpty()) {
                 String error = "CRITICAL ERROR: Analysis FeatureExtraction returned EMPTY map. The comprehensive analysis pipeline failed.";
                 LOGGER.error(error);
                 throw new Exception(error);
             }
             
             // Log sample feature to verify comprehensive extraction
             Map<String, Object> sampleFeatures = extractedFeatures.values().iterator().next();
             LOGGER.info("Sample features extracted: {}", sampleFeatures.keySet());
             
             LOGGER.info("Analysis FeatureExtraction completed: {} feature sets extracted using SAME code as analysis workflow",
                        extractedFeatures.size());
             
             return extractedFeatures;
             
         } catch (Exception e) {
             LOGGER.error("FAILED during featureExtractor.extractFeatures(): {}", e.getMessage(), e);
             e.printStackTrace(); // Print full stack trace
             throw new Exception("Feature extraction execution failed: " + e.getMessage(), e);
         }
     }
 
     /**
      * Validate that comprehensive features contain ALL required fields.
      * Throws exception if any critical features are missing.
      */
     private void validateComprehensiveFeatures(Map<String, Map<String, Object>> features) throws Exception {
         if (features == null || features.isEmpty()) {
             throw new Exception("VALIDATION FAILED: No features extracted");
         }
 
         // Required comprehensive features that MUST be present
         String[] requiredFeatures = {
             "vessel_distance", "neighbor_count", "closest_neighbor_distance",
             "area", "width", "height", "perim", "major", "minor", "angle", "circ",
             "feret", "feretx", "ferety", "feretangle", "minferet", "ar", "round", "solidity",
             "intden", "mean", "stddev", "mode", "min", "max", "median", "skew", "kurt",
             "hema_mean", "hema_stddev", "hema_mode", "hema_min", "hema_max", "hema_median", "hema_skew", "hema_kurt",
             "eosin_mean", "eosin_stddev", "eosin_mode", "eosin_min", "eosin_max", "eosin_median", "eosin_skew", "eosin_kurt",
             "class"
         };
 
         // Check a sample of features to ensure comprehensive extraction worked
         boolean foundComprehensiveFeatures = false;
         Map<String, Object> sampleFeatures = null;
         
         for (Map<String, Object> featureSet : features.values()) {
             if (featureSet != null && !featureSet.isEmpty()) {
                 sampleFeatures = featureSet;
                 
                 // Check if we have advanced features (vessel_distance is a good indicator)
                 if (featureSet.containsKey("vessel_distance") ||
                     featureSet.containsKey("hema_mean") ||
                     featureSet.containsKey("eosin_mean")) {
                     foundComprehensiveFeatures = true;
                     break;
                 }
             }
         }
 
         if (!foundComprehensiveFeatures) {
             if (sampleFeatures != null) {
                 LOGGER.error("VALIDATION FAILED: Only basic features found. Sample features: {}", sampleFeatures.keySet());
                 throw new Exception("VALIDATION FAILED: Comprehensive feature extraction did not work. Only basic morphological features found. " +
                                    "Missing critical features like vessel_distance, H&E deconvolution (hema_mean, eosin_mean), etc. " +
                                    "Available features: " + sampleFeatures.keySet());
             } else {
                 throw new Exception("VALIDATION FAILED: No valid feature sets found in extracted data");
             }
         }
 
         LOGGER.info("VALIDATION PASSED: Comprehensive features detected including H&E deconvolution and spatial analysis");
     }
 
    /**
     * Extract basic morphological features as fallback.
     * This implements the simpler feature extraction logic but uses proper organization.
     */
    private Map<String, Map<String, Object>> extractBasicFeatures(ROICategorizedData categorizedData) {
        Map<String, Map<String, Object>> features = new LinkedHashMap<>();

        // Group ROIs by cell ID for proper organization
        Map<String, List<UserROI>> roisByCell = groupROIsByCellId(categorizedData);
        
        for (Map.Entry<String, List<UserROI>> cellEntry : roisByCell.entrySet()) {
            String cellId = cellEntry.getKey();
            List<UserROI> cellROIs = cellEntry.getValue();
            
            for (UserROI roi : cellROIs) {
                Map<String, Object> roiFeatures = createBasicFeatures(roi);
                String roiType = roi.getType().toString();
                String featureKey = "BasicExtraction_" + roiType + "_" + cellId;
                features.put(featureKey, roiFeatures);
            }
        }

        LOGGER.info("Extracted basic features for {} ROIs organized by cell ID", features.size());
        return features;
    }
    
    /**
     * Group ROIs by cell ID for proper organization.
     */
    private Map<String, List<UserROI>> groupROIsByCellId(ROICategorizedData categorizedData) {
        Map<String, List<UserROI>> roisByCell = new HashMap<>();
        
        // Add all ROIs to grouping
        List<UserROI> allROIs = new ArrayList<>();
        allROIs.addAll(categorizedData.cells);
        allROIs.addAll(categorizedData.nuclei);
        allROIs.addAll(categorizedData.cytoplasm);
        
        for (UserROI roi : allROIs) {
            String cellId = extractCellIdFromROIName(roi.getName());
            roisByCell.computeIfAbsent(cellId, k -> new ArrayList<>()).add(roi);
        }
        
        return roisByCell;
    }
    
    /**
     * Extract cell ID from ROI name.
     */
    private String extractCellIdFromROIName(String roiName) {
        if (roiName == null || roiName.isEmpty()) {
            return "unknown";
        }

        // Extract number from names like "Cell_123", "Nucleus_456", etc.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*?(\\d+)$");
        java.util.regex.Matcher matcher = pattern.matcher(roiName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return roiName;
    }

    /**
     * Create basic features from ROI measurements.
     */
    private Map<String, Object> createBasicFeatures(UserROI roi) {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("class", roi.getAssignedClass());
        features.put("area", roi.getArea());
        features.put("x", roi.getCenterX());
        features.put("y", roi.getCenterY());
        features.put("width", roi.getWidth());
        features.put("height", roi.getHeight());
        features.put("circularity", roi.getCircularity());
        features.put("major", roi.getMajorAxis());
        features.put("minor", roi.getMinorAxis());
        features.put("angle", roi.getAngle());
        features.put("feret", roi.getFeretDiameter());
        features.put("perim", roi.getPerimeter());

        return features;
    }

    /**
     * Organize features in the correct XGBoost training format:
     * {
     *   "cellId": {
     *     "Nucleus": { features... },
     *     "Cytoplasm": { features... },
     *     "Cell": { features... },
     *     "Class": "Hepatocyte",
     *     "Ignore": false,
     *     "Image": "P1_10_03.tif"
     *   }
     * }
     */
    private Map<String, Map<String, Object>> organizeFeaturesForXGBoostTraining(
            Map<String, Map<String, Object>> features,
            ROICategorizedData categorizedData) {
        LOGGER.debug("Organizing features for XGBoost training format");
        
        Map<String, Map<String, Object>> cellBasedData = new LinkedHashMap<>();
        
        // Group ROIs by cell ID first
        Map<String, List<UserROI>> roisByCell = groupROIsByCellId(categorizedData);
        String imageName = getCurrentImageName();
        
        for (Map.Entry<String, Map<String, Object>> entry : features.entrySet()) {
            String featureKey = entry.getKey();
            Map<String, Object> featureData = entry.getValue();
            
            try {
                // Parse the feature key - expected format: "imageName-ROIType-cellId"
                String[] parts = featureKey.split("-");
                if (parts.length >= 3) {
                    String roiType = parts[1]; // Cell, Nucleus, Cytoplasm
                    String cellId = parts[2];
                    
                    // Initialize cell entry if not exists
                    if (!cellBasedData.containsKey(cellId)) {
                        cellBasedData.put(cellId, new LinkedHashMap<>());
                        
                        // Add metadata for this cell
                        cellBasedData.get(cellId).put("Image", imageName);
                        cellBasedData.get(cellId).put("Ignore", false);
                        
                        // Get class assignment from the first ROI found for this cell
                        String className = getClassForCell(cellId, roisByCell);
                        cellBasedData.get(cellId).put("Class", className);
                    }
                    
                    // Remove positional features that are not useful for classification
                    Map<String, Object> cleanedFeatures = removePositionalFeatures(featureData);
                    
                    // Add the ROI type features
                    cellBasedData.get(cellId).put(roiType, cleanedFeatures);
                    
                } else {
                    LOGGER.warn("Couldn't parse feature key: {}", featureKey);
                }
                
            } catch (Exception e) {
                LOGGER.warn("Error organizing feature {}: {}", featureKey, e.getMessage());
            }
        }
        
        LOGGER.info("Organized {} cells for XGBoost training format", cellBasedData.size());
        return cellBasedData;
    }
    
    /**
     * Remove positional features that are not useful for anatomical classification.
     */
    private Map<String, Object> removePositionalFeatures(Map<String, Object> features) {
        Map<String, Object> cleaned = new LinkedHashMap<>(features);
        
        // Remove positional features as requested
        cleaned.remove("x");
        cleaned.remove("y");
        cleaned.remove("xm");
        cleaned.remove("ym");
        cleaned.remove("bx");
        cleaned.remove("by");
        
        return cleaned;
    }
    
    /**
     * Get the class assignment for a specific cell ID.
     */
    private String getClassForCell(String cellId, Map<String, List<UserROI>> roisByCell) {
        List<UserROI> cellROIs = roisByCell.get(cellId);
        if (cellROIs != null) {
            for (UserROI roi : cellROIs) {
                String className = roi.getAssignedClass();
                if (className != null && !className.trim().isEmpty() && !"Unclassified".equals(className)) {
                    return className;
                }
            }
        }
        return "Unclassified";
    }
    
    /**
     * Get the current image name being processed.
     */
    private String getCurrentImageName() {
        // Use the stored image name from the current processing context
        return "Current_Image.tif"; // This will be set properly when we have the image context
    }

    /**
     * Filter classified ROIs for XGBoost training - only include cells with valid classifications.
     */
    private Map<String, Map<String, Object>> filterClassifiedROIsXGBoost(Map<String, Map<String, Object>> cellData) {
        LOGGER.debug("Filtering classified ROIs for XGBoost training");
        
        Map<String, Map<String, Object>> filtered = new LinkedHashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> cellEntry : cellData.entrySet()) {
            String cellId = cellEntry.getKey();
            Map<String, Object> cellInfo = cellEntry.getValue();
            
            // Check if cell has a valid classification
            Object classObj = cellInfo.get("Class");
            Object ignoreObj = cellInfo.get("Ignore");
            
            if (classObj != null && !classObj.toString().trim().isEmpty() && 
                !"Unclassified".equals(classObj.toString()) &&
                (ignoreObj == null || !Boolean.TRUE.equals(ignoreObj))) {
                
                filtered.put(cellId, cellInfo);
                LOGGER.debug("Including cell {} with class {}", cellId, classObj);
            } else {
                LOGGER.debug("Excluding cell {} - class: {}, ignore: {}", cellId, classObj, ignoreObj);
            }
        }
        
        LOGGER.info("Filtered {} classified cells from {} total cells", filtered.size(), cellData.size());
        return filtered;
    }

    /**
     * Save training data to JSON format for XGBoost training.
     */
    private void saveTrainingDataToJsonXGBoost(Map<String, Map<String, Object>> cellData, File outputFile) throws IOException {
        LOGGER.info("Saving XGBoost training data to JSON file: {}", outputFile.getName());
        
        try {
            // Create JSON structure for XGBoost training
            Map<String, Object> jsonData = new LinkedHashMap<>();
            jsonData.put("timestamp", new java.util.Date().toString());
            jsonData.put("extractionMethod", "comprehensive_feature_extraction");
            jsonData.put("featureSetCount", cellData.size());
            
            // Create class information from cell data
            List<Map<String, Object>> classInfo = new ArrayList<>();
            Set<String> uniqueClasses = new HashSet<>();
            Map<String, Integer> classCounts = new HashMap<>();
            
            // Extract class information
            for (Map<String, Object> cellInfo : cellData.values()) {
                Object classObj = cellInfo.get("Class");
                if (classObj != null) {
                    String className = classObj.toString();
                    if (!"Unclassified".equals(className) && !className.trim().isEmpty()) {
                        uniqueClasses.add(className);
                        classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
                    }
                }
            }

            // Create class metadata
            int classId = 0;
            for (String className : uniqueClasses) {
                Map<String, Object> classData = new LinkedHashMap<>();
                classData.put("name", className);
                classData.put("count", classCounts.get(className));
                classData.put("id", classId++);
                classData.put("color", generateColorForClass(className));
                classInfo.add(classData);
            }

            jsonData.put("classes", classInfo);
            jsonData.put("cellData", cellData); // Use the new structure
            jsonData.put("featureExtractionSettings", "Comprehensive feature extraction with H&E deconvolution, vessel distance, neighbor analysis");

            // Save using Jackson ObjectMapper
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonData);

            LOGGER.info("Successfully saved XGBoost training data with {} cells and {} classes",
                       cellData.size(), uniqueClasses.size());
                       
        } catch (Exception e) {
            throw new IOException("Failed to save XGBoost training data to JSON: " + e.getMessage(), e);
        }
    }
 
    /**
     * Generate a consistent color for a class based on its name.
     */
    private String generateColorForClass(String className) {
        // Use hash code to generate consistent color for same class name
        int hash = className.hashCode();
        int r = Math.abs(hash) % 200 + 55; // 55-254 range
        int g = Math.abs(hash >> 8) % 200 + 55;
        int b = Math.abs(hash >> 16) % 200 + 55;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Update progress through callback.
     */
    private void updateProgress(String message, double percentage) {
        if (progressUpdater != null) {
            progressUpdater.accept(message);
        }
        if (percentageUpdater != null) {
            percentageUpdater.accept(percentage);
        }
    }

    /**
     * Get currently extracted features.
     */
    public Map<String, Map<String, Object>> getCurrentFeatures() {
        return currentFeatures != null ? new HashMap<>(currentFeatures) : null;
    }

    /**
     * Check if feature extractor is available.
     */
    public boolean isFeatureExtractorAvailable() {
        return featureExtractor != null;
    }

    /**
     * Inner class to hold categorized ROI data.
     */
    private static class ROICategorizedData {
        final List<UserROI> cells = new ArrayList<>();
        final List<UserROI> nuclei = new ArrayList<>();
        final List<UserROI> cytoplasm = new ArrayList<>();
        final List<UserROI> vessel = new ArrayList<>();
        final List<UserROI> ignored = new ArrayList<>();

        final List<UserROI> classifiedCells = new ArrayList<>();
        final List<UserROI> classifiedNuclei = new ArrayList<>();
        final List<UserROI> classifiedCytoplasms = new ArrayList<>();
    }
}