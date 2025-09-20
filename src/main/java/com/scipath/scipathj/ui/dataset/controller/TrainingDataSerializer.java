package com.scipath.scipathj.ui.dataset.controller;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for serializing training data to JSON format.
 * Follows Single Responsibility Principle - only handles JSON serialization.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrainingDataSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingDataSerializer.class);

    /**
     * Save training data to JSON format for XGBoost training.
     *
     * @param cellData The organized cell data
     * @param outputFile The file to save to
     * @throws IOException if saving fails
     */
    public void saveTrainingDataToJson(Map<String, Map<String, Object>> cellData, File outputFile) throws IOException {
        LOGGER.info("Saving XGBoost training data to JSON file: {}", outputFile.getName());
        
        try {
            // Create JSON structure for XGBoost training
            Map<String, Object> jsonData = createJsonStructure(cellData);

            // Save using Jackson ObjectMapper
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonData);

            LOGGER.info("Successfully saved XGBoost training data with {} cells", cellData.size());
                       
        } catch (Exception e) {
            throw new IOException("Failed to save XGBoost training data to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Create the complete JSON structure for training data.
     *
     * @param cellData The cell data to serialize
     * @return Complete JSON structure
     */
    private Map<String, Object> createJsonStructure(Map<String, Map<String, Object>> cellData) {
        Map<String, Object> jsonData = new LinkedHashMap<>();
        
        // Add metadata
        jsonData.put("timestamp", new java.util.Date().toString());
        jsonData.put("extractionMethod", "comprehensive_feature_extraction");
        jsonData.put("featureSetCount", cellData.size());
        
        // Create class information from cell data
        List<Map<String, Object>> classInfo = extractClassInformation(cellData);
        jsonData.put("classes", classInfo);
        
        // Add the main cell data
        jsonData.put("cellData", cellData);
        
        // Add feature extraction settings info
        jsonData.put("featureExtractionSettings", 
                    "Comprehensive feature extraction with H&E deconvolution, vessel distance, neighbor analysis");

        return jsonData;
    }

    /**
     * Extract class information from cell data for metadata.
     *
     * @param cellData The cell data
     * @return List of class information maps
     */
    private List<Map<String, Object>> extractClassInformation(Map<String, Map<String, Object>> cellData) {
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

        LOGGER.info("Extracted {} unique classes: {}", uniqueClasses.size(), uniqueClasses);
        return classInfo;
    }

    /**
     * Generate a consistent color for a class based on its name.
     *
     * @param className The class name
     * @return A hex color string
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
     * Validate that the output file can be written to.
     *
     * @param outputFile The file to validate
     * @throws IOException if the file cannot be written to
     */
    public void validateOutputFile(File outputFile) throws IOException {
        if (outputFile == null) {
            throw new IOException("Output file cannot be null");
        }

        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new IOException("Could not create parent directories for: " + outputFile.getAbsolutePath());
            }
        }

        // Check if we can write to the location
        if (outputFile.exists() && !outputFile.canWrite()) {
            throw new IOException("Cannot write to existing file: " + outputFile.getAbsolutePath());
        }

        LOGGER.debug("Output file validation passed: {}", outputFile.getAbsolutePath());
    }
}