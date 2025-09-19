package com.scipath.scipathj.training.data;

import com.scipath.scipathj.training.model.TrainingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Simplified, single-pass training data reader.
 * Replaces the complex JSONDataReader with cleaner architecture.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class TrainingDataReader implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingDataReader.class);
    
    private final Map<String, float[]> features = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private final List<String> featureNames = new ArrayList<>();
    private final Map<String, Integer> classNameToIdMap = new HashMap<>();
    private final Map<Integer, String> idToClassNameMap = new HashMap<>();
    private final Map<String, String> classColors = new HashMap<>();
    
    /**
     * Creates a new training data reader and loads data from file.
     */
    public TrainingDataReader(File jsonFile) throws IOException {
        LOGGER.info("Loading training data from: {}", jsonFile.getAbsolutePath());
        parseJSONFile(jsonFile);
        LOGGER.info("Training data loaded: {} samples, {} features", features.size(), featureNames.size());
    }
    
    /**
     * Single-pass streaming parser (simplified implementation).
     */
    private void parseJSONFile(File jsonFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);
        
        // Parse classes
        if (root.has("classes")) {
            JsonNode classesArray = root.get("classes");
            for (int i = 0; i < classesArray.size(); i++) {
                String className = classesArray.get(i).asText();
                classNameToIdMap.put(className, i);
                idToClassNameMap.put(i, className);
            }
        }
        
        // Parse class colors
        if (root.has("classColors")) {
            JsonNode classColorsNode = root.get("classColors");
            classColorsNode.fields().forEachRemaining(entry -> 
                classColors.put(entry.getKey(), entry.getValue().asText())
            );
        }
        
        // Parse classified ROIs (simplified)
        if (root.has("classifiedROIs")) {
            parseROIData(root.get("classifiedROIs"));
        }
    }
    
    /**
     * Parses ROI data and extracts features.
     */
    private void parseROIData(JsonNode classifiedROIs) {
        // First pass: collect feature names
        Set<String> allFeatureNames = new HashSet<>();
        
        classifiedROIs.fields().forEachRemaining(fileEntry -> {
            JsonNode roiData = fileEntry.getValue();
            roiData.fields().forEachRemaining(roiEntry -> {
                JsonNode components = roiEntry.getValue();
                
                // Extract features from different component types
                extractFeatureNames(components, allFeatureNames);
            });
        });
        
        featureNames.addAll(allFeatureNames);
        Collections.sort(featureNames); // Ensure consistent ordering
        
        // Second pass: extract actual feature values
        classifiedROIs.fields().forEachRemaining(fileEntry -> {
            String fileName = fileEntry.getKey();
            JsonNode roiData = fileEntry.getValue();
            
            roiData.fields().forEachRemaining(roiEntry -> {
                String roiId = roiEntry.getKey();
                JsonNode components = roiEntry.getValue();
                
                String roiKey = fileName + ":" + roiId;
                extractFeatureValues(components, roiKey);
            });
        });
    }
    
    /**
     * Extracts feature names from ROI components.
     */
    private void extractFeatureNames(JsonNode components, Set<String> featureNames) {
        if (components.has("Cell")) {
            components.get("Cell").fieldNames().forEachRemaining(featureNames::add);
        }
        if (components.has("Nucleus")) {
            components.get("Nucleus").fieldNames().forEachRemaining(featureNames::add);
        }
        if (components.has("Cytoplasm")) {
            components.get("Cytoplasm").fieldNames().forEachRemaining(featureNames::add);
        }
    }
    
    /**
     * Extracts feature values for a specific ROI.
     */
    private void extractFeatureValues(JsonNode components, String roiKey) {
        float[] featureVector = new float[featureNames.size()];
        Arrays.fill(featureVector, 0.0f); // Default value
        
        // Extract from different component types
        extractFromComponent(components, "Cell", featureVector);
        extractFromComponent(components, "Nucleus", featureVector);
        extractFromComponent(components, "Cytoplasm", featureVector);
        
        features.put(roiKey, featureVector);
        
        // Extract class label
        if (components.has("assignedClass")) {
            String className = components.get("assignedClass").asText();
            Integer classId = classNameToIdMap.get(className);
            if (classId != null) {
                labels.put(roiKey, classId);
            }
        }
    }
    
    /**
     * Extracts features from a specific component.
     */
    private void extractFromComponent(JsonNode components, String componentName, float[] featureVector) {
        if (components.has(componentName)) {
            JsonNode component = components.get(componentName);
            component.fields().forEachRemaining(entry -> {
                String featureName = entry.getKey();
                int featureIndex = featureNames.indexOf(featureName);
                if (featureIndex >= 0) {
                    featureVector[featureIndex] = (float) entry.getValue().asDouble();
                }
            });
        }
    }
    
    /**
     * Returns training statistics.
     */
    public TrainingResult.TrainingStatistics getStatistics() {
        Map<String, Long> classDistribution = new HashMap<>();
        for (Integer classId : labels.values()) {
            String className = idToClassNameMap.get(classId);
            classDistribution.merge(className, 1L, Long::sum);
        }
        
        return new TrainingResult.TrainingStatistics(
            features.size(),    // totalSamples
            0,                  // trainingSamples (to be set during split)
            0,                  // testSamples (to be set during split)
            featureNames.size(), // numFeatures
            classNameToIdMap.size(), // numClasses
            classDistribution,   // classDistribution
            0L                  // trainingTimeMs (to be set during training)
        );
    }
    
    // Getters for existing compatibility
    public Map<String, float[]> getFeatures() {
        return Collections.unmodifiableMap(features);
    }
    
    public Map<String, Integer> getLabels() {
        return Collections.unmodifiableMap(labels);
    }
    
    public List<String> getFeatureNames() {
        return Collections.unmodifiableList(featureNames);
    }
    
    public Map<String, Integer> getClassNameToIdMap() {
        return Collections.unmodifiableMap(classNameToIdMap);
    }
    
    public Map<Integer, String> getIdToClassNameMap() {
        return Collections.unmodifiableMap(idToClassNameMap);
    }
    
    public Map<String, String> getClassColors() {
        return Collections.unmodifiableMap(classColors);
    }
    
    @Override
    public void close() {
        LOGGER.debug("Closing TrainingDataReader");
        // No resources to clean up in this implementation
    }
}