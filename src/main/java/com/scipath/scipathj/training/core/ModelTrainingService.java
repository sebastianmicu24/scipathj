package com.scipath.scipathj.training.core;

import com.scipath.scipathj.training.data.TrainingDataReader;
import com.scipath.scipathj.training.model.TrainingConfiguration;
import com.scipath.scipathj.training.model.TrainingResult;
import com.scipath.scipathj.training.persistence.ModelPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ml.dmlc.xgboost4j.java.Booster;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Clean service for XGBoost model training operations.
 * Replaces the monolithic XGBoostTrainer with proper separation of concerns.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public class ModelTrainingService implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelTrainingService.class);
    
    private final DataPreprocessor preprocessor;
    private final ModelValidator validator;
    private final ModelEvaluator evaluator;
    private final ModelPersistenceService persistence;
    private final ExecutorService executorService;
    
    /**
     * Creates a new model training service with dependency injection.
     */
    public ModelTrainingService(DataPreprocessor preprocessor,
                               ModelValidator validator,
                               ModelEvaluator evaluator,
                               ModelPersistenceService persistence) {
        this.preprocessor = preprocessor;
        this.validator = validator;
        this.evaluator = evaluator;
        this.persistence = persistence;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ModelTraining-Thread");
            t.setDaemon(true);
            return t;
        });
        
        LOGGER.info("ModelTrainingService initialized");
    }
    
    /**
     * Trains a model synchronously with the given configuration.
     * 
     * @param config training configuration
     * @return training result
     * @throws ModelTrainingException if training fails
     */
    public TrainingResult trainModel(TrainingConfiguration config) throws ModelTrainingException {
        LOGGER.info("Starting model training with configuration: {}", config.getDisplayName());
        
        try (ResourceManager rm = new ResourceManager()) {
            // Validate configuration
            validator.validateConfiguration(config);
            
            // Load and validate training data
            TrainingDataReader dataReader = rm.manage(new TrainingDataReader(config.dataFile()));
            validator.validateTrainingData(dataReader);
            
            // Preprocess data
            var preprocessedData = rm.manage(preprocessor.createTrainTestSplit(dataReader, config));
            
            // Train model
            Booster booster = preprocessor.trainXGBoostModel(preprocessedData, config);
            rm.manage(booster, () -> {
                try { booster.dispose(); } catch (Exception e) { /* log error */ }
            });
            
            // Evaluate model
            TrainingResult.EvaluationMetrics evaluation = evaluator.evaluate(booster, preprocessedData.testMatrix());
            
            // Save model
            var modelBundle = persistence.saveModel(booster, config, evaluation, dataReader);
            
            // Create result
            TrainingResult result = new TrainingResult(
                modelBundle,
                evaluation,
                config,
                dataReader.getStatistics()
            );
            
            LOGGER.info("Model training completed successfully: {}", result.getSummary());
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Model training failed", e);
            throw new ModelTrainingException("Model training failed", e);
        }
    }
    
    /**
     * Trains a model asynchronously.
     * 
     * @param config training configuration
     * @return future containing training result
     */
    public CompletableFuture<TrainingResult> trainModelAsync(TrainingConfiguration config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return trainModel(config);
            } catch (ModelTrainingException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Trains a model with progress monitoring.
     * 
     * @param config training configuration
     * @param progressObserver progress observer for monitoring
     * @return future containing training result
     */
    public CompletableFuture<TrainingResult> trainModelWithProgress(
            TrainingConfiguration config, 
            TrainingProgressObserver progressObserver) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressObserver.onProgressUpdate(0, 100, "Starting training...");
                
                // Set up progress tracking in preprocessor and other components
                preprocessor.setProgressObserver(progressObserver);
                
                TrainingResult result = trainModel(config);
                
                progressObserver.onProgressUpdate(100, 100, "Training completed");
                progressObserver.onComplete(result);
                
                return result;
                
            } catch (Exception e) {
                progressObserver.onError(e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Validates a training configuration without actually training.
     * 
     * @param config configuration to validate
     * @throws ModelTrainingException if configuration is invalid
     */
    public void validateConfiguration(TrainingConfiguration config) throws ModelTrainingException {
        try {
            validator.validateConfiguration(config);
            LOGGER.info("Configuration validation passed");
        } catch (Exception e) {
            LOGGER.error("Configuration validation failed", e);
            throw new ModelTrainingException("Configuration validation failed", e);
        }
    }
    
    /**
     * Validates training data without training.
     * 
     * @param dataFile training data file
     * @throws ModelTrainingException if data is invalid
     */
    public void validateTrainingData(File dataFile) throws ModelTrainingException {
        try (TrainingDataReader dataReader = new TrainingDataReader(dataFile)) {
            validator.validateTrainingData(dataReader);
            LOGGER.info("Training data validation passed: {}", dataReader.getStatistics());
        } catch (Exception e) {
            LOGGER.error("Training data validation failed", e);
            throw new ModelTrainingException("Training data validation failed", e);
        }
    }
    
    @Override
    public void close() {
        LOGGER.debug("Closing ModelTrainingService");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                LOGGER.warn("Training executor did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for executor shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close other services if they implement AutoCloseable
        try {
            if (persistence instanceof AutoCloseable) {
                ((AutoCloseable) persistence).close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing persistence service", e);
        }
    }
}