package com.scipath.scipathj.training.core;

import com.scipath.scipathj.training.data.TrainingDataReader;
import com.scipath.scipathj.training.model.TrainingConfiguration;

/**
 * Interface for validating training configurations and data.
 * Provides validation logic for training pipeline.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ModelValidator {
    
    /**
     * Validates training configuration.
     * 
     * @param config configuration to validate
     * @throws ModelTrainingException if configuration is invalid
     */
    void validateConfiguration(TrainingConfiguration config) throws ModelTrainingException;
    
    /**
     * Validates training data.
     * 
     * @param dataReader training data reader
     * @throws ModelTrainingException if data is invalid
     */
    void validateTrainingData(TrainingDataReader dataReader) throws ModelTrainingException;
    
    /**
     * Default implementation of ModelValidator.
     */
    class DefaultModelValidator implements ModelValidator {
        
        @Override
        public void validateConfiguration(TrainingConfiguration config) throws ModelTrainingException {
            if (config == null) {
                throw new ModelTrainingException("Configuration cannot be null");
            }
            
            if (config.dataFile() == null || !config.dataFile().exists()) {
                throw new ModelTrainingException("Data file must exist");
            }
            
            if (config.outputDir() == null) {
                throw new ModelTrainingException("Output directory cannot be null");
            }
            
            var params = config.parameters();
            if (params.learningRate() <= 0 || params.learningRate() > 1) {
                throw new ModelTrainingException("Learning rate must be between 0 and 1");
            }
            
            if (params.maxDepth() <= 0 || params.maxDepth() > 20) {
                throw new ModelTrainingException("Max depth must be between 1 and 20");
            }
            
            if (params.numTrees() <= 0 || params.numTrees() > 10000) {
                throw new ModelTrainingException("Number of trees must be between 1 and 10000");
            }
        }
        
        @Override
        public void validateTrainingData(TrainingDataReader dataReader) throws ModelTrainingException {
            if (dataReader == null) {
                throw new ModelTrainingException("Data reader cannot be null");
            }
            
            if (dataReader.getFeatures().isEmpty()) {
                throw new ModelTrainingException("No features found in training data");
            }
            
            if (dataReader.getLabels().isEmpty()) {
                throw new ModelTrainingException("No labels found in training data");
            }
            
            if (dataReader.getFeatureNames().isEmpty()) {
                throw new ModelTrainingException("No feature names found in training data");
            }
            
            if (dataReader.getClassNameToIdMap().isEmpty()) {
                throw new ModelTrainingException("No classes found in training data");
            }
            
            // Check for class imbalance warning
            var statistics = dataReader.getStatistics();
            if (statistics.numClasses() < 2) {
                throw new ModelTrainingException("At least 2 classes required for training");
            }
            
            if (statistics.totalSamples() < 10) {
                throw new ModelTrainingException("At least 10 samples required for training");
            }
        }
    }
}