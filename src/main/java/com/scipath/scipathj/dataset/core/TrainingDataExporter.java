package com.scipath.scipathj.dataset.core;

import com.scipath.scipathj.dataset.model.TrainingDataset;
import com.scipath.scipathj.dataset.model.ClassificationClass;
import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

/**
 * Service for exporting training datasets to JSON format.
 * Handles serialization of ROIs, features, and class information.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class TrainingDataExporter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingDataExporter.class);
    
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new training data exporter.
     */
    public TrainingDataExporter() {
        this.objectMapper = new ObjectMapper();
        LOGGER.debug("TrainingDataExporter initialized");
    }
    
    /**
     * Exports training dataset to JSON format.
     * 
     * @param dataset training dataset to export
     * @param outputPath output file path
     * @throws DatasetException if export fails
     */
    public void exportToJSON(TrainingDataset dataset, Path outputPath) throws DatasetException {
        LOGGER.info("Exporting training dataset to: {}", outputPath);
        
        try {
            // Create root JSON object
            ObjectNode root = objectMapper.createObjectNode();
            
            // Add metadata
            root.put("exportedAt", java.time.LocalDateTime.now().toString());
            root.put("version", "2.0.0");
            root.put("source", "SciPathJ Dataset Creator");
            
            // Add classes array
            ArrayNode classesArray = objectMapper.createArrayNode();
            for (ClassificationClass clazz : dataset.classes()) {
                classesArray.add(clazz.name());
            }
            root.set("classes", classesArray);
            
            // Add class colors
            ObjectNode classColors = objectMapper.createObjectNode();
            for (ClassificationClass clazz : dataset.classes()) {
                classColors.put(clazz.name(), clazz.color());
            }
            root.set("classColors", classColors);
            
            // Add classified ROIs data
            ObjectNode classifiedROIs = objectMapper.createObjectNode();
            
            // Group ROIs by image
            Map<String, List<UserROI>> roisByImage = dataset.classifiedROIs().values().stream()
                .collect(java.util.stream.Collectors.groupingBy(UserROI::getImageFileName));
            
            for (Map.Entry<String, List<UserROI>> imageEntry : roisByImage.entrySet()) {
                String imageName = imageEntry.getKey();
                List<UserROI> imageROIs = imageEntry.getValue();
                
                ObjectNode imageData = objectMapper.createObjectNode();
                
                for (UserROI roi : imageROIs) {
                    String roiKey = imageName + ":" + roi.getName();
                    ObjectNode roiData = createROIData(roi, dataset.features().get(roiKey));
                    imageData.set(roi.getName(), roiData);
                }
                
                classifiedROIs.set(imageName, imageData);
            }
            
            root.set("classifiedROIs", classifiedROIs);
            
            // Add dataset statistics
            ObjectNode statistics = objectMapper.createObjectNode();
            statistics.put("totalROIs", dataset.getROICount());
            statistics.put("totalFeatures", dataset.getFeatureCount());
            statistics.put("totalClasses", dataset.getClassCount());
            
            // Add class distribution
            ObjectNode classDistribution = objectMapper.createObjectNode();
            Map<String, Long> distribution = dataset.getClassDistribution();
            for (Map.Entry<String, Long> entry : distribution.entrySet()) {
                classDistribution.put(entry.getKey(), entry.getValue());
            }
            statistics.set("classDistribution", classDistribution);
            
            root.set("statistics", statistics);
            
            // Write to file
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(outputPath, jsonOutput);
            
            LOGGER.info("Training dataset exported successfully: {} ROIs, {} features, {} classes",
                       dataset.getROICount(), dataset.getFeatureCount(), dataset.getClassCount());
            
        } catch (IOException e) {
            LOGGER.error("Failed to export training dataset", e);
            throw new DatasetException("Failed to export training dataset to JSON", e);
        }
    }
    
    /**
     * Creates JSON data for a single ROI including its features and classification.
     */
    private ObjectNode createROIData(UserROI roi, Map<String, Double> features) {
        ObjectNode roiData = objectMapper.createObjectNode();
        
        // Add ROI metadata
        roiData.put("type", roi.getType().name());
        roiData.put("assignedClass", roi.getAssignedClass());
        
        // Add different components based on ROI type
        switch (roi.getType()) {
            case CELL -> {
                ObjectNode cellData = objectMapper.createObjectNode();
                if (features != null) {
                    for (Map.Entry<String, Double> feature : features.entrySet()) {
                        cellData.put(feature.getKey(), feature.getValue());
                    }
                }
                roiData.set("Cell", cellData);
            }
            case NUCLEUS -> {
                ObjectNode nucleusData = objectMapper.createObjectNode();
                if (features != null) {
                    for (Map.Entry<String, Double> feature : features.entrySet()) {
                        nucleusData.put(feature.getKey(), feature.getValue());
                    }
                }
                roiData.set("Nucleus", nucleusData);
            }
            case CYTOPLASM -> {
                ObjectNode cytoplasmData = objectMapper.createObjectNode();
                if (features != null) {
                    for (Map.Entry<String, Double> feature : features.entrySet()) {
                        cytoplasmData.put(feature.getKey(), feature.getValue());
                    }
                }
                roiData.set("Cytoplasm", cytoplasmData);
            }
            case VESSEL -> {
                ObjectNode vesselData = objectMapper.createObjectNode();
                if (features != null) {
                    for (Map.Entry<String, Double> feature : features.entrySet()) {
                        vesselData.put(feature.getKey(), feature.getValue());
                    }
                }
                roiData.set("Vessel", vesselData);
            }
            default -> {
                // For other types, put features directly
                if (features != null) {
                    for (Map.Entry<String, Double> feature : features.entrySet()) {
                        roiData.put(feature.getKey(), feature.getValue());
                    }
                }
            }
        }
        
        return roiData;
    }
    
    /**
     * Validates that the dataset can be exported.
     */
    public void validateDataset(TrainingDataset dataset) throws DatasetException {
        if (dataset.getROICount() == 0) {
            throw new DatasetException("Cannot export empty dataset");
        }
        
        if (dataset.getClassCount() == 0) {
            throw new DatasetException("Cannot export dataset without classes");
        }
        
        if (dataset.getFeatureCount() == 0) {
            throw new DatasetException("Cannot export dataset without features");
        }
        
        // Check that all ROIs have features
        for (String roiKey : dataset.classifiedROIs().keySet()) {
            if (!dataset.features().containsKey(roiKey)) {
                throw new DatasetException("ROI missing features: " + roiKey);
            }
        }
        
        LOGGER.debug("Dataset validation passed: {} ROIs, {} features, {} classes",
                    dataset.getROICount(), dataset.getFeatureCount(), dataset.getClassCount());
    }
}