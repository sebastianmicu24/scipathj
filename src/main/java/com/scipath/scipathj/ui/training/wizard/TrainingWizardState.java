package com.scipath.scipathj.ui.training.wizard;

import com.scipath.scipathj.training.TrainingSettings;

import java.io.File;
import java.util.*;

/**
 * Shared state management for the XGBoost training wizard.
 * Maintains all data and configuration across wizard steps.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 */
public class TrainingWizardState {

    // Step 1: Data Loading state
    private File jsonFile;
    private List<String> availableFeatures = new ArrayList<>();
    private Set<String> selectedFeatures = new HashSet<>();
    private Map<String, String> featureCategories = new HashMap<>();
    private Map<String, Integer> classDistribution = new HashMap<>();
    private int totalSamples = 0;

    // Step 2: Data Split state
    private double trainRatio = 0.70;
    private double evalRatio = 0.20;
    private double testRatio = 0.10;
    private Map<String, Integer> trainClassCounts = new HashMap<>();
    private Map<String, Integer> evalClassCounts = new HashMap<>();
    private Map<String, Integer> testClassCounts = new HashMap<>();

    // Step 3: Parameters state
    private TrainingSettings trainingSettings;
    private File outputDirectory;
    private String presetConfiguration = "Balanced";

    // Step 4: Training state
    private boolean trainingInProgress = false;
    private boolean trainingCompleted = false;
    private List<Double> trainingF1Scores = new ArrayList<>();
    private List<Double> evaluationF1Scores = new ArrayList<>();
    private int currentEpoch = 0;
    private int totalEpochs = 100;
    private long trainingStartTime = 0;

    // Step 5: Evaluation state
    private double finalTrainingF1 = 0.0;
    private double finalEvaluationF1 = 0.0;
    private double bestEvaluationF1 = 0.0;
    private int bestEpoch = 0;
    private Map<String, Double> perClassMetrics = new HashMap<>();
    private double[][] confusionMatrix;
    private Map<String, Double> featureImportance = new HashMap<>();

    // Step 6: Final Testing state
    private double finalTestAccuracy = 0.0;
    private double finalTestF1 = 0.0;
    private boolean modelSaved = false;
    private File savedModelFile;

    // UI state
    private Map<String, Object> stepStates = new HashMap<>();

    /**
     * Default constructor initializes with sensible defaults.
     */
    public TrainingWizardState() {
        this.trainingSettings = new TrainingSettings();
        initializeDefaults();
    }

    /**
     * Initialize default values and feature categories.
     */
    private void initializeDefaults() {
        // Initialize feature categories for grouping
        initializeFeatureCategories();
    }

    /**
     * Initialize feature category mappings for UI grouping.
     */
    private void initializeFeatureCategories() {
        // Spatial features
        featureCategories.put("vessel_distance", "Spatial");
        featureCategories.put("neighbor_count", "Spatial");
        featureCategories.put("closest_neighbor_distance", "Spatial");

        // Morphological features
        featureCategories.put("area", "Morphological");
        featureCategories.put("width", "Morphological");
        featureCategories.put("height", "Morphological");
        featureCategories.put("perim", "Morphological");
        featureCategories.put("major", "Morphological");
        featureCategories.put("minor", "Morphological");
        featureCategories.put("angle", "Morphological");
        featureCategories.put("circ", "Morphological");
        featureCategories.put("feret", "Morphological");
        featureCategories.put("ar", "Morphological");
        featureCategories.put("round", "Morphological");
        featureCategories.put("solidity", "Morphological");

        // Statistical features
        featureCategories.put("intden", "Statistical");
        featureCategories.put("mean", "Statistical");
        featureCategories.put("stddev", "Statistical");
        featureCategories.put("mode", "Statistical");
        featureCategories.put("min", "Statistical");
        featureCategories.put("max", "Statistical");
        featureCategories.put("median", "Statistical");
        featureCategories.put("skew", "Statistical");
        featureCategories.put("kurt", "Statistical");

        // H&E Hematoxylin features
        featureCategories.put("hema_mean", "H&E Hematoxylin");
        featureCategories.put("hema_stddev", "H&E Hematoxylin");
        featureCategories.put("hema_mode", "H&E Hematoxylin");
        featureCategories.put("hema_min", "H&E Hematoxylin");
        featureCategories.put("hema_max", "H&E Hematoxylin");
        featureCategories.put("hema_median", "H&E Hematoxylin");
        featureCategories.put("hema_skew", "H&E Hematoxylin");
        featureCategories.put("hema_kurt", "H&E Hematoxylin");

        // H&E Eosin features
        featureCategories.put("eosin_mean", "H&E Eosin");
        featureCategories.put("eosin_stddev", "H&E Eosin");
        featureCategories.put("eosin_mode", "H&E Eosin");
        featureCategories.put("eosin_min", "H&E Eosin");
        featureCategories.put("eosin_max", "H&E Eosin");
        featureCategories.put("eosin_median", "H&E Eosin");
        featureCategories.put("eosin_skew", "H&E Eosin");
        featureCategories.put("eosin_kurt", "H&E Eosin");
    }

    /**
     * Get features grouped by category for UI display.
     * Now supports the new cellData format with prefixes (cell_, nucleus_, cytoplasm_).
     */
    public Map<String, List<String>> getFeaturesByCategory() {
        Map<String, List<String>> grouped = new HashMap<>();
        
        for (String feature : availableFeatures) {
            String category = categorizeFeature(feature);
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(feature);
        }
        
        // Sort features within each category
        grouped.values().forEach(Collections::sort);
        
        return grouped;
    }
    
    /**
     * Categorize a feature based on its name and prefix.
     * Supports both legacy format and new cellData format with prefixes.
     */
    private String categorizeFeature(String feature) {
        // Handle new cellData format with prefixes
        if (feature.startsWith("cell_")) {
            return "Cell Features";
        } else if (feature.startsWith("nucleus_")) {
            return "Nucleus Features";
        } else if (feature.startsWith("cytoplasm_")) {
            return "Cytoplasm Features";
        }
        
        // Legacy format - use existing mapping
        String legacyCategory = featureCategories.get(feature);
        if (legacyCategory != null) {
            return legacyCategory;
        }
        
        // If no specific mapping found, try to infer from feature name
        String lowerFeature = feature.toLowerCase();
        if (lowerFeature.contains("vessel") || lowerFeature.contains("neighbor")) {
            return "Spatial";
        } else if (lowerFeature.contains("area") || lowerFeature.contains("perim") ||
                   lowerFeature.contains("circ") || lowerFeature.contains("feret") ||
                   lowerFeature.contains("width") || lowerFeature.contains("height")) {
            return "Morphological";
        } else if (lowerFeature.contains("mean") || lowerFeature.contains("std") ||
                   lowerFeature.contains("min") || lowerFeature.contains("max") ||
                   lowerFeature.contains("median") || lowerFeature.contains("skew") ||
                   lowerFeature.contains("kurt")) {
            return "Statistical";
        } else if (lowerFeature.contains("hema")) {
            return "H&E Hematoxylin";
        } else if (lowerFeature.contains("eosin")) {
            return "H&E Eosin";
        }
        
        return "Other";
    }

    /**
     * Calculate split sample counts based on current ratios.
     */
    public void calculateSplitCounts() {
        int trainSamples = (int) Math.round(totalSamples * trainRatio);
        int evalSamples = (int) Math.round(totalSamples * evalRatio);
        int testSamples = totalSamples - trainSamples - evalSamples; // Ensure all samples are used

        // Calculate per-class distribution for each split
        trainClassCounts.clear();
        evalClassCounts.clear();
        testClassCounts.clear();

        for (Map.Entry<String, Integer> entry : classDistribution.entrySet()) {
            String className = entry.getKey();
            int classTotal = entry.getValue();
            
            int classTrain = (int) Math.round(classTotal * trainRatio);
            int classEval = (int) Math.round(classTotal * evalRatio);
            int classTest = classTotal - classTrain - classEval;

            trainClassCounts.put(className, classTrain);
            evalClassCounts.put(className, classEval);
            testClassCounts.put(className, classTest);
        }
    }

    /**
     * Validate current wizard state for step transitions.
     */
    public boolean isStepValid(int stepNumber) {
        switch (stepNumber) {
            case 1: // Data Loading
                return jsonFile != null && !selectedFeatures.isEmpty();
            case 2: // Data Split
                return Math.abs((trainRatio + evalRatio + testRatio) - 1.0) < 0.001;
            case 3: // Parameters
                return trainingSettings != null && outputDirectory != null;
            case 4: // Training
                return trainingCompleted;
            case 5: // Evaluation
                return finalEvaluationF1 > 0.0;
            case 6: // Final Testing
                return modelSaved;
            default:
                return false;
        }
    }

    /**
     * Get step completion status message.
     */
    public String getStepStatusMessage(int stepNumber) {
        if (isStepValid(stepNumber)) {
            return "✅ Step completed";
        }
        
        switch (stepNumber) {
            case 1:
                if (jsonFile == null) return "⚠️ Please select a training data file";
                if (selectedFeatures.isEmpty()) return "⚠️ Please select at least one feature";
                break;
            case 2:
                if (Math.abs((trainRatio + evalRatio + testRatio) - 1.0) >= 0.001) 
                    return "⚠️ Split ratios must sum to 100%";
                break;
            case 3:
                if (trainingSettings == null) return "⚠️ Invalid training settings";
                if (outputDirectory == null) return "⚠️ Please select output directory";
                break;
            case 4:
                if (!trainingCompleted) return "🔄 Training in progress...";
                break;
            case 5:
                if (finalEvaluationF1 <= 0.0) return "⚠️ No evaluation results available";
                break;
            case 6:
                if (!modelSaved) return "⚠️ Model not yet saved";
                break;
        }
        
        return "⏳ Step not completed";
    }

    // Getters and setters for all state properties

    // Step 1: Data Loading
    public File getJsonFile() { return jsonFile; }
    public void setJsonFile(File jsonFile) { this.jsonFile = jsonFile; }

    public List<String> getAvailableFeatures() { return availableFeatures; }
    public void setAvailableFeatures(List<String> availableFeatures) { 
        this.availableFeatures = availableFeatures; 
    }

    public Set<String> getSelectedFeatures() { return selectedFeatures; }
    public void setSelectedFeatures(Set<String> selectedFeatures) { 
        this.selectedFeatures = selectedFeatures; 
    }

    public Map<String, Integer> getClassDistribution() { return classDistribution; }
    public void setClassDistribution(Map<String, Integer> classDistribution) { 
        this.classDistribution = classDistribution; 
    }

    public int getTotalSamples() { return totalSamples; }
    public void setTotalSamples(int totalSamples) { this.totalSamples = totalSamples; }

    // Step 2: Data Split
    public double getTrainRatio() { return trainRatio; }
    public void setTrainRatio(double trainRatio) { 
        this.trainRatio = trainRatio; 
        calculateSplitCounts();
    }

    public double getEvalRatio() { return evalRatio; }
    public void setEvalRatio(double evalRatio) { 
        this.evalRatio = evalRatio; 
        calculateSplitCounts();
    }

    public double getTestRatio() { return testRatio; }
    public void setTestRatio(double testRatio) { 
        this.testRatio = testRatio; 
        calculateSplitCounts();
    }

    public Map<String, Integer> getTrainClassCounts() { return trainClassCounts; }
    public Map<String, Integer> getEvalClassCounts() { return evalClassCounts; }
    public Map<String, Integer> getTestClassCounts() { return testClassCounts; }

    // Step 3: Parameters
    public TrainingSettings getTrainingSettings() { return trainingSettings; }
    public void setTrainingSettings(TrainingSettings trainingSettings) { 
        this.trainingSettings = trainingSettings; 
    }

    public File getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(File outputDirectory) { 
        this.outputDirectory = outputDirectory; 
    }

    public String getPresetConfiguration() { return presetConfiguration; }
    public void setPresetConfiguration(String presetConfiguration) { 
        this.presetConfiguration = presetConfiguration; 
    }

    // Step 4: Training
    public boolean isTrainingInProgress() { return trainingInProgress; }
    public void setTrainingInProgress(boolean trainingInProgress) { 
        this.trainingInProgress = trainingInProgress; 
    }

    public boolean isTrainingCompleted() { return trainingCompleted; }
    public void setTrainingCompleted(boolean trainingCompleted) { 
        this.trainingCompleted = trainingCompleted; 
    }

    public List<Double> getTrainingF1Scores() { return trainingF1Scores; }
    public List<Double> getEvaluationF1Scores() { return evaluationF1Scores; }

    public int getCurrentEpoch() { return currentEpoch; }
    public void setCurrentEpoch(int currentEpoch) { this.currentEpoch = currentEpoch; }

    public int getTotalEpochs() { return totalEpochs; }
    public void setTotalEpochs(int totalEpochs) { this.totalEpochs = totalEpochs; }

    public long getTrainingStartTime() { return trainingStartTime; }
    public void setTrainingStartTime(long trainingStartTime) { 
        this.trainingStartTime = trainingStartTime; 
    }

    // Step 5: Evaluation
    public double getFinalTrainingF1() { return finalTrainingF1; }
    public void setFinalTrainingF1(double finalTrainingF1) { 
        this.finalTrainingF1 = finalTrainingF1; 
    }

    public double getFinalEvaluationF1() { return finalEvaluationF1; }
    public void setFinalEvaluationF1(double finalEvaluationF1) { 
        this.finalEvaluationF1 = finalEvaluationF1; 
    }

    public double getBestEvaluationF1() { return bestEvaluationF1; }
    public void setBestEvaluationF1(double bestEvaluationF1) { 
        this.bestEvaluationF1 = bestEvaluationF1; 
    }

    public int getBestEpoch() { return bestEpoch; }
    public void setBestEpoch(int bestEpoch) { this.bestEpoch = bestEpoch; }

    public Map<String, Double> getPerClassMetrics() { return perClassMetrics; }
    public void setPerClassMetrics(Map<String, Double> perClassMetrics) { 
        this.perClassMetrics = perClassMetrics; 
    }

    public double[][] getConfusionMatrix() { return confusionMatrix; }
    public void setConfusionMatrix(double[][] confusionMatrix) { 
        this.confusionMatrix = confusionMatrix; 
    }

    public Map<String, Double> getFeatureImportance() { return featureImportance; }
    public void setFeatureImportance(Map<String, Double> featureImportance) { 
        this.featureImportance = featureImportance; 
    }

    // Step 6: Final Testing
    public double getFinalTestAccuracy() { return finalTestAccuracy; }
    public void setFinalTestAccuracy(double finalTestAccuracy) { 
        this.finalTestAccuracy = finalTestAccuracy; 
    }

    public double getFinalTestF1() { return finalTestF1; }
    public void setFinalTestF1(double finalTestF1) { this.finalTestF1 = finalTestF1; }

    public boolean isModelSaved() { return modelSaved; }
    public void setModelSaved(boolean modelSaved) { this.modelSaved = modelSaved; }

    public File getSavedModelFile() { return savedModelFile; }
    public void setSavedModelFile(File savedModelFile) { this.savedModelFile = savedModelFile; }

    // Generic step state management
    public void saveStepState(String stepName, Object state) {
        stepStates.put(stepName, state);
    }

    public Object getStepState(String stepName) {
        return stepStates.get(stepName);
    }
}