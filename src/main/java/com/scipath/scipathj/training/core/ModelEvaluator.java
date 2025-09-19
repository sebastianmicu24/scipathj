package com.scipath.scipathj.training.core;

import com.scipath.scipathj.training.model.TrainingResult;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface for evaluating trained models.
 * Provides evaluation metrics and performance analysis.
 * 
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ModelEvaluator {
    
    /**
     * Evaluates a trained model on test data.
     * 
     * @param booster trained XGBoost model
     * @param testMatrix test data matrix
     * @return evaluation metrics
     * @throws ModelTrainingException if evaluation fails
     */
    TrainingResult.EvaluationMetrics evaluate(Booster booster, DMatrix testMatrix) 
        throws ModelTrainingException;
    
    /**
     * Default implementation of ModelEvaluator.
     */
    class DefaultModelEvaluator implements ModelEvaluator {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelEvaluator.class);
        
        @Override
        public TrainingResult.EvaluationMetrics evaluate(Booster booster, DMatrix testMatrix) 
                throws ModelTrainingException {
            
            LOGGER.info("Evaluating trained model");
            
            try {
                // Get predictions
                float[][] predictions = booster.predict(testMatrix);
                float[] trueLabels = testMatrix.getLabel();
                
                if (predictions.length == 0 || trueLabels.length == 0) {
                    throw new ModelTrainingException("No test data available for evaluation");
                }
                
                // Calculate accuracy
                int correct = 0;
                Map<String, Integer> classCorrect = new HashMap<>();
                Map<String, Integer> classTotal = new HashMap<>();
                
                for (int i = 0; i < predictions.length; i++) {
                    int predictedClass = getPredictedClass(predictions[i]);
                    int trueClass = (int) trueLabels[i];
                    
                    String trueClassName = "Class_" + trueClass;
                    classTotal.merge(trueClassName, 1, Integer::sum);
                    
                    if (predictedClass == trueClass) {
                        correct++;
                        classCorrect.merge(trueClassName, 1, Integer::sum);
                    }
                }
                
                double accuracy = (double) correct / trueLabels.length;
                
                // Calculate per-class metrics
                Map<String, Double> classMetrics = new HashMap<>();
                for (String className : classTotal.keySet()) {
                    int correctCount = classCorrect.getOrDefault(className, 0);
                    int totalCount = classTotal.get(className);
                    double classAccuracy = (double) correctCount / totalCount;
                    classMetrics.put(className + "_accuracy", classAccuracy);
                }
                
                // For now, use accuracy for all metrics (simplified)
                TrainingResult.EvaluationMetrics metrics = new TrainingResult.EvaluationMetrics(
                    accuracy,      // accuracy
                    accuracy,      // precision (simplified)
                    accuracy,      // recall (simplified)
                    accuracy,      // f1Score (simplified)
                    classMetrics   // classMetrics
                );
                
                LOGGER.info("Model evaluation completed: accuracy = {:.3f}", accuracy);
                return metrics;
                
            } catch (XGBoostError e) {
                LOGGER.error("XGBoost evaluation failed", e);
                throw new ModelTrainingException("Model evaluation failed", e);
            }
        }
        
        /**
         * Gets the predicted class from probability array.
         */
        private int getPredictedClass(float[] probabilities) {
            int maxIndex = 0;
            float maxProb = probabilities[0];
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i];
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    }
}