package com.scipath.scipathj.training.core;

import com.scipath.scipathj.training.data.TrainingDataReader;
import com.scipath.scipathj.training.model.TrainingConfiguration;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;

/**
 * Interface for data preprocessing operations in XGBoost training.
 * Handles data splitting, matrix creation, and model training.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface DataPreprocessor extends AutoCloseable {
    
    /**
     * Preprocessed data container.
     */
    record PreprocessedData(
        DMatrix trainMatrix,
        DMatrix testMatrix,
        int trainSize,
        int testSize
    ) implements AutoCloseable {
        
        @Override
        public void close() {
            try {
                if (trainMatrix != null) trainMatrix.dispose();
                if (testMatrix != null) testMatrix.dispose();
            } catch (Exception e) {
                // Log error but don't throw
            }
        }
        
        public DMatrix testMatrix() {
            return testMatrix;
        }
    }
    
    /**
     * Creates train/test split from training data.
     * 
     * @param dataReader training data reader
     * @param config training configuration
     * @return preprocessed data with train/test matrices
     * @throws ModelTrainingException if preprocessing fails
     */
    PreprocessedData createTrainTestSplit(TrainingDataReader dataReader, 
                                         TrainingConfiguration config) 
                                         throws ModelTrainingException;
    
    /**
     * Trains XGBoost model with preprocessed data.
     * 
     * @param data preprocessed training data
     * @param config training configuration
     * @return trained XGBoost booster
     * @throws ModelTrainingException if training fails
     */
    Booster trainXGBoostModel(PreprocessedData data, 
                             TrainingConfiguration config) 
                             throws ModelTrainingException;
    
    /**
     * Sets progress observer for monitoring training progress.
     * 
     * @param observer progress observer
     */
    void setProgressObserver(TrainingProgressObserver observer);
    
    @Override
    default void close() {
        // Default implementation - override if cleanup needed
    }
}