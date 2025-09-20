package com.scipath.scipathj.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Unified JSON data reader for training data from SciPathJ's dataset export.
 * Handles the modern JSON export format from DatasetControlsPanel with
 * comprehensive feature extraction and training label processing.
 *
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 1.0.0
 */
public class JSONDataReader {
    private static final Logger logger = LoggerFactory.getLogger(JSONDataReader.class);

    // Cell features by cell ID
    private final Map<String, float[]> cellFeatures = new HashMap<>();
    
    // Training labels by cell ID
    private final Map<String, Float> trainingLabels = new HashMap<>();
    
    // Class name to ID mapping
    private final Map<String, Integer> classNameToIdMap = new HashMap<>();

    // ID to class name mapping
    private final Map<Integer, String> idToClassNameMap = new HashMap<>();

    // Class colors
    private final Map<String, String> classColors = new HashMap<>();

    // Feature names in order
    private List<String> featureNames = new ArrayList<>();

    // Feature selection
    private List<String> selectedFeatureNames = new ArrayList<>();

    // XGBoost-compatible label mapping (original class ID -> XGBoost index)
    private final Map<Integer, Integer> originalToXgboostIndex = new HashMap<>();

    // Reverse mapping (XGBoost index -> original class ID)
    private final Map<Integer, Integer> xgboostToOriginalId = new HashMap<>();

    /**
     * Load training data from the new simplified JSON format.
     */
    public JSONDataReader(File jsonFile, List<String> featureFilter) {
        logger.info("Loading unified training data from: {}", jsonFile.getAbsolutePath());
        try {
            parseJSONFile(jsonFile, featureFilter);
            logger.info("Training data loaded successfully. Features: {}, Samples: {}", 
                       selectedFeatureNames.size(), cellFeatures.size());
        } catch (IOException e) {
            logger.error("Error loading JSON training data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load training data", e);
        }
    }

    private void parseJSONFile(File jsonFile, List<String> featureFilter) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);

        // Parse feature names
        if (root.has("featureNames")) {
            JsonNode featureNamesNode = root.get("featureNames");
            for (JsonNode featureName : featureNamesNode) {
                featureNames.add(featureName.asText());
            }
        } else {
            logger.warn("No featureNames found in JSON, will infer from data");
        }

        // Parse class mapping - support comprehensive format, traditional format, and legacy fallback
        if (root.has("classifiedROIs")) {
            // New comprehensive format: extract class names from classifiedROIs structure
            JsonNode classifiedROIs = root.get("classifiedROIs");
            Iterator<Map.Entry<String, JsonNode>> classIterator = classifiedROIs.fields();
            int classId = 0;
            
            while (classIterator.hasNext()) {
                Map.Entry<String, JsonNode> classEntry = classIterator.next();
                String className = classEntry.getKey().trim(); // Remove any whitespace
                
                classNameToIdMap.put(className, classId);
                idToClassNameMap.put(classId, className);
                logger.debug("Mapped class '{}' to ID {} from comprehensive format", className, classId);
                classId++;
            }
            
            logger.info("Loaded classes from comprehensive format: {}", classNameToIdMap);
            
            // Create XGBoost-compatible label mapping (original IDs -> sequential indices 0,1,2...)
            createXGBoostLabelMapping();
            
        } else if (root.has("classes")) {
            // Traditional format: classes is an array of objects with {name, count, id, color}
            JsonNode classesArray = root.get("classes");
            for (int i = 0; i < classesArray.size(); i++) {
                JsonNode classNode = classesArray.get(i);
                if (classNode.isObject() && classNode.has("name")) {
                    String className = classNode.get("name").asText();
                    // Use class ID from JSON if available, otherwise use array index
                    int classId = i;
                    if (classNode.has("id") && !classNode.get("id").isNull()) {
                        classId = classNode.get("id").asInt();
                    }
                    classNameToIdMap.put(className, classId);
                    idToClassNameMap.put(classId, className);
                    logger.debug("Mapped class '{}' to ID {} from traditional format", className, classId);

                    // Store class color if available
                    if (classNode.has("color") && !classNode.get("color").isNull()) {
                        String color = classNode.get("color").asText();
                        classColors.put(className, color);
                        logger.debug("Stored color '{}' for class '{}'", color, className);
                    }
                } else if (classNode.isTextual()) {
                    // Fallback: classes is a simple string array
                    String className = classNode.asText();
                    classNameToIdMap.put(className, i);
                    idToClassNameMap.put(i, className);
                    logger.debug("Mapped class '{}' to ID {} (old string format)", className, i);
                }
            }
            logger.info("Loaded classes from traditional format: {}", classNameToIdMap);

            // Create XGBoost-compatible label mapping (original IDs -> sequential indices 0,1,2...)
            createXGBoostLabelMapping();
            
        } else if (root.has("classMapping")) {
            // Legacy format: classMapping as object with name->id mappings
            JsonNode classMappingNode = root.get("classMapping");
            Iterator<Map.Entry<String, JsonNode>> fields = classMappingNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String className = entry.getKey();
                int classId = entry.getValue().asInt();
                classNameToIdMap.put(className, classId);
                idToClassNameMap.put(classId, className);
            }
            logger.info("Loaded class mapping (legacy format): {}", classNameToIdMap);
            
            // Create XGBoost-compatible label mapping
            createXGBoostLabelMapping();
            
        } else {
            logger.warn("No class information found in JSON (neither 'classifiedROIs', 'classes', nor 'classMapping')");
        }
// Try to parse labels and features from different formats
if (root.has("classifiedROIs")) {
    parseLabelsFromClassifiedROIs(root.get("classifiedROIs"));

    // Also extract features from the comprehensive format if no features parsed yet
    if (cellFeatures.isEmpty()) {
        parseFeaturesFromClassifiedROIs(root.get("classifiedROIs"), featureFilter);
    }
} else if (root.has("cellData")) {
    // Handle the new cellData format (comprehensive_feature_extraction)
    parseCellDataFormat(root.get("cellData"), featureFilter);
}

        // Parse cell features
        if (root.has("cellFeatures")) {
            JsonNode cellFeaturesNode = root.get("cellFeatures");
            Iterator<Map.Entry<String, JsonNode>> cellIterator = cellFeaturesNode.fields();
            
            // If feature names not set, infer from first cell
            if (featureNames.isEmpty() && cellIterator.hasNext()) {
                Map.Entry<String, JsonNode> firstCell = cellIterator.next();
                JsonNode firstFeatures = firstCell.getValue();
                Iterator<String> featureIterator = firstFeatures.fieldNames();
                while (featureIterator.hasNext()) {
                    featureNames.add(featureIterator.next());
                }
                logger.info("Inferred feature names from data: {}", featureNames);
                
                // Reset iterator
                cellIterator = cellFeaturesNode.fields();
            }
            
            // Apply feature filter
            selectedFeatureNames = (featureFilter != null && !featureFilter.isEmpty()) 
                ? new ArrayList<>(featureFilter) 
                : new ArrayList<>(featureNames);
            
            // Parse all cells
            while (cellIterator.hasNext()) {
                Map.Entry<String, JsonNode> cellEntry = cellIterator.next();
                String cellId = cellEntry.getKey();
                JsonNode featuresNode = cellEntry.getValue();
                
                // Extract features in order
                float[] features = new float[selectedFeatureNames.size()];
                for (int i = 0; i < selectedFeatureNames.size(); i++) {
                    String featureName = selectedFeatureNames.get(i);
                    if (featuresNode.has(featureName)) {
                        features[i] = (float) featuresNode.get(featureName).asDouble();
                    } else {
                        features[i] = 0.0f; // Default value for missing features
                        logger.warn("Feature '{}' missing for cell '{}', using 0.0", featureName, cellId);
                    }
                }
                
                cellFeatures.put(cellId, features);
            }
        }

        // Parse cell labels
        if (root.has("cellLabels")) {
            JsonNode cellLabelsNode = root.get("cellLabels");
            Iterator<Map.Entry<String, JsonNode>> labelIterator = cellLabelsNode.fields();
            while (labelIterator.hasNext()) {
                Map.Entry<String, JsonNode> labelEntry = labelIterator.next();
                String cellId = labelEntry.getKey();
                float label = (float) labelEntry.getValue().asDouble();
                trainingLabels.put(cellId, label);
            }
        }

        logger.info("Loaded {} cells with {} features each", cellFeatures.size(), selectedFeatureNames.size());
        logger.info("Feature selection: {}", selectedFeatureNames);
    }

    /**
     * Parse training labels from the new comprehensive format classifiedROIs section.
     * New format: classifiedROIs -> className -> roiName -> classId -> features
     */
    private void parseLabelsFromClassifiedROIs(JsonNode classifiedROIs) {
        logger.info("Parsing labels from new comprehensive classifiedROIs section...");

        Iterator<Map.Entry<String, JsonNode>> classIterator = classifiedROIs.fields();
        int labelCount = 0;

        while (classIterator.hasNext()) {
            Map.Entry<String, JsonNode> classEntry = classIterator.next();
            String className = classEntry.getKey().trim(); // Remove any whitespace
            JsonNode roiDataByClass = classEntry.getValue();

            // Get class ID for this class name
            Integer classId = classNameToIdMap.get(className);
            if (classId == null) {
                logger.warn("Unknown class name '{}' - skipping", className);
                continue;
            }

            // Convert to XGBoost-compatible index (0-based)
            Integer xgboostIndex = originalToXgboostIndex.get(classId);
            if (xgboostIndex == null) {
                logger.warn("No XGBoost mapping for class ID {} (class: {}) - skipping", classId, className);
                continue;
            }

            // Parse all ROIs for this class
            Iterator<Map.Entry<String, JsonNode>> roiIterator = roiDataByClass.fields();
            while (roiIterator.hasNext()) {
                Map.Entry<String, JsonNode> roiEntry = roiIterator.next();
                String roiName = roiEntry.getKey().trim();
                JsonNode classIdData = roiEntry.getValue();

                // Skip ROIs marked as ignored
                Iterator<Map.Entry<String, JsonNode>> classIdIterator = classIdData.fields();
                while (classIdIterator.hasNext()) {
                    Map.Entry<String, JsonNode> classIdEntry = classIdIterator.next();
                    String classIdKey = classIdEntry.getKey().trim();
                    JsonNode roiFeatures = classIdEntry.getValue();

                    // Check if this ROI should be ignored
                    if (roiFeatures.has("ignore") && roiFeatures.get("ignore").asBoolean()) {
                        logger.debug("Skipping ignored ROI: {}", roiName);
                        continue;
                    }

                    // Create unique cell ID from ROI name and class ID
                    String cellId = roiName + "_" + classIdKey;
                    trainingLabels.put(cellId, xgboostIndex.floatValue());
                    labelCount++;
                    
                    logger.debug("Added XGBoost label {} for cell {} (class: {} -> XGBoost index: {})",
                        xgboostIndex, cellId, className, xgboostIndex);
                }
            }
        }

        logger.info("Parsed {} training labels from comprehensive classifiedROIs format", labelCount);
    }

    /**
     * Parse feature data from the new comprehensive format classifiedROIs section.
     * New format: classifiedROIs -> className -> roiName -> classId -> features
     */
    private void parseFeaturesFromClassifiedROIs(JsonNode classifiedROIs, List<String> featureFilter) {
        logger.info("Parsing features from new comprehensive classifiedROIs section...");

        // Collect all unique feature names first
        Set<String> allFeatureNames = new HashSet<>();

        Iterator<Map.Entry<String, JsonNode>> classIterator = classifiedROIs.fields();
        while (classIterator.hasNext()) {
            Map.Entry<String, JsonNode> classEntry = classIterator.next();
            JsonNode roiDataByClass = classEntry.getValue();

            Iterator<Map.Entry<String, JsonNode>> roiIterator = roiDataByClass.fields();
            while (roiIterator.hasNext()) {
                Map.Entry<String, JsonNode> roiEntry = roiIterator.next();
                JsonNode classIdData = roiEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> classIdIterator = classIdData.fields();
                while (classIdIterator.hasNext()) {
                    Map.Entry<String, JsonNode> classIdEntry = classIdIterator.next();
                    JsonNode roiFeatures = classIdEntry.getValue();

                    // Extract all feature names from this ROI (exclude special fields)
                    Iterator<Map.Entry<String, JsonNode>> fieldIterator = roiFeatures.fields();
                    while (fieldIterator.hasNext()) {
                        Map.Entry<String, JsonNode> field = fieldIterator.next();
                        String fieldName = field.getKey();
                        if (!"class".equals(fieldName) && !"ignore".equals(fieldName)) {
                            allFeatureNames.add(fieldName);
                        }
                    }
                }
            }
        }

        // Set feature names if not already set
        if (featureNames.isEmpty()) {
            featureNames = new ArrayList<>(allFeatureNames);
            Collections.sort(featureNames); // Sort for consistency
            logger.info("Collected {} feature names from comprehensive format: {}", featureNames.size(), featureNames);
        }

        // Apply feature filter
        selectedFeatureNames = (featureFilter != null && !featureFilter.isEmpty())
            ? new ArrayList<>(featureFilter)
            : new ArrayList<>(featureNames);

        // Parse feature data for each cell
        classIterator = classifiedROIs.fields(); // Reset iterator
        int cellCount = 0;

        while (classIterator.hasNext()) {
            Map.Entry<String, JsonNode> classEntry = classIterator.next();
            String className = classEntry.getKey().trim();
            JsonNode roiDataByClass = classEntry.getValue();

            Iterator<Map.Entry<String, JsonNode>> roiIterator = roiDataByClass.fields();
            while (roiIterator.hasNext()) {
                Map.Entry<String, JsonNode> roiEntry = roiIterator.next();
                String roiName = roiEntry.getKey().trim();
                JsonNode classIdData = roiEntry.getValue();

                Iterator<Map.Entry<String, JsonNode>> classIdIterator = classIdData.fields();
                while (classIdIterator.hasNext()) {
                    Map.Entry<String, JsonNode> classIdEntry = classIdIterator.next();
                    String classIdKey = classIdEntry.getKey().trim();
                    JsonNode roiFeatures = classIdEntry.getValue();

                    // Skip ROIs marked as ignored
                    if (roiFeatures.has("ignore") && roiFeatures.get("ignore").asBoolean()) {
                        logger.debug("Skipping ignored ROI: {}", roiName);
                        continue;
                    }

                    List<Float> featuresList = new ArrayList<>();

                    // Extract features from each selected feature in order
                    for (String selectedFeature : selectedFeatureNames) {
                        boolean found = false;

                        if (roiFeatures.has(selectedFeature) && !roiFeatures.get(selectedFeature).isNull()) {
                            if (roiFeatures.get(selectedFeature).isNumber()) {
                                featuresList.add((float) roiFeatures.get(selectedFeature).asDouble());
                                found = true;
                            } else if (roiFeatures.get(selectedFeature).isTextual() &&
                                      "N/A".equals(roiFeatures.get(selectedFeature).asText())) {
                                featuresList.add(Float.NaN);
                                found = true;
                            }
                        }

                        if (!found) {
                            featuresList.add(0.0f); // Default value for missing features
                            logger.debug("Feature '{}' missing for cell '{}', using 0.0", selectedFeature, roiName);
                        }
                    }

                    // Convert list to array and store
                    float[] featuresArray = new float[featuresList.size()];
                    for (int i = 0; i < featuresList.size(); i++) {
                        featuresArray[i] = featuresList.get(i);
                    }

                    // Create unique cell ID from ROI name and class ID
                    String cellId = roiName + "_" + classIdKey;
                    cellFeatures.put(cellId, featuresArray);
                    cellCount++;

                    logger.debug("Added features for cell {} (class: {}, {} features)",
                        cellId, className, featuresArray.length);
                }
            }
        }

        logger.info("Parsed {} cells with {} features each from comprehensive classifiedROIs", cellCount, selectedFeatureNames.size());
    }

    /**
     * Parse the cellData format from comprehensive_feature_extraction JSON.
     * This handles the structure where each cell has metadata and ROI sections (Cell, Cytoplasm, Nucleus).
     */
    private void parseCellDataFormat(JsonNode cellDataNode, List<String> featureFilter) {
        logger.info("Parsing cellData format from comprehensive_feature_extraction...");
        
        // First pass: collect all available features from all ROI types
        Set<String> allFeatureNames = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> cellIterator = cellDataNode.fields();
        
        while (cellIterator.hasNext()) {
            Map.Entry<String, JsonNode> cellEntry = cellIterator.next();
            JsonNode cellNode = cellEntry.getValue();
            
            // Extract features from Cell, Cytoplasm, and Nucleus sections
            for (String roiType : new String[]{"Cell", "Cytoplasm", "Nucleus"}) {
                if (cellNode.has(roiType)) {
                    JsonNode roiNode = cellNode.get(roiType);
                    Iterator<String> fieldNames = roiNode.fieldNames();
                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        // Skip non-numeric fields like "ignore"
                        if (!fieldName.equals("ignore")) {
                            allFeatureNames.add(roiType.toLowerCase() + "_" + fieldName);
                        }
                    }
                }
            }
        }
        
        // Set feature names
        featureNames = new ArrayList<>(allFeatureNames);
        Collections.sort(featureNames); // Ensure consistent ordering
        
        // Apply feature filter
        selectedFeatureNames = (featureFilter != null && !featureFilter.isEmpty())
            ? new ArrayList<>(featureFilter)
            : new ArrayList<>(featureNames);
            
        logger.info("Found {} total features, selected {} features",
                   featureNames.size(), selectedFeatureNames.size());
        
        // Second pass: extract features and labels for each cell
        cellIterator = cellDataNode.fields();
        int cellCount = 0;
        
        while (cellIterator.hasNext()) {
            Map.Entry<String, JsonNode> cellEntry = cellIterator.next();
            String cellId = cellEntry.getKey();
            JsonNode cellNode = cellEntry.getValue();
            
            // Extract class label
            if (cellNode.has("Class")) {
                String className = cellNode.get("Class").asText();
                Integer classId = classNameToIdMap.get(className);
                if (classId != null) {
                    trainingLabels.put(cellId, classId.floatValue());
                } else {
                    logger.warn("Unknown class '{}' for cell {}", className, cellId);
                    continue;
                }
            } else {
                logger.warn("Cell {} missing class information", cellId);
                continue;
            }
            
            // Extract features
            float[] features = new float[selectedFeatureNames.size()];
            boolean hasValidFeatures = false;
            
            for (int i = 0; i < selectedFeatureNames.size(); i++) {
                String featureName = selectedFeatureNames.get(i);
                features[i] = 0.0f; // Default value
                
                // Parse feature name to get ROI type and feature
                String[] parts = featureName.split("_", 2);
                if (parts.length == 2) {
                    String roiType = parts[0];
                    String actualFeature = parts[1];
                    
                    // Capitalize first letter for JSON lookup
                    String roiTypeCap = roiType.substring(0, 1).toUpperCase() + roiType.substring(1);
                    
                    if (cellNode.has(roiTypeCap)) {
                        JsonNode roiNode = cellNode.get(roiTypeCap);
                        if (roiNode.has(actualFeature)) {
                            JsonNode featureNode = roiNode.get(actualFeature);
                            if (featureNode.isNumber()) {
                                features[i] = (float) featureNode.asDouble();
                                hasValidFeatures = true;
                            }
                        }
                    }
                }
            }
            
            if (hasValidFeatures) {
                cellFeatures.put(cellId, features);
                cellCount++;
                logger.debug("Added features for cell {} (class: {}, {} features)",
                           cellId, cellNode.get("Class").asText(), features.length);
            } else {
                logger.warn("Cell {} has no valid numeric features", cellId);
            }
        }
        
        logger.info("Parsed {} cells with {} features each from cellData format",
                   cellCount, selectedFeatureNames.size());
    }

    /**
     * Create XGBoost-compatible label mapping where original class IDs are remapped
     * to sequential indices starting from 0 (required by XGBoost)
     */
    private void createXGBoostLabelMapping() {
        logger.info("Creating XGBoost-compatible label mapping...");

        // Get all unique class IDs from the loaded mapping
        Set<Integer> uniqueClassIds = new HashSet<>(idToClassNameMap.keySet());
        List<Integer> sortedIds = new ArrayList<>(uniqueClassIds);
        Collections.sort(sortedIds);

        // Create mapping from original class ID to XGBoost index (0, 1, 2...)
        for (int i = 0; i < sortedIds.size(); i++) {
            int originalId = sortedIds.get(i);
            int xgboostIndex = i;

            originalToXgboostIndex.put(originalId, xgboostIndex);
            xgboostToOriginalId.put(xgboostIndex, originalId);

            String className = idToClassNameMap.get(originalId);
            logger.debug("XGBoost mapping: original class '{}' (ID={}) → XGBoost index {}",
                className, originalId, xgboostIndex);
        }

        logger.info("XGBoost label mapping complete: {} classes remapped from {} to indices 0-{}",
            sortedIds.size(), sortedIds, sortedIds.size() - 1);
    }

    // Getters for compatibility with existing XGBoost trainer
    public Map<String, float[]> getAllCellFeatures() {
        return cellFeatures;
    }
    
    public Map<String, float[]> getFilteredCellFeatures() {
        return cellFeatures; // Already filtered during parsing
    }

    public Map<String, Float> getTrainingLabels() {
        return trainingLabels;
    }

    public Map<String, Integer> getClassNameToIdMap() {
        return classNameToIdMap;
    }

    public Map<Integer, String> getIdToClassNameMap() {
        return idToClassNameMap;
    }

    public Map<String, String> getClassColors() {
        return classColors;
    }

    public List<String> getSelectedFeatureNames() {
        return selectedFeatureNames;
    }

    public List<String> getFeatureNames() {
        return featureNames;
    }
    
    public List<String> getAllFeatureNames() {
        return featureNames;
    }

    /**
     * Get the XGBoost-compatible label mapping (original class ID -> XGBoost index)
     * @return mapping from original class IDs to XGBoost indices (0, 1, 2...)
     */
    public Map<Integer, Integer> getOriginalToXgboostIndex() {
        return originalToXgboostIndex;
    }

    /**
     * Get the reverse XGBoost label mapping (XGBoost index -> original class ID)
     * @return mapping from XGBoost indices back to original class IDs
     */
    public Map<Integer, Integer> getXgboostToOriginalId() {
        return xgboostToOriginalId;
    }
}