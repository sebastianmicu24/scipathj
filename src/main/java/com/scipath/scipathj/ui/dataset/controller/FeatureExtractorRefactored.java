package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.ui.dataset.controller.ROICategorizer.ROICategorizedData;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrator for feature extraction following SOLID principles.
 * This class coordinates the work of specialized components to extract training data.
 * 
 * Responsibilities:
 * - Orchestrate the overall feature extraction workflow
 * - Coordinate between specialized components
 * - Handle progress reporting
 * - Manage exceptions and error reporting
 *
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 1.0.0
 */
public class FeatureExtractorRefactored {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractorRefactored.class);

    // Specialized components following SOLID principles
    private final ROICategorizer roiCategorizer;
    private final FeatureExtractionEngine featureExtractionEngine;
    private final TrainingDataOrganizer dataOrganizer;
    private final TrainingDataSerializer dataSerializer;
    private final FeatureValidator validator;

    // Callbacks for progress updates
    private final Consumer<String> progressUpdater;
    private final Consumer<Double> percentageUpdater;

    // State
    private Map<String, Map<String, Object>> currentFeatures;

    /**
     * Creates a new refactored FeatureExtractor with dependency injection.
     *
     * @param configManager Configuration manager for settings
     * @param progressUpdater Callback for progress update messages
     * @param percentageUpdater Callback for progress percentage updates
     */
    public FeatureExtractorRefactored(
            ConfigurationManager configManager,
            Consumer<String> progressUpdater,
            Consumer<Double> percentageUpdater) {
        
        this.progressUpdater = progressUpdater;
        this.percentageUpdater = percentageUpdater;

        // Initialize specialized components
        this.roiCategorizer = new ROICategorizer();
        this.featureExtractionEngine = new ComprehensiveFeatureExtractionEngine(configManager);
        this.dataOrganizer = new TrainingDataOrganizer(roiCategorizer);
        this.dataSerializer = new TrainingDataSerializer();
        this.validator = new FeatureValidator();

        LOGGER.info("FeatureExtractor initialized with specialized components following SOLID principles");
    }

    /**
     * Extract features from classified ROIs and save training data.
     * This is the main orchestration method that coordinates all specialized components.
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

        LOGGER.info("Starting orchestrated feature extraction for training data");

        try {
            // Step 1: Validate and categorize ROIs
            updateProgress("Categorizing and validating ROIs", 0.1);
            ROICategorizedData categorizedData = categorizeAndValidateROIs(allROIs);

            // Step 2: Extract features using comprehensive analysis
            updateProgress("Extracting comprehensive features", 0.3);
            Map<String, Map<String, Object>> extractedFeatures = extractFeatures(categorizedData, imagePlus);

            // Step 3: Organize features for XGBoost training format
            updateProgress("Organizing features for training", 0.6);
            Map<String, Map<String, Object>> organizedData = organizeFeatures(extractedFeatures, categorizedData, imagePlus);

            // Step 4: Filter to classified ROIs only
            updateProgress("Filtering classified ROIs", 0.8);
            Map<String, Map<String, Object>> filteredData = filterClassifiedData(organizedData);

            // Step 5: Validate complete workflow
            updateProgress("Validating training data", 0.9);
            validateCompleteWorkflow(categorizedData, extractedFeatures, organizedData, filteredData);

            // Step 6: Save to JSON
            updateProgress("Saving training data to JSON", 0.95);
            saveTrainingData(filteredData, outputFile);

            updateProgress("Feature extraction completed successfully", 1.0);
            
            // Store for later access
            this.currentFeatures = extractedFeatures;

            LOGGER.info("Orchestrated feature extraction completed successfully. Processed {} cells", filteredData.size());

        } catch (Exception e) {
            LOGGER.error("Feature extraction workflow failed: {}", e.getMessage(), e);
            updateProgress("Feature extraction failed: " + e.getMessage(), -1.0);
            throw e;
        }
    }

    /**
     * Categorize ROIs and validate them for training data extraction.
     */
    private ROICategorizedData categorizeAndValidateROIs(Map<String, UserROI> allROIs) throws Exception {
        if (allROIs.isEmpty()) {
            throw new Exception("No ROIs found in overlay for training data extraction");
        }

        ROICategorizedData categorizedData = roiCategorizer.categorizeROIs(allROIs);
        validator.validateCategorizedData(categorizedData);

        return categorizedData;
    }

    /**
     * Extract features using the comprehensive analysis engine.
     */
    private Map<String, Map<String, Object>> extractFeatures(
            ROICategorizedData categorizedData, 
            ImagePlus imagePlus) throws Exception {
        
        if (!featureExtractionEngine.isAvailable()) {
            throw new Exception("Feature extraction engine is not available. Check configuration settings.");
        }

        LOGGER.info("Using feature extraction engine: {}", featureExtractionEngine.getEngineName());
        
        Map<String, Map<String, Object>> extractedFeatures = featureExtractionEngine.extractFeatures(categorizedData, imagePlus);
        
        // Validate features
        validator.validateComprehensiveFeatures(extractedFeatures);
        
        return extractedFeatures;
    }

    /**
     * Organize features into XGBoost training format.
     */
    private Map<String, Map<String, Object>> organizeFeatures(
            Map<String, Map<String, Object>> extractedFeatures,
            ROICategorizedData categorizedData,
            ImagePlus imagePlus) throws Exception {
        
        String imageName = imagePlus != null ? imagePlus.getTitle() : "Unknown_Image.tif";
        
        Map<String, Map<String, Object>> organizedData = dataOrganizer.organizeFeaturesForXGBoostTraining(
                extractedFeatures, categorizedData, imageName);
        
        validator.validateOrganizedData(organizedData);
        
        return organizedData;
    }

    /**
     * Filter data to only include classified ROIs.
     */
    private Map<String, Map<String, Object>> filterClassifiedData(
            Map<String, Map<String, Object>> organizedData) throws Exception {
        
        Map<String, Map<String, Object>> filteredData = dataOrganizer.filterClassifiedROIsForTraining(organizedData);
        
        validator.validateFilteredData(filteredData);
        
        return filteredData;
    }

    /**
     * Validate the complete workflow.
     */
    private void validateCompleteWorkflow(
            ROICategorizedData categorizedData,
            Map<String, Map<String, Object>> extractedFeatures,
            Map<String, Map<String, Object>> organizedData,
            Map<String, Map<String, Object>> filteredData) throws Exception {
        
        validator.validateCompleteWorkflow(categorizedData, extractedFeatures, organizedData, filteredData);
    }

    /**
     * Save training data to JSON format.
     */
    private void saveTrainingData(
            Map<String, Map<String, Object>> filteredData, 
            File outputFile) throws IOException {
        
        dataSerializer.validateOutputFile(outputFile);
        dataSerializer.saveTrainingDataToJson(filteredData, outputFile);
    }

    /**
     * Update progress through callback.
     */
    private void updateProgress(String message, double percentage) {
        if (progressUpdater != null) {
            progressUpdater.accept(message);
        }
        if (percentageUpdater != null && percentage >= 0) {
            percentageUpdater.accept(percentage);
        }
        
        LOGGER.debug("Progress: {} ({}%)", message, (int)(percentage * 100));
    }

    /**
     * Get currently extracted features.
     */
    public Map<String, Map<String, Object>> getCurrentFeatures() {
        return currentFeatures != null ? new HashMap<>(currentFeatures) : null;
    }

    /**
     * Check if feature extractor is available and properly configured.
     */
    public boolean isFeatureExtractorAvailable() {
        return featureExtractionEngine != null && featureExtractionEngine.isAvailable();
    }

    /**
     * Get information about the current feature extraction engine.
     */
    public String getFeatureExtractionEngineInfo() {
        if (featureExtractionEngine != null) {
            return featureExtractionEngine.getEngineName() + " (Available: " + featureExtractionEngine.isAvailable() + ")";
        }
        return "No feature extraction engine configured";
    }

    /**
     * Perform a quick validation check before starting extraction.
     */
    public void validateBeforeExtraction(Map<String, UserROI> allROIs, ImagePlus imagePlus) throws Exception {
        if (allROIs == null || allROIs.isEmpty()) {
            throw new Exception("No ROIs provided for feature extraction");
        }

        if (imagePlus == null) {
            throw new Exception("ImagePlus is required for comprehensive feature extraction");
        }

        if (!isFeatureExtractorAvailable()) {
            throw new Exception("Feature extraction engine is not available. Check configuration.");
        }

        // Quick categorization check
        ROICategorizedData categorizedData = roiCategorizer.categorizeROIs(allROIs);
        validator.validateCategorizedData(categorizedData);

        LOGGER.info("Pre-extraction validation passed");
    }
}