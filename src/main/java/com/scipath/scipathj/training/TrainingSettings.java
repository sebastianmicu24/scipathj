package com.scipath.scipathj.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Configuration holder for XGBoost training settings in SciPathJ.
 * Similar to SCHELI ConfigVariables but simplified for SciPathJ.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrainingSettings {

    private static final Logger logger = LoggerFactory.getLogger(TrainingSettings.class);

    // XGBoost parameters
    private float learningRate = 0.1f;
    private int maxDepth = 6;
    private int minChildWeight = 1;
    private int numTrees = 100;
    private float subsample = 1.0f;
    private float colsampleBytree = 1.0f;
    private float lambda = 1.0f;
    private float alpha = 0.0f;
    private float gamma = 0.0f;
    
    // Multi-class specific parameters
    private String objective = "multi:softprob";  // For multi-class classification with probabilities
    private String evalMetric = "mlogloss";       // Multi-class log loss for proper evaluation
    private int numClasses = 2;                   // Number of classes (auto-detected)

    // Training parameters
    private float trainRatio = 0.7f;
    private boolean balanceClasses = true;

    // Feature selection
    private List<String> selectedFeatures = new ArrayList<>();
    private Map<String, Boolean> featureEnabled = new HashMap<>();

    // Class management
    private Map<String, Boolean> classEnabled = new HashMap<>();

    /**
     * Default constructor with sensible defaults.
     */
    public TrainingSettings() {
        logger.debug("Created TrainingSettings with default values");
    }

    /**
     * Constructor with selected features.
     *
     * @param selectedFeatures initial selected features list
     */
    public TrainingSettings(List<String> selectedFeatures) {
        this();
        if (selectedFeatures != null) {
            this.selectedFeatures.addAll(selectedFeatures);
            for (String feature : selectedFeatures) {
                featureEnabled.put(feature, true);
            }
        }
    }

    // Getters and setters for XGBoost parameters

    public float getLearningRate() { return learningRate; }
    public void setLearningRate(float learningRate) { this.learningRate = learningRate; }

    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public int getMinChildWeight() { return minChildWeight; }
    public void setMinChildWeight(int minChildWeight) { this.minChildWeight = minChildWeight; }

    public int getNumTrees() { return numTrees; }
    public void setNumTrees(int numTrees) { this.numTrees = numTrees; }

    public float getSubsample() { return subsample; }
    public void setSubsample(float subsample) { this.subsample = subsample; }

    public float getColsampleBytree() { return colsampleBytree; }
    public void setColsampleBytree(float colsampleBytree) { this.colsampleBytree = colsampleBytree; }

    public float getLambda() { return lambda; }
    public void setLambda(float lambda) { this.lambda = lambda; }

    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { this.alpha = alpha; }

    public float getGamma() { return gamma; }
    public void setGamma(float gamma) { this.gamma = gamma; }
    
    // Multi-class parameters
    
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    
    public String getEvalMetric() { return evalMetric; }
    public void setEvalMetric(String evalMetric) { this.evalMetric = evalMetric; }
    
    public int getNumClasses() { return numClasses; }
    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
        // Auto-configure objective based on number of classes
        if (numClasses == 2) {
            this.objective = "binary:logistic";
            this.evalMetric = "logloss";
        } else if (numClasses > 2) {
            this.objective = "multi:softprob";
            this.evalMetric = "mlogloss";
        }
    }

    // Training parameters

    public float getTrainRatio() { return trainRatio; }
    public void setTrainRatio(float trainRatio) { this.trainRatio = trainRatio; }

    public boolean isBalanceClasses() { return balanceClasses; }
    public void setBalanceClasses(boolean balanceClasses) { this.balanceClasses = balanceClasses; }

    // Feature management

    public List<String> getSelectedFeatures() {
        return new ArrayList<>(selectedFeatures);
    }

    public void setSelectedFeatures(List<String> selectedFeatures) {
        this.selectedFeatures.clear();
        if (selectedFeatures != null) {
            this.selectedFeatures.addAll(selectedFeatures);
        }
    }

    public boolean isFeatureEnabled(String featureName) {
        return featureEnabled.getOrDefault(featureName, true);
    }

    public void setFeatureEnabled(String featureName, boolean enabled) {
        featureEnabled.put(featureName, enabled);

        if (enabled && !selectedFeatures.contains(featureName)) {
            selectedFeatures.add(featureName);
        } else if (!enabled) {
            selectedFeatures.remove(featureName);
        }
    }

    public void updateFeatureSelection(List<String> availableFeatures, Map<String, Boolean> selections) {
        this.featureEnabled.clear();
        this.selectedFeatures.clear();

        if (availableFeatures != null) {
            for (String feature : availableFeatures) {
                boolean enabled = selections.getOrDefault(feature, true);
                featureEnabled.put(feature, enabled);
                if (enabled) {
                    selectedFeatures.add(feature);
                }
            }
        }
    }

    // Class management

    public boolean isClassEnabled(String className) {
        return classEnabled.getOrDefault(className, true);
    }

    public void setClassEnabled(String className, boolean enabled) {
        classEnabled.put(className, enabled);
    }

    public Map<String, Boolean> getClassSelections() {
        return new HashMap<>(classEnabled);
    }

    public void updateClassSelections(Map<String, Boolean> classSelections) {
        this.classEnabled.clear();
        if (classSelections != null) {
            this.classEnabled.putAll(classSelections);
        }
    }

    /**
     * Validates current settings.
     *
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (learningRate <= 0 || learningRate > 1) {
            errors.add("Learning rate must be between 0 and 1 (exclusive)");
        }

        if (maxDepth < 1 || maxDepth > 20) {
            errors.add("Max depth must be between 1 and 20");
        }

        if (minChildWeight < 0 || minChildWeight > 100) {
            errors.add("Min child weight must be between 0 and 100");
        }

        if (numTrees < 1 || numTrees > 10000) {
            errors.add("Number of trees must be between 1 and 10000");
        }

        if (subsample <= 0 || subsample > 1) {
            errors.add("Subsample must be between 0 and 1 (exclusive)");
        }

        if (colsampleBytree <= 0 || colsampleBytree > 1) {
            errors.add("Colsample bytree must be between 0 and 1 (exclusive)");
        }

        if (lambda < 0 || lambda > 10) {
            errors.add("Lambda must be between 0 and 10");
        }

        if (alpha < 0 || alpha > 10) {
            errors.add("Alpha must be between 0 and 10");
        }

        if (gamma < 0 || gamma > 10) {
            errors.add("Gamma must be between 0 and 10");
        }

        if (trainRatio <= 0 || trainRatio >= 1) {
            errors.add("Train ratio must be between 0 and 1 (exclusive)");
        }

        return errors;
    }

    /**
     * Creates a copy of this TrainingSettings instance.
     *
     * @return a new TrainingSettings with the same values
     */
    public TrainingSettings copyFrom(TrainingSettings other) {
        this.learningRate = other.learningRate;
        this.maxDepth = other.maxDepth;
        this.minChildWeight = other.minChildWeight;
        this.numTrees = other.numTrees;
        this.subsample = other.subsample;
        this.colsampleBytree = other.colsampleBytree;
        this.lambda = other.lambda;
        this.alpha = other.alpha;
        this.gamma = other.gamma;
        this.trainRatio = other.trainRatio;
        this.balanceClasses = other.balanceClasses;
        this.selectedFeatures.clear();
        this.selectedFeatures.addAll(other.selectedFeatures);
        this.featureEnabled.clear();
        this.featureEnabled.putAll(other.featureEnabled);
        this.classEnabled.clear();
        this.classEnabled.putAll(other.classEnabled);
        return this;
    }

    /**
     * Resets all settings to defaults.
     */
    public void resetToDefaults() {
        logger.info("Resetting training settings to defaults");

        // Reset XGBoost parameters
        learningRate = 0.1f;
        maxDepth = 6;
        minChildWeight = 1;
        numTrees = 100;
        subsample = 1.0f;
        colsampleBytree = 1.0f;
        lambda = 1.0f;
        alpha = 0.0f;
        gamma = 0.0f;

        // Reset training parameters
        trainRatio = 0.7f;
        balanceClasses = true;

        // Keep feature selections as-is since they depend on data
    }

    @Override
    public String toString() {
        return String.format("TrainingSettings{learningRate=%.3f, maxDepth=%d, numTrees=%d, trainRatio=%.2f, features=%d}",
            learningRate, maxDepth, numTrees, trainRatio, selectedFeatures.size());
    }
}