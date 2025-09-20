package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.ui.dataset.controller.ROICategorizer.ROICategorizedData;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for organizing feature data into the correct XGBoost training format.
 * Follows Single Responsibility Principle - only handles data organization.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrainingDataOrganizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingDataOrganizer.class);

    private final ROICategorizer roiCategorizer;

    public TrainingDataOrganizer(ROICategorizer roiCategorizer) {
        this.roiCategorizer = roiCategorizer;
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
     *
     * @param features The extracted features
     * @param categorizedData The categorized ROI data
     * @param imageName The name of the current image
     * @return Organized cell-based data
     */
    public Map<String, Map<String, Object>> organizeFeaturesForXGBoostTraining(
            Map<String, Map<String, Object>> features,
            ROICategorizedData categorizedData,
            String imageName) {
        
        LOGGER.debug("Organizing features for XGBoost training format");
        
        Map<String, Map<String, Object>> cellBasedData = new LinkedHashMap<>();
        
        // Group ROIs by cell ID first
        Map<String, List<UserROI>> roisByCell = roiCategorizer.groupROIsByCellId(categorizedData);
        
        for (Map.Entry<String, Map<String, Object>> entry : features.entrySet()) {
            String featureKey = entry.getKey();
            Map<String, Object> featureData = entry.getValue();
            
            try {
                // Parse the feature key - handle different possible formats
                String cellId = null;
                String roiType = null;
                
                // Debug the actual feature key format
                LOGGER.debug("Processing feature key: {}", featureKey);
                
                // Parse feature keys like "P1_10_03.tif_Cytoplasm_978"
                // The pattern is: imageName_ROIType_cellID
                
                // First extract ROI type and cell ID using helper methods
                roiType = extractROITypeFromKey(featureKey);
                cellId = extractCellIdFromKey(featureKey);
                
                LOGGER.debug("Parsed feature key '{}' -> cellId='{}', roiType='{}'", featureKey, cellId, roiType);
                
                // Validate extracted values
                if (cellId != null && roiType != null && isValidROIType(roiType)) {
                    // Initialize cell entry if not exists
                    if (!cellBasedData.containsKey(cellId)) {
                        cellBasedData.put(cellId, new LinkedHashMap<>());
                        
                        // Add metadata for this cell
                        cellBasedData.get(cellId).put("Image", imageName);
                        cellBasedData.get(cellId).put("Ignore", false);
                        
                        // Get class assignment from the first ROI found for this cell
                        String className = roiCategorizer.getClassForCell(cellId, roisByCell);
                        cellBasedData.get(cellId).put("Class", className);
                    }
                    
                    // Remove positional features that are not useful for classification
                    Map<String, Object> cleanedFeatures = removePositionalFeatures(featureData);
                    
                    // Add the ROI type features
                    cellBasedData.get(cellId).put(roiType, cleanedFeatures);
                    
                    LOGGER.debug("Successfully organized: cellId={}, roiType={}", cellId, roiType);
                } else {
                    LOGGER.warn("Couldn't parse feature key: {} (cellId={}, roiType={})", featureKey, cellId, roiType);
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
     *
     * @param features The original features
     * @return Cleaned features without positional data
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
     * Filter to only include manually classified ROIs for XGBoost training.
     *
     * @param cellData The organized cell data
     * @return Filtered data containing only classified cells
     */
    public Map<String, Map<String, Object>> filterClassifiedROIsForTraining(Map<String, Map<String, Object>> cellData) {
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
     * Get the current image name being processed.
     * This can be enhanced to extract from ImagePlus or other context.
     *
     * @return The current image name
     */
    public String getCurrentImageName() {
        // This will be set properly when we have the image context
        return "Current_Image.tif";
    }

    /**
     * Check if a string is a valid ROI type.
     * 
     * @param roiType The ROI type to validate
     * @return true if it's a valid ROI type
     */
    private boolean isValidROIType(String roiType) {
        if (roiType == null) return false;
        String normalized = roiType.toLowerCase();
        return normalized.equals("cell") || normalized.equals("nucleus") || 
               normalized.equals("cytoplasm") || normalized.equals("vessel");
    }

    /**
     * Extract ROI type from feature key using pattern matching.
     * 
     * @param featureKey The feature key to parse
     * @return The extracted ROI type or null
     */
    private String extractROITypeFromKey(String featureKey) {
        if (featureKey == null) return null;
        
        String key = featureKey.toLowerCase();
        if (key.contains("cell")) return "Cell";
        if (key.contains("nucleus")) return "Nucleus";
        if (key.contains("cytoplasm")) return "Cytoplasm";
        if (key.contains("vessel")) return "Vessel";
        
        return null;
    }

    /**
     * Extract cell ID from feature key using pattern matching.
     * For keys like "P1_10_03.tif_Cytoplasm_978", extract "978"
     * 
     * @param featureKey The feature key to parse
     * @return The extracted cell ID or null
     */
    private String extractCellIdFromKey(String featureKey) {
        if (featureKey == null) return null;
        
        // Look for numbers at the end of the string after the last underscore
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*_([0-9]+)$");
        java.util.regex.Matcher matcher = pattern.matcher(featureKey);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback: look for any numbers at the end
        pattern = java.util.regex.Pattern.compile(".*?([0-9]+)$");
        matcher = pattern.matcher(featureKey);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}