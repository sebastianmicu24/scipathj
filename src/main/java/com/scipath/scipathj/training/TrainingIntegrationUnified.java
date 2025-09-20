package com.scipath.scipathj.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Arrays;

import com.scipath.scipathj.training.XGBoostTrainerUnified;
import com.scipath.scipathj.training.TrainingSettings;

/**
 * Integration point for the simplified training system.
 * Provides a clean interface between UI components and the new simplified trainers.
 * 
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class TrainingIntegrationUnified {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingIntegrationUnified.class);
    
    /**
     * Trains a model using the simplified pipeline.
     * 
     * @param trainingDataFile JSON file exported from dataset creation
     * @param outputModelName name for the output model (optional)
     * @return training results including accuracy metrics
     */
    public static Map<String, Object> trainModel(File trainingDataFile, String outputModelName) {
        logger.info("Starting simplified training with data file: {}", trainingDataFile.getAbsolutePath());
        
        try {
            // Create training settings with sensible defaults
            TrainingSettings settings = new TrainingSettings();
            settings.setTrainRatio(0.7f);
            settings.setLearningRate(0.1f);
            settings.setMaxDepth(6);
            settings.setNumTrees(100);
            
            // Use all available features by default
            // The SimplifiedJSONDataReader will read the actual feature names from the JSON
            
            // Create and run trainer
            try (XGBoostTrainerUnified trainer = new XGBoostTrainerUnified(trainingDataFile, settings)) {
                Map<String, Object> results = trainer.trainModel();
                
                logger.info("Training completed successfully");
                logger.info("Results: {}", results);
                
                return results;
            }
            
        } catch (Exception e) {
            logger.error("Training failed: {}", e.getMessage(), e);
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Trains a model with custom settings.
     */
    public static Map<String, Object> trainModelWithSettings(File trainingDataFile, TrainingSettings settings) {
        logger.info("Starting training with custom settings for file: {}", trainingDataFile.getAbsolutePath());
        
        try (XGBoostTrainerUnified trainer = new XGBoostTrainerUnified(trainingDataFile, settings)) {
            return trainer.trainModel();
        } catch (Exception e) {
            logger.error("Training with custom settings failed: {}", e.getMessage(), e);
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates that a training data file is properly formatted.
     */
    public static boolean validateTrainingDataFile(File trainingDataFile) {
        if (!trainingDataFile.exists()) {
            logger.warn("Training data file does not exist: {}", trainingDataFile.getAbsolutePath());
            return false;
        }
        
        try {
            // Try to load the data file to validate format
            JSONDataReader reader = new JSONDataReader(trainingDataFile, null);
            
            boolean hasFeatures = !reader.getAllCellFeatures().isEmpty();
            boolean hasLabels = !reader.getTrainingLabels().isEmpty();
            boolean hasClasses = !reader.getClassNameToIdMap().isEmpty();
            
            logger.info("Training data validation - Features: {}, Labels: {}, Classes: {}", 
                       hasFeatures, hasLabels, hasClasses);
            
            return hasFeatures && hasLabels && hasClasses;
            
        } catch (Exception e) {
            logger.error("Training data validation failed: {}", e.getMessage());
            return false;
        }
    }
}