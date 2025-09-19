package com.scipath.scipathj.training.model;

import com.scipath.scipathj.training.XGBoostModelBundle;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable record representing the result of model training.
 * Contains the trained model, evaluation metrics, and training statistics.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public record TrainingResult(
    XGBoostModelBundle modelBundle,
    EvaluationMetrics evaluation,
    TrainingConfiguration configuration,
    TrainingStatistics statistics,
    LocalDateTime completedAt
) {
    
    /**
     * Evaluation metrics for the trained model.
     */
    public record EvaluationMetrics(
        double accuracy,
        double precision,
        double recall,
        double f1Score,
        Map<String, Double> classMetrics
    ) {
        
        /**
         * Creates default metrics (placeholder).
         */
        public static EvaluationMetrics createDefault() {
            return new EvaluationMetrics(0.0, 0.0, 0.0, 0.0, Map.of());
        }
    }
    
    /**
     * Training statistics and metadata.
     */
    public record TrainingStatistics(
        int totalSamples,
        int trainingSamples,
        int testSamples,
        int numFeatures,
        int numClasses,
        Map<String, Long> classDistribution,
        long trainingTimeMs
    ) {
        
        /**
         * Creates default statistics (placeholder).
         */
        public static TrainingStatistics createDefault() {
            return new TrainingStatistics(0, 0, 0, 0, 0, Map.of(), 0L);
        }
    }
    
    /**
     * Creates a new training result.
     */
    public TrainingResult(XGBoostModelBundle modelBundle,
                         EvaluationMetrics evaluation,
                         TrainingConfiguration configuration,
                         TrainingStatistics statistics) {
        this(modelBundle, evaluation, configuration, statistics, LocalDateTime.now());
    }
    
    /**
     * Validates the result parameters.
     */
    public TrainingResult {
        if (modelBundle == null) {
            throw new IllegalArgumentException("Model bundle cannot be null");
        }
        if (evaluation == null) {
            throw new IllegalArgumentException("Evaluation metrics cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (statistics == null) {
            throw new IllegalArgumentException("Statistics cannot be null");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("Completion time cannot be null");
        }
    }
    
    /**
     * Returns a summary string of the training result.
     */
    public String getSummary() {
        return String.format("TrainingResult[accuracy=%.3f, samples=%d, features=%d, classes=%d]",
                           evaluation.accuracy(), 
                           statistics.totalSamples(),
                           statistics.numFeatures(), 
                           statistics.numClasses());
    }
    
    /**
     * Returns the model file path if available.
     */
    public String getModelPath() {
        return configuration.getOutputDirPath() + "/xgboost_model_bundle.json";
    }
    
    /**
     * Checks if the training was successful (accuracy > threshold).
     */
    public boolean isSuccessful(double accuracyThreshold) {
        return evaluation.accuracy() >= accuracyThreshold;
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}