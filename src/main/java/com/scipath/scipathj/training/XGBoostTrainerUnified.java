package com.scipath.scipathj.training;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Simplified XGBoost trainer that works with the new export format.
 * Focused on core functionality without complex debugging and legacy compatibility.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class XGBoostTrainerUnified implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(XGBoostTrainerUnified.class);

    private JSONDataReader dataReader;
    private TrainingSettings settings;
    private Booster model;
    private List<DMatrix> managedMatrices = new ArrayList<>();

    /**
     * Initialize trainer with data file and settings.
     */
    public XGBoostTrainerUnified(File dataFile, TrainingSettings settings) {
        this.settings = settings;
        
        // Load training data with simplified reader
        List<String> featureFilter = settings.getSelectedFeatures();
        this.dataReader = new JSONDataReader(dataFile, featureFilter);
        
        logger.info("Simplified XGBoost trainer initialized");
        logger.info("Features: {}", dataReader.getSelectedFeatureNames());
        logger.info("Classes: {}", dataReader.getClassNameToIdMap());
        logger.info("Total samples: {}", dataReader.getAllCellFeatures().size());
    }

    /**
     * Train the XGBoost model.
     */
    public Map<String, Object> trainModel() throws XGBoostError {
        logger.info("Starting XGBoost training with simplified architecture");
        
        // Validate data
        if (dataReader.getTrainingLabels().isEmpty()) {
            throw new RuntimeException("No training labels found in data");
        }
        if (dataReader.getAllCellFeatures().isEmpty()) {
            throw new RuntimeException("No features found in data");
        }

        // Create train/test split
        DMatrix[] splitData = createTrainTestSplit();
        if (splitData == null || splitData.length != 2) {
            throw new RuntimeException("Failed to create train/test split");
        }

        DMatrix trainMatrix = splitData[0];
        DMatrix testMatrix = splitData[1];
        
        logger.info("Training set: {} samples", trainMatrix.rowNum());
        logger.info("Test set: {} samples", testMatrix.rowNum());

        // Create XGBoost parameters
        Map<String, Object> params = createXGBoostParameters();
        
        // Train model
        logger.info("Training XGBoost model...");
        int numRounds = settings.getNumTrees();
        this.model = XGBoost.train(trainMatrix, params, numRounds, new HashMap<>(), null, null);
        
        // Evaluate model
        Map<String, Object> metrics = evaluateModel(testMatrix);
        
        // Save model
        saveModel();
        
        logger.info("Training completed successfully");
        return metrics;
    }

    private DMatrix[] createTrainTestSplit() throws XGBoostError {
        Map<String, float[]> features = dataReader.getFilteredCellFeatures();
        Map<String, Float> labels = dataReader.getTrainingLabels();
        
        List<String> allKeys = new ArrayList<>();
        for (String key : features.keySet()) {
            if (labels.containsKey(key)) {
                allKeys.add(key);
            }
        }
        
        if (allKeys.isEmpty()) {
            logger.error("No matching features and labels found");
            return null;
        }
        
        // Shuffle for random split
        Collections.shuffle(allKeys);
        
        int totalSamples = allKeys.size();
        int trainSize = (int) (totalSamples * settings.getTrainRatio());
        
        logger.info("Splitting {} samples: {} train, {} test", totalSamples, trainSize, totalSamples - trainSize);
        
        // Create train split
        List<String> trainKeys = allKeys.subList(0, trainSize);
        DMatrix trainMatrix = createDMatrix(trainKeys, features, labels);
        if (trainMatrix != null) {
            managedMatrices.add(trainMatrix);
        }
        
        // Create test split
        List<String> testKeys = allKeys.subList(trainSize, totalSamples);
        DMatrix testMatrix = createDMatrix(testKeys, features, labels);
        if (testMatrix != null) {
            managedMatrices.add(testMatrix);
        }
        
        return new DMatrix[]{trainMatrix, testMatrix};
    }

    private DMatrix createDMatrix(List<String> keys, Map<String, float[]> features, Map<String, Float> labels) throws XGBoostError {
        if (keys.isEmpty()) {
            logger.warn("No keys provided for DMatrix creation");
            return null;
        }
        
        int numSamples = keys.size();
        int numFeatures = dataReader.getSelectedFeatureNames().size();
        
        // Prepare feature data (row-major format)
        float[] featureData = new float[numSamples * numFeatures];
        float[] labelData = new float[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            String key = keys.get(i);
            float[] cellFeatures = features.get(key);
            Float label = labels.get(key);
            
            if (cellFeatures == null || label == null) {
                logger.warn("Missing data for key: {}", key);
                continue;
            }
            
            // Copy features
            System.arraycopy(cellFeatures, 0, featureData, i * numFeatures, numFeatures);
            labelData[i] = label;
        }
        
        // Create DMatrix
        DMatrix matrix = new DMatrix(featureData, numSamples, numFeatures);
        matrix.setLabel(labelData);
        
        logger.debug("Created DMatrix: {} samples x {} features", numSamples, numFeatures);
        return matrix;
    }

    private Map<String, Object> createXGBoostParameters() {
        Map<String, Object> params = new HashMap<>();
        
        // Basic parameters
        params.put("objective", "multi:softprob");
        params.put("eval_metric", "mlogloss");
        params.put("num_class", dataReader.getClassNameToIdMap().size());
        
        // Learning parameters
        params.put("learning_rate", settings.getLearningRate());
        params.put("max_depth", settings.getMaxDepth());
        params.put("subsample", settings.getSubsample());
        params.put("colsample_bytree", settings.getColsampleBytree());
        
        // Regularization
        params.put("reg_alpha", settings.getAlpha());
        params.put("reg_lambda", settings.getLambda());
        
        // Other settings
        params.put("nthread", Runtime.getRuntime().availableProcessors());
        params.put("verbosity", 1);
        
        logger.info("XGBoost parameters: {}", params);
        return params;
    }

    private Map<String, Object> evaluateModel(DMatrix testMatrix) throws XGBoostError {
        Map<String, Object> metrics = new HashMap<>();
        
        if (model == null || testMatrix == null) {
            logger.warn("Cannot evaluate - model or test data is null");
            return metrics;
        }
        
        // Get predictions
        float[][] predictions = model.predict(testMatrix);
        float[] trueLabels = testMatrix.getLabel();
        
        // Calculate accuracy
        int correct = 0;
        for (int i = 0; i < predictions.length; i++) {
            int predictedClass = getPredictedClass(predictions[i]);
            int trueClass = (int) trueLabels[i];
            if (predictedClass == trueClass) {
                correct++;
            }
        }
        
        double accuracy = (double) correct / predictions.length;
        metrics.put("accuracy", accuracy);
        metrics.put("total_samples", predictions.length);
        metrics.put("correct_predictions", correct);
        
        logger.info("Model evaluation - Accuracy: {:.4f} ({}/{} correct)", 
                   accuracy, correct, predictions.length);
        
        return metrics;
    }

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

    private void saveModel() {
        if (model == null) {
            logger.warn("Cannot save model - model is null");
            return;
        }
        
        try {
            // Use default output directory
            File outputDir = new File("models");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File modelFile = new File(outputDir, "xgboost_model_" + System.currentTimeMillis() + ".bin");
            model.saveModel(modelFile.getAbsolutePath());
            
            logger.info("Model saved to: {}", modelFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save model: {}", e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Clean up resources
        if (model != null) {
            try {
                model.dispose();
                logger.debug("Disposed XGBoost model");
            } catch (Exception e) {
                logger.warn("Error disposing model: {}", e.getMessage());
            }
        }
        
        // Clean up DMatrix objects
        for (DMatrix matrix : managedMatrices) {
            try {
                matrix.dispose();
            } catch (Exception e) {
                logger.warn("Error disposing DMatrix: {}", e.getMessage());
            }
        }
        managedMatrices.clear();
        
        logger.debug("SimplifiedXGBoostTrainer closed");
    }

    // Getters for access to training data
    public JSONDataReader getDataReader() {
        return dataReader;
    }
    
    public Booster getModel() {
        return model;
    }
}