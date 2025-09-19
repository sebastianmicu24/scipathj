package com.scipath.scipathj.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Loads training data from JSON file and prepares feature arrays for XGBoost.
 * Simplified version of SCHELI ReadData focused only on JSON parsing.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class JSONDataReader {

    private static final Logger logger = LoggerFactory.getLogger(JSONDataReader.class);

    // All cell features organized by file:id
    private final Map<String, float[]> allCellFeatures = new HashMap<>();

    // Training labels by file:id
    private final Map<String, Float> trainingLabels = new HashMap<>();

    // Class name to ID mapping
    private final Map<String, Integer> classNameToIdMap = new HashMap<>();

    // Reverse mapping for evaluation
    private final Map<Integer, String> idToClassNameMap = new HashMap<>();

    // All collected features ordered
    private List<String> featureNames = new ArrayList<>();

    // Feature masking for selection
    private Map<String, Boolean> featureEnabled = new HashMap<>();

    // Class colors from training data
    private final Map<String, String> classColors = new HashMap<>();

    /**
     * Constructs a new JSONDataReader.
     *
     * @param jsonFile the JSON file containing training data
     * @param featureFilter optional list of features to include (null = all)
     */
    public JSONDataReader(File jsonFile, List<String> featureFilter) {
        logger.info("Loading training data from: {}", jsonFile.getAbsolutePath());
        try {
            // First pass - collect all feature names to build complete name set
            parseJSONFileForFeatureNames(jsonFile, featureFilter);
            // Second pass - parse data with proper feature knowledge
            parseJSONFile(jsonFile, featureFilter);
            logger.info("Training data loaded successfully. Features: {}, Samples: {}",
                featureNames.size(), allCellFeatures.size());
        } catch (IOException e) {
            logger.error("Error loading JSON training data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load training data", e);
        }
    }

    private void parseJSONFileForFeatureNames(File jsonFile, List<String> featureFilter) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);

        // First pass: collect feature names from actual JSON structure
        if (root.has("classifiedROIs")) {
            JsonNode classifiedROIs = root.get("classifiedROIs");
            Iterator<Map.Entry<String, JsonNode>> fileIterator = classifiedROIs.fields();

            boolean collected = false;
            while (fileIterator.hasNext() && !collected) {
                Map.Entry<String, JsonNode> fileEntry = fileIterator.next();
                JsonNode roiData = fileEntry.getValue();
                Iterator<Map.Entry<String, JsonNode>> roiIterator = roiData.fields();

                if (roiIterator.hasNext()) {
                    Map.Entry<String, JsonNode> roiEntry = roiIterator.next();
                    JsonNode components = roiEntry.getValue();

                    // Process first ROI to collect feature names
                    collectFeatureNamesFromActualStructure(components);
                    collected = true;
                }
            }
        }
    }

    private void parseJSONFile(File jsonFile, List<String> featureFilter) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);

        // Parse classes
        if (root.has("classes")) {
            JsonNode classesArray = root.get("classes");
            for (int i = 0; i < classesArray.size(); i++) {
                String className = classesArray.get(i).asText();
                classNameToIdMap.put(className, i);
                idToClassNameMap.put(i, className);
                logger.debug("Mapped class '{}' to ID {}", className, i);
            }
        }

        // Parse class colors
        if (root.has("classColors")) {
            JsonNode classColorsNode = root.get("classColors");
            Iterator<Map.Entry<String, JsonNode>> colorIterator = classColorsNode.fields();
            while (colorIterator.hasNext()) {
                Map.Entry<String, JsonNode> colorEntry = colorIterator.next();
                String className = colorEntry.getKey();
                String color = colorEntry.getValue().asText();
                classColors.put(className, color);
                logger.debug("Loaded class color '{}' for '{}'", color, className);
            }
        }

        // Parse classified ROIs
        if (root.has("classifiedROIs")) {
            JsonNode classifiedROIs = root.get("classifiedROIs");

            Iterator<Map.Entry<String, JsonNode>> fileIterator = classifiedROIs.fields();
            while (fileIterator.hasNext()) {
                Map.Entry<String, JsonNode> fileEntry = fileIterator.next();
                String filename = fileEntry.getKey();

                JsonNode roiData = fileEntry.getValue();
                Iterator<Map.Entry<String, JsonNode>> roiIterator = roiData.fields();

                while (roiIterator.hasNext()) {
                    Map.Entry<String, JsonNode> roiEntry = roiIterator.next();
                    String roiId = roiEntry.getKey();
                    JsonNode components = roiEntry.getValue();

                    processROIComponents(filename, roiId, components, featureFilter);
                }
            }
        }

        // Initialize feature selection defaults
        initializeFeatureSelection(featureFilter);
    }

    private void processROIComponents(String filename, String roiId, JsonNode components, List<String> featureFilter) {
        String roiKey = filename + ":" + roiId;

        // Process each component (Cell, Cytoplasm, Nucleus)
        List<float[]> componentFeatures = new ArrayList<>();
        String[] componentTypes = {"Cell", "Cytoplasm", "Nucleus"};

        for (String componentType : componentTypes) {
            if (components.has(componentType)) {
                JsonNode componentData = components.get(componentType);

                // Extract label from Cell component (assuming same for all components)
                if (componentType.equals("Cell") && componentData.has("class")) {
                    String className = componentData.get("class").asText();
                    Integer classId = classNameToIdMap.get(className);
                    if (classId != null) {
                        trainingLabels.put(roiKey, classId.floatValue());
                    } else {
                        logger.warn("Unknown class '{}' for ROI {}", className, roiKey);
                    }
                }

                // Extract features from component with proper component-prefix filtering
                float[] features = extractFeaturesFromComponent(componentData, featureFilter, componentType);
                componentFeatures.add(features);
            } else {
                // Use zero-array for missing components
                componentFeatures.add(new float[0]);
            }
        }

        // Concatenate features from all components
        float[] combinedFeatures = concatenateFeatures(componentFeatures);
        allCellFeatures.put(roiKey, combinedFeatures);
    }

    private float[] extractFeaturesFromComponent(JsonNode componentData, List<String> featureFilter, String componentType) {
        List<Float> features = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fieldIterator = componentData.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
            String featureName = fieldEntry.getKey();
            JsonNode valueNode = fieldEntry.getValue();

            // Skip non-feature fields
            if ("class".equals(featureName) || "ignore".equals(featureName)) {
                continue;
            }

            // Apply feature filter if provided - check both prefixed and basic names
            String prefixedFeatureName = componentType + "." + featureName;
            if (featureFilter != null && !featureFilter.contains(featureName) && !featureFilter.contains(prefixedFeatureName)) {
                // Also check underscore variant since that's what collectFeatureNamesWithPrefixes creates
                String underscoreFeatureName = componentType + "_" + featureName;
                if (!featureFilter.contains(underscoreFeatureName)) {
                    continue;
                }
            }

            // Convert to float
            if (valueNode.isNumber()) {
                features.add(valueNode.floatValue());
            } else if (valueNode.isTextual() && "N/A".equals(valueNode.asText())) {
                features.add(Float.NaN);
            }
        }

        float[] result = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            result[i] = features.get(i);
        }
        return result;
    }

    private void collectFeatureNames(JsonNode componentData) {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = componentData.fields();
        while (fieldIterator.hasNext()) {
            String featureName = fieldIterator.next().getKey();

            // Skip non-feature fields
            if (!"class".equals(featureName) && !"ignore".equals(featureName)) {
                featureNames.add(featureName);
            }
        }
        logger.debug("Collected {} feature names: {}", featureNames.size(), featureNames);
    }

    private void collectFeatureNamesFromActualStructure(JsonNode roiTypeData) {
        logger.debug("Collecting features from JSON structure...");

        // roiTypeData here represents: roiType -> features mapping
        // e.g., { "Cell": {...}, "Cytoplasm": {...}, "Nucleus": {...} }

        Iterator<Map.Entry<String, JsonNode>> roiTypeIterator = roiTypeData.fields();
        while (roiTypeIterator.hasNext()) {
            Map.Entry<String, JsonNode> roiTypeEntry = roiTypeIterator.next();
            String roiType = roiTypeEntry.getKey();
            JsonNode featureData = roiTypeEntry.getValue();

            logger.debug("Processing ROI type: {} with {} fields", roiType, featureData.size());

            // Extract feature names from this ROI type
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = featureData.fields();
            while (fieldIterator.hasNext()) {
                String featureName = fieldIterator.next().getKey();

                // Skip non-feature fields
                if (!"class".equals(featureName) && !"ignore".equals(featureName)) {
                    String fullFeatureName = roiType + "_" + featureName;
                    if (!featureNames.contains(fullFeatureName)) {
                        featureNames.add(fullFeatureName);
                        logger.debug("Added feature: {}", fullFeatureName);
                    }
                }
            }
        }

        logger.debug("Collected {} total feature names: {}", featureNames.size(), featureNames);
    }

    private void collectFeatureNamesWithPrefixes(JsonNode componentData, String[] componentTypes) {
        List<String> baseFeatures = new ArrayList<>();

        // Collect basic feature names first
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = componentData.fields();
        while (fieldIterator.hasNext()) {
            String featureName = fieldIterator.next().getKey();

            // Skip non-feature fields
            if (!"class".equals(featureName) && !"ignore".equals(featureName)) {
                baseFeatures.add(featureName);
                logger.debug("Found feature: {}", featureName);
            }
        }

        logger.debug("Base features collected: {}", baseFeatures);

        // Create feature names with prefixes for each component
        for (String componentType : componentTypes) {
            for (String baseFeature : baseFeatures) {
                // Add component prefix to each feature (e.g., "Cell.vessel_distance")
                String fullFeatureName = componentType + "_" + baseFeature;
                featureNames.add(fullFeatureName);
            }
        }
        logger.debug("Collected {} feature names with prefixes: {}", featureNames.size(), featureNames);
    }

    private float[] concatenateFeatures(List<float[]> componentFeatures) {
        int totalSize = 0;
        for (float[] features : componentFeatures) {
            totalSize += features.length;
        }

        float[] result = new float[totalSize];
        int offset = 0;
        for (float[] features : componentFeatures) {
            if (features.length > 0) {
                System.arraycopy(features, 0, result, offset, features.length);
            }
            offset += features.length;
        }

        return result;
    }

    private float[] concatenateFeaturesSimple(List<float[]> componentFeatures) {
        int totalSize = 0;
        for (float[] features : componentFeatures) {
            totalSize += features.length;
        }

        float[] result = new float[totalSize];
        int offset = 0;
        for (float[] features : componentFeatures) {
            if (features.length > 0) {
                System.arraycopy(features, 0, result, offset, features.length);
            }
            offset += features.length;
        }

        return result;
    }

    private void initializeFeatureSelection(List<String> featureFilter) {
        for (String featureName : featureNames) {
            // Auto-filter out unwanted features that are not useful for modeling
            boolean isUnwantedFeature = featureName.contains("closest_vessel") ||
                                       featureName.contains("closest_neighbor") ||
                                       featureName.contains("_closest_vessel") ||
                                       featureName.contains("_closest_neighbor");

            if (isUnwantedFeature) {
                featureEnabled.put(featureName, false);
                continue;
            }

            // Use the provided filter or enable by default if no filter specified
            boolean enabled = (featureFilter == null) || featureFilter.contains(featureName);
            featureEnabled.put(featureName, enabled);
        }
    }

    /**
     * Gets all cell features.
     *
     * @return map of ROI key to feature array
     */
    public Map<String, float[]> getAllCellFeatures() {
        return Collections.unmodifiableMap(allCellFeatures);
    }

    /**
     * Gets training labels.
     *
     * @return map of ROI key to class ID
     */
    public Map<String, Float> getTrainingLabels() {
        return Collections.unmodifiableMap(trainingLabels);
    }

    /**
     * Gets class name to ID mapping.
     *
     * @return mapping
     */
    public Map<String, Integer> getClassNameToIdMap() {
        return Collections.unmodifiableMap(classNameToIdMap);
    }

    /**
     * Gets ID to class name mapping.
     *
     * @return mapping
     */
    public Map<Integer, String> getIdToClassNameMap() {
        return Collections.unmodifiableMap(idToClassNameMap);
    }

    /**
     * Gets class colors from training data.
     *
     * @return mapping of class name to hex color
     */
    public Map<String, String> getClassColors() {
        return Collections.unmodifiableMap(classColors);
    }

    /**
     * Gets available feature names.
     *
     * @return feature names list
     */
    public List<String> getFeatureNames() {
        return Collections.unmodifiableList(featureNames);
    }

    /**
     * Gets selected features based on feature selection configuration.
     *
     * @return list of selected feature names
     */
    public List<String> getSelectedFeatureNames() {
        List<String> selected = new ArrayList<>();
        for (String featureName : featureNames) {
            if (featureEnabled.getOrDefault(featureName, true)) {
                selected.add(featureName);
            }
        }
        return selected;
    }

    /**
     * Updates feature selection.
     *
     * @param featureName feature to update
     * @param enabled whether feature is enabled
     */
    public void setFeatureEnabled(String featureName, boolean enabled) {
        featureEnabled.put(featureName, enabled);
    }

    /**
     * Creates filtered feature arrays based on current selection.
     *
     * @return map of ROI key to filtered feature array
     */
    public Map<String, float[]> getFilteredCellFeatures() {
        Map<String, float[]> filteredFeatures = new HashMap<>();
        List<String> selectedFeatureNames = getSelectedFeatureNames();

        if (selectedFeatureNames.isEmpty()) {
            logger.warn("No features selected for filtering");
            return new HashMap<>(allCellFeatures);
        }

        logger.info("Filtering features - selected: {}, total: {}", selectedFeatureNames.size(), featureNames.size());

        for (Map.Entry<String, float[]> entry : allCellFeatures.entrySet()) {
            String roiKey = entry.getKey();
            float[] allFeatures = entry.getValue();
            float[] filtered = new float[selectedFeatureNames.size()];

            // Since features are concatenated by component, map selected features to positions
            int filteredIndex = 0;
            for (String selectedFeature : selectedFeatureNames) {
                // Find the global index of this feature in the concatenated array
                int featureIndex = getFeatureGlobalIndex(selectedFeature);
                if (featureIndex >= 0 && featureIndex < allFeatures.length) {
                    filtered[filteredIndex++] = allFeatures[featureIndex];
                } else {
                    logger.warn("Feature '{}' not found in concatenated array (length: {})", selectedFeature, allFeatures.length);
                }
            }

            filteredFeatures.put(roiKey, filtered);
        }

        logger.info("Filtered features from {} to {} per sample", featureNames.size(), selectedFeatureNames.size());
        return filteredFeatures;
    }

    /**
     * Gets the global index of a feature in the concatenated feature array.
     *
     * @param featureName the feature name with component prefix
     * @return global index or -1 if not found
     */
    private int getFeatureGlobalIndex(String featureName) {
        // Since we use underscore format (Cell_vessel_distance), find the position
        String[] components = featureName.split("_", 2);
        if (components.length != 2) {
            return -1; // Invalid format
        }

        String componentType = components[0];
        String baseFeatureName = components[1];
        int componentIndex = getComponentIndex(componentType);
        int featureIndex = getFeatureIndexInComponent(baseFeatureName);

        if (componentIndex == -1 || featureIndex == -1) {
            return -1;
        }

        // Calculate global index based on component position
        return (componentIndex * getFeaturesPerComponent()) + featureIndex;
    }

    private int getComponentIndex(String componentType) {
        switch (componentType) {
            case "Cell": return 0;
            case "Cytoplasm": return 1;
            case "Nucleus": return 2;
            default: return -1;
        }
    }

    private int getFeatureIndexInComponent(String featureName) {
        // Count features that belong to Cell component (first component)
        int index = 0;
        for (String fname : featureNames) {
            if (fname.startsWith("Cell_")) {
                String baseName = fname.substring(5); // Remove "Cell_" prefix
                if (baseName.equals(featureName)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private int getFeaturesPerComponent() {
        // Count how many features belong to each component (assumes equal number)
        long cellFeatures = featureNames.stream().filter(name -> name.startsWith("Cell_")).count();
        return (int) cellFeatures;
    }
}