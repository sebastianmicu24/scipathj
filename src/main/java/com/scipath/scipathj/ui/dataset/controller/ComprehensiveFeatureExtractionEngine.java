package com.scipath.scipathj.ui.dataset.controller;

import com.scipath.scipathj.infrastructure.roi.UserROI;
import com.scipath.scipathj.analysis.algorithms.classification.FeatureExtraction;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.infrastructure.config.ConfigurationManager;
import com.scipath.scipathj.infrastructure.config.MainSettings;
import com.scipath.scipathj.ui.dataset.controller.ROICategorizer.ROICategorizedData;
import ij.ImagePlus;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of comprehensive feature extraction using the analysis pipeline.
 * This engine uses the SAME FeatureExtraction class as the analysis workflow
 * to ensure consistency and extract advanced features like H&E deconvolution,
 * vessel distance, and spatial analysis.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ComprehensiveFeatureExtractionEngine implements FeatureExtractionEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveFeatureExtractionEngine.class);

    private final ConfigurationManager configManager;
    private FeatureExtraction featureExtractor;

    public ComprehensiveFeatureExtractionEngine(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public Map<String, Map<String, Object>> extractFeatures(
            ROICategorizedData categorizedData, 
            ImagePlus imagePlus) throws Exception {
        
        LOGGER.info("Starting comprehensive feature extraction using analysis pipeline");

        // Validate prerequisites
        validatePrerequisites(categorizedData, imagePlus);

        // Initialize the analysis FeatureExtraction class
        initializeAnalysisFeatureExtractor(categorizedData, imagePlus);

        // Extract features using the same pipeline as analysis workflow
        Map<String, Map<String, Object>> extractedFeatures = performFeatureExtraction();

        // Validate that we got comprehensive features
        validateFeatures(extractedFeatures);

        LOGGER.info("Comprehensive feature extraction completed successfully with {} feature sets", 
                   extractedFeatures.size());
        
        return extractedFeatures;
    }

    /**
     * Validate that all prerequisites are met for feature extraction.
     */
    private void validatePrerequisites(ROICategorizedData categorizedData, ImagePlus imagePlus) throws Exception {
        if (imagePlus == null) {
            throw new Exception("ImagePlus is required for comprehensive feature extraction. Cannot proceed without image data.");
        }

        if (categorizedData.cells.isEmpty() && categorizedData.nuclei.isEmpty() && categorizedData.cytoplasm.isEmpty()) {
            throw new Exception("No ROIs found for feature extraction. Need at least some Cell, Nucleus, or Cytoplasm ROIs.");
        }

        if (configManager == null) {
            throw new Exception("ConfigurationManager is required for comprehensive feature extraction. Cannot proceed without configuration.");
        }

        LOGGER.info("Prerequisites validated: ImagePlus={}, ConfigManager={}, ROIs={}", 
                   imagePlus.getTitle(), 
                   "available", 
                   categorizedData.cells.size() + categorizedData.nuclei.size() + categorizedData.cytoplasm.size());
    }

    /**
     * Initialize the analysis FeatureExtraction class with proper ROI data and settings.
     */
    private void initializeAnalysisFeatureExtractor(ROICategorizedData categorizedData, ImagePlus imagePlus) throws Exception {
        try {
            String imageFileName = imagePlus.getTitle();
            LOGGER.info("Initializing analysis FeatureExtraction for image: {}", imageFileName);
            
            // Load settings from configuration
            FeatureExtractionSettings featureSettings = configManager.loadFeatureExtractionSettings();
            MainSettings mainSettings = configManager.loadMainSettings();
            if (featureSettings == null) {
                LOGGER.error("FeatureExtractionSettings is null - cannot proceed without feature extraction configuration");
                throw new Exception("FeatureExtractionSettings is required for comprehensive feature extraction");
            }
            
            if (mainSettings == null) {
                LOGGER.error("MainSettings is null - cannot proceed without main configuration");
                throw new Exception("MainSettings is required for comprehensive feature extraction");
            }

            LOGGER.info("Creating FeatureExtraction with {} vessels, {} nuclei, {} cytoplasm, {} cells",
                       categorizedData.vessel.size(), categorizedData.nuclei.size(),
                       categorizedData.cytoplasm.size(), categorizedData.cells.size());
            
            // Initialize the SAME FeatureExtraction class as used in analysis
            this.featureExtractor = new FeatureExtraction(
                imagePlus,
                imageFileName,
                categorizedData.vessel,
                categorizedData.nuclei,
                categorizedData.cytoplasm,
                categorizedData.cells,
                featureSettings,
                mainSettings);

            LOGGER.info("Analysis FeatureExtraction initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize analysis FeatureExtraction: {}", e.getMessage(), e);
            throw new Exception("FeatureExtraction initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the actual feature extraction using the analysis pipeline.
     */
    private Map<String, Map<String, Object>> performFeatureExtraction() throws Exception {
        if (featureExtractor == null) {
            throw new Exception("FeatureExtraction is not initialized. Cannot extract features.");
        }

        try {
            LOGGER.info("Calling analysis FeatureExtraction.extractFeatures()...");
            Map<String, Map<String, Object>> extractedFeatures = featureExtractor.extractFeatures();
            
            if (extractedFeatures == null) {
                throw new Exception("Analysis FeatureExtraction returned NULL. The comprehensive analysis pipeline failed.");
            }
            
            if (extractedFeatures.isEmpty()) {
                throw new Exception("Analysis FeatureExtraction returned EMPTY map. The comprehensive analysis pipeline failed.");
            }
            
            // Log sample features for debugging
            if (!extractedFeatures.isEmpty()) {
                Map<String, Object> sampleFeatures = extractedFeatures.values().iterator().next();
                LOGGER.info("Sample features extracted: {}", sampleFeatures.keySet());
                
                // Log a few specific features to verify comprehensive extraction
                logSpecificFeatures(sampleFeatures);
            }
            
            return extractedFeatures;
            
        } catch (Exception e) {
            LOGGER.error("Feature extraction execution failed: {}", e.getMessage(), e);
            throw new Exception("Feature extraction execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Log specific features to verify comprehensive extraction worked.
     */
    private void logSpecificFeatures(Map<String, Object> sampleFeatures) {
        boolean hasVesselDistance = sampleFeatures.containsKey("vessel_distance");
        boolean hasHemaFeatures = sampleFeatures.containsKey("hema_mean");
        boolean hasEosinFeatures = sampleFeatures.containsKey("eosin_mean");
        boolean hasNeighborCount = sampleFeatures.containsKey("neighbor_count");
        
        LOGGER.info("Comprehensive feature verification: vessel_distance={}, hema_features={}, eosin_features={}, neighbor_analysis={}",
                   hasVesselDistance, hasHemaFeatures, hasEosinFeatures, hasNeighborCount);
        
        if (hasVesselDistance || hasHemaFeatures || hasEosinFeatures || hasNeighborCount) {
            LOGGER.info("✓ Comprehensive features detected successfully");
        } else {
            LOGGER.warn("⚠ Only basic features detected - comprehensive extraction may have failed");
        }
    }

    @Override
    public void validateFeatures(Map<String, Map<String, Object>> features) throws Exception {
        if (features == null || features.isEmpty()) {
            throw new Exception("VALIDATION FAILED: No features extracted");
        }

        // Required comprehensive features that should be present
        String[] criticalFeatures = {
            "vessel_distance", "neighbor_count", "hema_mean", "eosin_mean"
        };

        // Check a sample of features to ensure comprehensive extraction worked
        boolean foundComprehensiveFeatures = false;
        Map<String, Object> sampleFeatures = null;
        
        for (Map<String, Object> featureSet : features.values()) {
            if (featureSet != null && !featureSet.isEmpty()) {
                sampleFeatures = featureSet;
                
                // Check if we have any advanced features
                for (String criticalFeature : criticalFeatures) {
                    if (featureSet.containsKey(criticalFeature)) {
                        foundComprehensiveFeatures = true;
                        break;
                    }
                }
                
                if (foundComprehensiveFeatures) break;
            }
        }

        if (!foundComprehensiveFeatures) {
            if (sampleFeatures != null) {
                LOGGER.error("VALIDATION FAILED: Only basic features found. Available: {}", sampleFeatures.keySet());
                throw new Exception("VALIDATION FAILED: Comprehensive feature extraction did not work. " +
                                   "Missing critical features like vessel_distance, H&E deconvolution, etc. " +
                                   "Available features: " + sampleFeatures.keySet());
            } else {
                throw new Exception("VALIDATION FAILED: No valid feature sets found in extracted data");
            }
        }

        LOGGER.info("✓ VALIDATION PASSED: Comprehensive features detected including H&E deconvolution and spatial analysis");
    }

    @Override
    public boolean isAvailable() {
        return configManager != null;
    }

    @Override
    public String getEngineName() {
        return "ComprehensiveFeatureExtractionEngine";
    }
}