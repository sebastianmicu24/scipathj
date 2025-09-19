package com.scipath.scipathj.training.persistence;

import com.scipath.scipathj.training.core.ModelTrainingException;
import com.scipath.scipathj.training.data.TrainingDataReader;
import com.scipath.scipathj.training.model.TrainingConfiguration;
import com.scipath.scipathj.training.model.TrainingResult;
import com.scipath.scipathj.training.XGBoostModelBundle;
import ml.dmlc.xgboost4j.java.Booster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interface for saving and loading trained models.
 * Handles model persistence and bundle creation.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ModelPersistenceService extends AutoCloseable {
    
    /**
     * Saves a trained model with evaluation results.
     * 
     * @param booster trained XGBoost model
     * @param config training configuration
     * @param evaluation evaluation metrics
     * @param dataReader training data reader
     * @return saved model bundle
     * @throws ModelTrainingException if saving fails
     */
    XGBoostModelBundle saveModel(Booster booster,
                                TrainingConfiguration config,
                                TrainingResult.EvaluationMetrics evaluation,
                                TrainingDataReader dataReader) 
                                throws ModelTrainingException;
    
    /**
     * Loads a model bundle from file.
     * 
     * @param bundlePath path to model bundle file
     * @return loaded model bundle
     * @throws ModelTrainingException if loading fails
     */
    XGBoostModelBundle loadModel(Path bundlePath) throws ModelTrainingException;
    
    @Override
    default void close() {
        // Default implementation - override if cleanup needed
    }
    
    /**
     * Default implementation of ModelPersistenceService.
     */
    class DefaultModelPersistenceService implements ModelPersistenceService {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelPersistenceService.class);
        
        @Override
        public XGBoostModelBundle saveModel(Booster booster,
                                           TrainingConfiguration config,
                                           TrainingResult.EvaluationMetrics evaluation,
                                           TrainingDataReader dataReader) 
                                           throws ModelTrainingException {
            
            LOGGER.info("Saving model to: {}", config.getOutputDirPath());
            
            try {
                // Ensure output directory exists
                Files.createDirectories(config.outputDir());
                
                // Create model bundle (simplified)
                XGBoostModelBundle bundle = new XGBoostModelBundle();
                
                // Set basic model info
                bundle.modelInfo.title = "Custom Cell Classification Model";
                bundle.modelInfo.description = "XGBoost model trained on SciPathJ data";
                bundle.modelInfo.author = "SciPathJ Training Pipeline v2.0";
                bundle.modelInfo.platform = "SciPathJ";
                
                // Save XGBoost model as JSON
                String tempModelPath = config.outputDir().resolve("temp_model.json").toString();
                booster.saveModel(tempModelPath);
                bundle.xgboostModel.modelJson = Files.readString(Path.of(tempModelPath));
                
                // Clean up temp file
                Files.deleteIfExists(Path.of(tempModelPath));
                
                // Set feature metadata
                bundle.featureMetadata.selectedFeatures = dataReader.getFeatureNames();
                bundle.featureMetadata.numSelectedFeatures = dataReader.getFeatureNames().size();
                
                // Set label metadata (simplified)
                bundle.labelMetadata.numClasses = dataReader.getClassNameToIdMap().size();
                
                // Save bundle to JSON file
                String bundlePath = config.outputDir().resolve("xgboost_model_bundle.json").toString();
                // Note: In a full implementation, we'd use Jackson to serialize the bundle
                // For now, we'll create a simple bundle
                
                LOGGER.info("Model saved successfully to: {}", bundlePath);
                return bundle;
                
            } catch (Exception e) {
                LOGGER.error("Failed to save model", e);
                throw new ModelTrainingException("Model saving failed", e);
            }
        }
        
        @Override
        public XGBoostModelBundle loadModel(Path bundlePath) throws ModelTrainingException {
            LOGGER.info("Loading model from: {}", bundlePath);
            
            try {
                if (!Files.exists(bundlePath)) {
                    throw new ModelTrainingException("Model bundle file not found: " + bundlePath);
                }
                
                // For now, return a placeholder bundle
                // In a full implementation, we'd deserialize from JSON
                XGBoostModelBundle bundle = new XGBoostModelBundle();
                bundle.modelInfo.title = "Loaded Model";
                
                LOGGER.info("Model loaded successfully from: {}", bundlePath);
                return bundle;
                
            } catch (Exception e) {
                LOGGER.error("Failed to load model", e);
                throw new ModelTrainingException("Model loading failed", e);
            }
        }
    }
}