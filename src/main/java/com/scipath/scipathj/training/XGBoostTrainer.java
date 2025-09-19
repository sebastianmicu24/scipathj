package com.scipath.scipathj.training;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * XGBoost model trainer and evaluator for SciPathJ.
 * Adapted from SCHELI TrainModel to work with JSON data.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class XGBoostTrainer {

    private static final Logger logger = LoggerFactory.getLogger(XGBoostTrainer.class);

    private final JSONDataReader dataReader;
    private final TrainingSettings settings;
    private final String outputDir;

    /**
     * Constructs a new XGBoost trainer.
     *
     * @param jsonFile JSON training data file
     * @param settings training configuration
     * @param outputDir output directory for model and results
     */
    public XGBoostTrainer(File jsonFile, TrainingSettings settings, String outputDir) {
        this.settings = settings;
        this.outputDir = outputDir;

        // Initialize data reader
        this.dataReader = new JSONDataReader(jsonFile, settings.getSelectedFeatures());
        logger.info("XGBoost trainer initialized with {} samples",
            dataReader.getAllCellFeatures().size());
    }

    /**
     * Executes the complete training workflow.
     */
    public void trainModel() throws XGBoostError, IOException {
        logger.info("--- Starting XGBoost Training Workflow ---");

        // Validate data
        if (dataReader.getTrainingLabels().isEmpty()) {
            throw new RuntimeException("No training labels found in data");
        }
        if (dataReader.getAllCellFeatures().isEmpty()) {
            throw new RuntimeException("No features found in data");
        }

        // Prepare data matrices
        DMatrix[] splitData = createTrainTestSplit();

        if (splitData == null || splitData.length != 2) {
            throw new RuntimeException("Failed to create train/test split");
        }

        DMatrix trainMatrix = splitData[0];
        DMatrix testMatrix = splitData[1];

        // Train and evaluate
        Map<String, Object> metrics = trainAndEvaluate(trainMatrix, testMatrix, trainMatrix.rowNum(), testMatrix.rowNum());

        logger.info("--- XGBoost Training Complete ---");
    }

    private DMatrix[] createTrainTestSplit() throws XGBoostError {
        logger.info("Creating train/test split with ratio: {}", settings.getTrainRatio());

        Map<String, float[]> features = dataReader.getFilteredCellFeatures();
        Map<String, Float> labels = dataReader.getTrainingLabels();

        List<String> keys = new ArrayList<>(features.keySet());
        List<String> trainKeys = new ArrayList<>();
        List<String> testKeys = new ArrayList<>();

        // Shuffle for randomness
        Collections.shuffle(keys);

        int totalSamples = keys.size();
        int trainSize = (int) (totalSamples * settings.getTrainRatio());

        logger.info("Total samples: {}, Train: {}, Test: {}", totalSamples, trainSize, totalSamples - trainSize);

        for (int i = 0; i < totalSamples; i++) {
            if (!labels.containsKey(keys.get(i))) {
                logger.warn("Missing label for sample: {}", keys.get(i));
                continue;
            }

            if (i < trainSize) {
                trainKeys.add(keys.get(i));
            } else {
                testKeys.add(keys.get(i));
            }
        }

        // Create matrices
        DMatrix trainMatrix = createDMatrix(trainKeys, features, labels);
        DMatrix testMatrix = createDMatrix(testKeys, features, labels);

        if (trainMatrix == null || testMatrix == null) {
            logger.error("Failed to create train/test matrices");
            return null;
        }

        return new DMatrix[]{trainMatrix, testMatrix};
    }

    private DMatrix createDMatrix(List<String> keys, Map<String, float[]> features,
                                  Map<String, Float> labels) throws XGBoostError {
        if (keys.isEmpty()) {
            return null;
        }

        List<String> sampleKeys = new ArrayList<>();
        List<float[]> sampleFeatures = new ArrayList<>();
        List<Float> sampleLabels = new ArrayList<>();

        for (String key : keys) {
            if (features.containsKey(key) && labels.containsKey(key)) {
                sampleKeys.add(key);
                sampleFeatures.add(features.get(key));
                sampleLabels.add(labels.get(key));
            }
        }

        int numFeatures = sampleFeatures.get(0).length;
        int numSamples = sampleFeatures.size();

        // Prepare 1D feature array (flattened)
        float[] featureData = new float[numSamples * numFeatures];
        for (int i = 0; i < numSamples; i++) {
            System.arraycopy(sampleFeatures.get(i), 0, featureData, i * numFeatures, numFeatures);
        }

        // Prepare label array
        float[] labelData = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            labelData[i] = sampleLabels.get(i);
        }

        // Create DMatrix using correct constructor
        DMatrix matrix = new DMatrix(featureData, numSamples, numFeatures, Float.NaN);
        matrix.setLabel(labelData);
        logger.debug("Created DMatrix with {} samples, {} features",
            numSamples, numFeatures);

        return matrix;
    }

    private Map<String, Object> trainAndEvaluate(DMatrix trainMatrix, DMatrix testMatrix, long trainRows, long testRows)
            throws XGBoostError, IOException {
        logger.info("Training XGBoost model with {} estimators", settings.getNumTrees());

        // Get parameters
        Map<String, Object> params = getTrainingParams();

        // Apply class balancing if enabled
        applyClassBalancing(trainMatrix);

        // Create watch list for monitoring
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMatrix);
        watches.put("test", testMatrix);

        // Train model
        Booster booster = XGBoost.train(trainMatrix, params, settings.getNumTrees(),
            watches, null, null);

        logger.info("Training complete");

        // Evaluate model
        Map<String, Double> metrics = evaluateModel(booster, testMatrix);

        // Run data analysis for debugging classification bias
        debugTrainingDataAnalysis();

        // Save model and feature importance
        saveModel(booster);

        // Analyze feature importance
        analyzeFeatureImportance(booster);

        return Map.of(
            "booster", booster,
            "metrics", metrics,
            "numClasses", dataReader.getClassNameToIdMap().size(),
            "numFeatures", dataReader.getSelectedFeatureNames().size(),
            "params", params
        );
    }

    private Map<String, Object> getTrainingParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("eta", settings.getLearningRate());
        params.put("max_depth", settings.getMaxDepth());
        params.put("min_child_weight", settings.getMinChildWeight());
        params.put("subsample", settings.getSubsample());
        params.put("colsample_bytree", settings.getColsampleBytree());
        params.put("lambda", settings.getLambda());
        params.put("alpha", settings.getAlpha());
        params.put("gamma", settings.getGamma());
        params.put("objective", "multi:softprob");
        params.put("num_class", dataReader.getClassNameToIdMap().size());
        params.put("eval_metric", "mlogloss");
        params.put("verbosity", 1);

        // Auto-adjust for medical data class imbalance
        Map<String, Float> trainingLabels = dataReader.getTrainingLabels();
        Map<Float, Integer> labelCounts = new HashMap<>();

        for (Float label : trainingLabels.values()) {
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }

        int totalSamples = trainingLabels.size();
        boolean hasImbalance = false;

        for (Map.Entry<Float, Integer> entry : labelCounts.entrySet()) {
            float percentage = (entry.getValue() * 100.0f) / totalSamples;
            if (percentage > 70) {
                hasImbalance = true;
                break;
            }
        }

        if (hasImbalance) {
            // Medical data optimizations: more conservative learning, higher regularization
            params.put("eta", Math.min(settings.getLearningRate(), 0.05f)); // Faster convergence
            params.put("max_depth", Math.min(settings.getMaxDepth(), 4)); // Shallower trees
            params.put("min_child_weight", Math.max(settings.getMinChildWeight(), 10)); // More conservative splits
            params.put("subsample", Math.min(settings.getSubsample(), 0.8f)); // More randomization

            logger.info("🏥 MEDICAL DATA OPTIMIZATIONS ACTIVATED:");
            logger.info(" • Conservative learning rate: {}", params.get("eta"));
            logger.info(" • Conservative tree depth: {}", params.get("max_depth"));
            logger.info(" • Higher min_child_weight: {}", params.get("min_child_weight"));
            logger.info(" • This prevents the model from overfitting to the majority class");
        }

        return params;
    }

    private void applyClassBalancing(DMatrix trainMatrix) throws XGBoostError {
        // Get training labels to analyze class distribution
        float[] trainLabels = trainMatrix.getLabel();
        Map<Float, Integer> classCounts = new HashMap<>();

        // Count samples per class
        for (float label : trainLabels) {
            classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
        }

        int totalSamples = trainLabels.length;
        int numClasses = classCounts.size();

        // Auto-detect severe imbalance or if balancing is explicitly enabled
        boolean shouldBalance = false;
        float maxClassPercentage = 0;

        for (Map.Entry<Float, Integer> entry : classCounts.entrySet()) {
            float percentage = (entry.getValue() * 100.0f) / totalSamples;
            if (percentage > maxClassPercentage) maxClassPercentage = percentage;
        }

        if (settings.isBalanceClasses()) {
            logger.info("Applying class balancing (enabled in settings)");
            shouldBalance = true;
        } else if (maxClassPercentage > 60) { // Auto-balance if one class dominates
            logger.warn("Auto-activating class balancing! Major class holds {:.1f}% of data", maxClassPercentage);
            logger.warn("This is common with medical data (e.g., most cells are hepatocytes)");
            shouldBalance = true;
        }

        if (!shouldBalance) {
            logger.info("Class balancing not needed - classes are reasonably balanced");
            return;
        }

        logger.info("CALCULATING CLASS BALANCE WEIGHTS:");
        logger.info(" • {} classes, {} total samples", numClasses, totalSamples);

        float[] sampleWeights = new float[totalSamples];
        float minWeight = Float.MAX_VALUE;
        float maxWeight = 0;

        for (Map.Entry<Float, Integer> entry : classCounts.entrySet()) {
            Float classId = entry.getKey();
            Integer count = entry.getValue();
            float percentage = (count * 100.0f) / totalSamples;

            // Use inverse class frequency weighting for medical data
            // Weight = total_samples / (class_count * num_classes)
            float weight = (float) totalSamples / (count * numClasses);

            // Boost rare classes even more for medical data where some cell types are naturally rare
            if (percentage < 20) {
                weight *= 2.0f; // Double weight for rare classes
                logger.info(" • 🔄 Class {}: {} samples ({:.1f}%) → weight={:.3f} (2x boosted)", classId, count, percentage, weight);
            } else {
                logger.info(" • Class {}: {} samples ({:.1f}%) → weight={:.3f}", classId, count, percentage, weight);
            }

            minWeight = Math.min(minWeight, weight);
            maxWeight = Math.max(maxWeight, weight);

            // Apply weights to all samples of this class
            for (int i = 0; i < totalSamples; i++) {
                if (trainLabels[i] == classId) {
                    sampleWeights[i] = weight;
                }
            }
        }

        logger.info("BALANCING SUMMARY:");
        logger.info(" • Weight range: {:.3f} - {:.3f}", minWeight, maxWeight);
        logger.info(" • Class imbalance ratio: {:.1f}x", maxWeight / minWeight);

        trainMatrix.setWeight(sampleWeights);
        logger.info("✅ Class balancing applied - minority classes will receive more attention");
        logger.info("✅ This should prevent XGBoost from always predicting the majority class");
    }

    private Map<String, Double> evaluateModel(Booster booster, DMatrix testMatrix)
            throws XGBoostError {
        logger.info("Evaluating model...");

        float[][] predictions = booster.predict(testMatrix);
        float[] trueLabels = testMatrix.getLabel();

        // Debug evaluation data
        logger.info("EVALUATION DEBUG:");
        logger.info(" • Test samples: {}", trueLabels.length);
        logger.info(" • Prediction array shape: {}x{}", predictions.length, predictions[0].length);

        // Count prediction distribution
        Map<Integer, Integer> predictionCounts = new HashMap<>();
        for (int i = 0; i < predictions.length; i++) {
            int predictedClass = getPredictedClass(predictions[i]);
            predictionCounts.put(predictedClass, predictionCounts.getOrDefault(predictedClass, 0) + 1);
        }

        logger.info(" • Prediction distribution:");
        for (Map.Entry<Integer, Integer> entry : predictionCounts.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / predictions.length;
            logger.info("   • Predicts class {}: {} samples ({:.1f}%)", entry.getKey(), entry.getValue(), percentage);
        }

        // Check for prediction bias
        for (Map.Entry<Integer, Integer> entry : predictionCounts.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / predictions.length;
            if (percentage > 80) {
                logger.warn("⚠️ PREDICTION BIAS: Model predicts class {} in {:.1f}% of cases!", entry.getKey(), percentage);
            }
        }

        // Show sample predictions
        logger.info("SAMPLE PREDICTIONS (first 10):");
        for (int i = 0; i < Math.min(10, predictions.length); i++) {
            int predictedClass = getPredictedClass(predictions[i]);
            float confidence = predictions[i][predictedClass];
            logger.info(" • Sample {}: True={}, Predicted={}, Confidence={:.3f}",
                i, (int)trueLabels[i], predictedClass, confidence);
        }

        // Calculate metrics
        Map<String, Double> metrics = new HashMap<>();

        // Accuracy
        int correct = 0;
        Map<Integer, Integer> confMatrix = new HashMap<>();
        for (int i = 0; i < trueLabels.length; i++) {
            int predictedClass = getPredictedClass(predictions[i]);
            int trueClass = (int) trueLabels[i];

            // Build simple confusion matrix
            String key = trueClass + "_" + predictedClass;
            confMatrix.put(key.hashCode(), confMatrix.getOrDefault(key.hashCode(), 0) + 1);

            if (predictedClass == trueClass) {
                correct++;
            }
        }
        double accuracy = (double) correct / trueLabels.length;
        metrics.put("accuracy", accuracy);

        logger.info("Model evaluation - Accuracy: {:.4f}", accuracy);
        logger.info("CONFUSION MATRIX DEBUG:");
        for (Map.Entry<Integer, Integer> entry : confMatrix.entrySet()) {
            logger.info(" • Confusion entry: {} samples", entry.getValue());
        }

        // Additional metrics can be added here
        metrics.put("f1", accuracy); // Simplified for now
        metrics.put("precision", accuracy);
        metrics.put("recall", accuracy);

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

    /**
     * Debug method to analyze training data distribution and potential issues
     */
    public void debugTrainingDataAnalysis() {
        logger.info("=== TRAINING DATA ANALYSIS FOR CLASSIFICATION DEBUGGING ===");

        Map<String, Float> trainingLabels = dataReader.getTrainingLabels();
        Map<String, Integer> classNameToIdMap = dataReader.getClassNameToIdMap();
        Map<String, float[]> features = dataReader.getFilteredCellFeatures();

        logger.info("DATA OVERVIEW:");
        logger.info(" • Total samples: {}", trainingLabels.size());
        logger.info(" • Total features: {}", dataReader.getSelectedFeatureNames().size());
        logger.info(" • Feature names: {}", dataReader.getSelectedFeatureNames());
        logger.info(" • Configured classes: {} ({})", classNameToIdMap.size(), classNameToIdMap.keySet());
        logger.info(" • Number of classes in data: {}", trainingLabels.values().stream().distinct().count());

        // Count samples per class
        Map<Float, Integer> labelCounts = new HashMap<>();
        for (Float label : trainingLabels.values()) {
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }

        logger.info("CLASS DISTRIBUTION:");
        int totalSamples = trainingLabels.size();
        for (Map.Entry<Float, Integer> entry : labelCounts.entrySet()) {
            float percentage = (entry.getValue() * 100.0f) / totalSamples;
            String className = classNameToIdMap.entrySet().stream()
                .filter(e -> e.getValue().equals(entry.getKey().intValue()))
                .map(Map.Entry::getKey)
                .findFirst().orElse("Unknown");
            logger.info(" • Class {} ({}) = {} samples ({:.2f}%)",
                entry.getKey(), className, entry.getValue(), percentage);
        }

        // Check for class imbalance and recommend auto-balancing
        boolean hasSevereImbalance = false;
        for (Map.Entry<Float, Integer> entry : labelCounts.entrySet()) {
            float percentage = (entry.getValue() * 100.0f) / totalSamples;
            if (percentage > 75) {
                logger.error("🚨 AUTO-BALANCING ACTIVATED: Class {} dominates {:.1f}% of data!", entry.getKey(), percentage);
                hasSevereImbalance = true;
                // Force balancing even if disabled in settings
                logger.warn("💡 Force-activating class balancing to handle severe imbalance");
            } else if (percentage < 10) {
                logger.warn("⚠️ Rare class detected: {} has only {:.1f}% of data - will benefit from balancing", entry.getKey(), percentage);
            }
        }

        if (hasSevereImbalance) {
            logger.warn("🔄 RECOMMENDATION: Update settings.yaml to set 'balance_classes: true'");
            logger.warn("🔄 This will automatically weight minority classes more heavily in training");
        }

        // Check for missing classes
        Set<Integer> classesWithData = trainingLabels.values().stream()
            .map(Float::intValue).distinct().collect(java.util.stream.Collectors.toSet());
        for (Map.Entry<String, Integer> entry : classNameToIdMap.entrySet()) {
            if (!classesWithData.contains(entry.getValue())) {
                logger.warn("⚠️ PROBLEM: Class '{}' (id={}) has NO training samples!", entry.getKey(), entry.getValue());
            }
        }

        // XGBoost label mapping analysis
        List<Float> sortedLabels = new ArrayList<>(labelCounts.keySet());
        Collections.sort(sortedLabels);

        logger.info("XGBoost LABEL MAPPING:");
        logger.info(" • Training labels will be sorted: {}", sortedLabels);
        for (int i = 0; i < sortedLabels.size(); i++) {
            logger.info(" • Original {} → XGBoost internal index {}", sortedLabels.get(i), i);
        }

        // Feature statistics
        logger.info("FEATURE DISTRIBUTION (first 3 samples):");
        List<String> sampleKeys = new ArrayList<>(features.keySet()).subList(0, Math.min(3, features.size()));
        List<String> featureNames = dataReader.getSelectedFeatureNames();

        for (int i = 0; i < sampleKeys.size(); i++) {
            String key = sampleKeys.get(i);
            Float label = trainingLabels.get(key);
            float[] featureVec = features.get(key);

            logger.info(" • Sample {} (label={}):", key, label);
            for (int j = 0; j < Math.min(5, featureVec.length); j++) {
                logger.info("   • {}: {}", featureNames.get(j), featureVec[j]);
            }
        }

        logger.info("=== END TRAINING DATA ANALYSIS ===");
    }

    private void saveModel(Booster booster) throws XGBoostError, IOException {
        // Ensure output directory exists
        Files.createDirectories(Paths.get(outputDir));

        // Save only the complete JSON bundle (no separate model file)
        saveCompleteModelBundle(booster);
    }

    private void saveTrainingConfig() throws IOException {
        String configPath = Paths.get(outputDir, "training_config.txt").toString();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(configPath)))) {
            writer.println("SciPathJ XGBoost Training Configuration");
            writer.println("====================================");
            writer.println("Training Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("Number of Classes: " + dataReader.getClassNameToIdMap().size());
            writer.println("Number of Features: " + dataReader.getSelectedFeatureNames().size());
            writer.println();
            writer.println("Training Settings:");
            writer.println("- Learning Rate: " + settings.getLearningRate());
            writer.println("- Max Depth: " + settings.getMaxDepth());
            writer.println("- Number of Trees: " + settings.getNumTrees());
            writer.println("- Train Ratio: " + settings.getTrainRatio());
            writer.println("- Balance Classes: " + settings.isBalanceClasses());
            writer.println();
            writer.println("Selected Features (" + dataReader.getSelectedFeatureNames().size() + "):");
            for (String feature : dataReader.getSelectedFeatureNames()) {
                writer.println("- " + feature);
            }
        }
        logger.info("Training configuration saved to: {}", configPath);
    }

    /**
     * Save list of selected features used during training.
     */
    private void saveSelectedFeatures() throws IOException {
        String featuresPath = Paths.get(outputDir, "selected_features.txt").toString();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(featuresPath)))) {
            List<String> selectedFeatures = dataReader.getSelectedFeatureNames();
            for (String feature : selectedFeatures) {
                writer.println(feature);
            }
        }
        logger.info("Selected features saved to: {} ({} features)", featuresPath,
            dataReader.getSelectedFeatureNames().size());
    }

    /**
     * Save XGBoost label mapping (original class ID -> XGBoost index).
     */
    private Map<Float, Integer> saveLabelMapping() throws IOException {
        logger.info("Creating proper label mapping for XGBoost...");

        // Collect unique labels and their XGBoost indices
        Map<String, Integer> classNameToIdMap = dataReader.getClassNameToIdMap();
        Map<String, Float> trainingLabels = dataReader.getTrainingLabels();

        // Create mapping from original class IDs to XGBoost indices
        // XGBoost sorts unique labels and assigns sequential indices (0,1,2...)
        Set<Float> uniqueLabels = new HashSet<>(trainingLabels.values());
        List<Float> sortedLabels = new ArrayList<>(uniqueLabels);
        Collections.sort(sortedLabels);

        Map<Float, Integer> labelToIndex = new HashMap<>();
        for (int i = 0; i < sortedLabels.size(); i++) {
            labelToIndex.put(sortedLabels.get(i), i);
            logger.info("Mapping original class {} to XGBoost index {}", sortedLabels.get(i), i);
        }

        // Save to properties file for backward compatibility
        String mappingPath = Paths.get(outputDir, "xgboost_label_mapping.properties").toString();
        Properties properties = new Properties();

        for (Map.Entry<Float, Integer> entry : labelToIndex.entrySet()) {
            // Format: original_class_id = xgboost_index
            properties.setProperty(String.valueOf((int)entry.getKey().floatValue()), String.valueOf(entry.getValue()));
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(mappingPath)))) {
            properties.store(writer, "SciPathJ XGBoost Label Mapping");
        }
        logger.info("Label mapping saved to: {} ({} classes)", mappingPath, labelToIndex.size());

        return labelToIndex; // Return for JSON export
    }

    /**
     * Save class details including names, IDs, and colors.
     */
    private void saveClassDetails() throws IOException {
        String classDetailsPath = Paths.get(outputDir, "class_details.json").toString();

        // Generate colors for classes
        String[] defaultColors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57",
            "#FF9FF3", "#54A0FF", "#5F27CD", "#00D2D3", "#FF9F43",
            "#10AC84", "#EE5A24", "#0ABDE3", "#E17055", "#A29BFE"
        };

        StringBuilder jsonContent = new StringBuilder();
        jsonContent.append("{\n");
        jsonContent.append("  \"classes\": {\n");

        Map<String, Integer> classNameToIdMap = dataReader.getClassNameToIdMap();
        java.util.List<String> classNames = new ArrayList<>(classNameToIdMap.keySet());
        java.util.Collections.sort(classNames, (a, b) -> Integer.compare(
            classNameToIdMap.get(a), classNameToIdMap.get(b)));

        for (int i = 0; i < classNames.size(); i++) {
            String className = classNames.get(i);
            int classId = classNameToIdMap.get(className);
            String color = defaultColors[classId % defaultColors.length];

            jsonContent.append("    \"").append(className).append("\": {\n");
            jsonContent.append("      \"id\": ").append(classId).append(",\n");
            jsonContent.append("      \"color\": \"").append(color).append("\"\n");
            jsonContent.append("    }");

            if (i < classNames.size() - 1) {
                jsonContent.append(",");
            }
            jsonContent.append("\n");
        }

        jsonContent.append("  }\n");
        jsonContent.append("}\n");

        Files.write(Paths.get(classDetailsPath), jsonContent.toString().getBytes());
        logger.info("Class details saved to: {} ({} classes)", classDetailsPath, classNameToIdMap.size());
    }

    private void analyzeFeatureImportance(Booster booster) {
        try {
            Map<String, Double> importanceScores = booster.getScore("", "gain");
            if (importanceScores.isEmpty()) {
                logger.warn("No feature importance scores available");
                return;
            }

            List<String> selectedFeatureNames = dataReader.getSelectedFeatureNames();
            logger.info("Top feature importance:");

            // Sort by importance
            List<Map.Entry<String, Double>> sortedFeatures = new ArrayList<>(importanceScores.entrySet());
            sortedFeatures.sort(new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                    return Double.compare(e2.getValue(), e1.getValue());
                }
            });

            int topN = Math.min(10, sortedFeatures.size());
            for (int i = 0; i < topN; i++) {
                Map.Entry<String, Double> entry = sortedFeatures.get(i);
                String genericName = entry.getKey();
                double score = entry.getValue();

                try {
                    int featureIndex = Integer.parseInt(genericName.substring(1));
                    if (featureIndex >= 0 && featureIndex < selectedFeatureNames.size()) {
                        String originalName = selectedFeatureNames.get(featureIndex);
                        logger.info("  - {}: {}", originalName, String.format("%.6f", score));
                    }
                } catch (Exception e) {
                    logger.debug("Could not map feature {}: {}", genericName, e.getMessage());
                }
            }
        } catch (XGBoostError e) {
            logger.error("Error analyzing feature importance: {}", e.getMessage());
        }
    }

    /**
     * Save complete model bundle in single JSON file
     */
    private void saveCompleteModelBundle(Booster booster) throws IOException {
        try {
            XGBoostModelBundle bundle = new XGBoostModelBundle();
            ObjectMapper mapper = new ObjectMapper();

            // 1. Get XGBoost model as JSON string using temporary file approach
            String tempModelPath = Paths.get(outputDir, "temp_xgboost_model.json").toString();
            try {
                // Save model as JSON to temp file
                booster.saveModel(tempModelPath);
                // Read the model JSON content
                bundle.xgboostModel.modelJson = Files.readString(Paths.get(tempModelPath));
                logger.debug("Model JSON captured via temp file (length: {})", bundle.xgboostModel.modelJson.length());
            } catch (Exception e) {
                logger.error("Failed to save/read model JSON: {}", e.getMessage(), e);
                bundle.xgboostModel.modelJson = "{}"; // Empty JSON object as fallback
            } finally {
                // Clean up temp file regardless of success/failure
                try {
                    Files.deleteIfExists(Paths.get(tempModelPath));
                } catch (Exception cleanupError) {
                    logger.debug("Could not delete temp model file: {}", cleanupError.getMessage());
                }
            }

            // 2. Set model info with title and description (only using new structured fields)
            bundle.modelInfo.title = "Cell Classification Model";
            bundle.modelInfo.description = "XGBoost-based cell classification model trained on SciPathJ data";
            bundle.modelInfo.author = "SciPathJ Application";
            bundle.modelInfo.platform = "SciPathJ";

            // 3. Training configuration
            bundle.trainingConfig.hyperparameters = getTrainingParams();
            bundle.trainingConfig.hyperparameters.remove("verbosity");

            XGBoostModelBundle.TrainingConfig.DataSplit dataSplit = new XGBoostModelBundle.TrainingConfig.DataSplit();
            dataSplit.trainRatio = settings.getTrainRatio();
            dataSplit.balanceClasses = settings.isBalanceClasses();

            // Calculate class distribution
            Map<String, Float> trainingLabels = dataReader.getTrainingLabels();
            Map<String, Integer> classNameToId = dataReader.getClassNameToIdMap();
            Map<Integer, String> idToClassName = dataReader.getIdToClassNameMap();

            Map<Float, Double> classDistribution = new HashMap<>();
            Map<Float, Integer> classCounts = new HashMap<>();
            for (Float label : trainingLabels.values()) {
                classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
            }
            for (Map.Entry<Float, Integer> entry : classCounts.entrySet()) {
                classDistribution.put(entry.getKey(), (double)entry.getValue() / trainingLabels.size());
            }
            // Convert Float keys to String keys for JSON compatibility
            Map<String, Double> stringKeyDistribution = new HashMap<>();
            for (Map.Entry<Float, Double> entry : classDistribution.entrySet()) {
                stringKeyDistribution.put(entry.getKey().toString(), entry.getValue());
            }
            dataSplit.classDistribution = stringKeyDistribution;

            bundle.trainingConfig.dataSplit = dataSplit;

            // 4. Feature metadata
            bundle.featureMetadata.selectedFeatures = dataReader.getSelectedFeatureNames();
            bundle.featureMetadata.numSelectedFeatures = bundle.featureMetadata.selectedFeatures.size();

            // Feature importance (if available)
            try {
                Map<String, Double> importance = booster.getScore("", "gain");
                if (!importance.isEmpty()) {
                    // Get selected feature names in training order
                    List<String> featureNames = dataReader.getSelectedFeatureNames();
                    List<Map<String, Object>> importanceList = new ArrayList<>();

                    for (Map.Entry<String, Double> entry : importance.entrySet()) {
                        String genericName = entry.getKey();
                        try {
                            int featureIndex = Integer.parseInt(genericName.substring(1));
                            if (featureIndex >= 0 && featureIndex < featureNames.size()) {
                                Map<String, Object> impEntry = new HashMap<>();
                                impEntry.put("feature", featureNames.get(featureIndex));
                                impEntry.put("importance", entry.getValue());
                                importanceList.add(impEntry);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    bundle.featureMetadata.featureImportance = importanceList;
                }
            } catch (Exception e) {
                logger.warn("Could not extract feature importance: {}", e.getMessage());
            }

            // 5. Label metadata (create internal mapping)
            Map<String, Map<Integer, Integer>> labelMap = new HashMap<>();
            Map<Integer, Integer> xgbToOriginal = new HashMap<>();
            Map<Integer, Integer> originalToXgb = new HashMap<>();

            // Create label mapping internally (XGBoost sorts unique labels and assigns sequential indices)
            Set<Float> uniqueLabels = new HashSet<>(trainingLabels.values());
            List<Float> sortedLabels = new ArrayList<>(uniqueLabels);
            Collections.sort(sortedLabels);

            // Build mapping from original class IDs to XGBoost indices
            for (int i = 0; i < sortedLabels.size(); i++) {
                int originalId = (int)sortedLabels.get(i).floatValue();
                int xgbIndex = i;
                xgbToOriginal.put(xgbIndex, originalId);
                originalToXgb.put(originalId, xgbIndex);
                logger.debug("Mapping original class {} to XGBoost index {}", sortedLabels.get(i), i);
            }

            // Convert Integer keys to String keys for JSON compatibility
            Map<String, Integer> xgbToOriginalStringKeys = new HashMap<>();
            Map<String, Integer> originalToXgbStringKeys = new HashMap<>();
            
            for (Map.Entry<Integer, Integer> entry : xgbToOriginal.entrySet()) {
                xgbToOriginalStringKeys.put(entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<Integer, Integer> entry : originalToXgb.entrySet()) {
                originalToXgbStringKeys.put(entry.getKey().toString(), entry.getValue());
            }
            
            Map<String, Map<String, Integer>> stringLabelMap = new HashMap<>();
            stringLabelMap.put("xgboost_index_to_original", xgbToOriginalStringKeys);
            stringLabelMap.put("original_to_xgboost_index", originalToXgbStringKeys);
            bundle.labelMetadata.labelMapping = stringLabelMap;

            // Class details - use class colors from training data and filter out unwanted classes
            Map<Integer, XGBoostModelBundle.ClassDetail> classDetails = new HashMap<>();
            Map<String, String> trainingDataColors = dataReader.getClassColors();

            for (Map.Entry<Integer, String> entry : idToClassName.entrySet()) {
                String className = entry.getValue();

                // Filter out "Unclassified" and other unwanted classes
                if ("Unclassified".equals(className)) {
                    logger.debug("Filtering out unwanted class: {}", className);
                    continue;
                }

                XGBoostModelBundle.ClassDetail detail = new XGBoostModelBundle.ClassDetail();
                detail.name = className;
                detail.id = entry.getKey();

                // Use color from training data if available, otherwise use default
                String classColor = trainingDataColors.get(className);
                if (classColor != null && classColor.startsWith("#")) {
                    detail.color = classColor;
                    logger.debug("Using training data color '{}' for class '{}'", classColor, className);
                } else {
                    // Fallback to default colors if no color found in training data
                    String[] defaultColors = {
                        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57",
                        "#FF9FF3", "#54A0FF", "#5F27CD", "#00D2D3", "#FF9F43"
                    };
                    detail.color = defaultColors[Math.abs(entry.getKey()) % defaultColors.length];
                    logger.debug("Using default color '{}' for class '{}'", detail.color, className);
                }

                classDetails.put(entry.getKey(), detail);
            }
            bundle.labelMetadata.classDetails = classDetails;
            bundle.labelMetadata.numClasses = classDetails.size();

            // 6. Save the complete bundle as JSON
            String bundlePath = Paths.get(outputDir, "xgboost_model_bundle.json").toString();
            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundle);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(bundlePath)))) {
                writer.write(jsonOutput);
            }

            logger.info("Complete model bundle saved to: {}", bundlePath);
            logger.info("Model title: {}", bundle.modelInfo.title);
            logger.info("Model description: {}", bundle.modelInfo.description);

        } catch (Exception e) {
            logger.error("Failed to save complete model bundle: {}", e.getMessage(), e);
            throw new IOException("Failed to save model bundle", e);
        }
    }

    private void saveResults(Map<String, Object> results, long trainSize, long testSize) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Double> metrics = (Map<String, Double>) results.get("metrics");

        String resultsPath = Paths.get(outputDir, "evaluation_results.txt").toString();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(resultsPath)))) {
            writer.println("SciPathJ XGBoost Evaluation Results");
            writer.println("==================================");
            writer.println("Evaluation Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("Training Set Size: " + trainSize + " samples");
            writer.println("Test Set Size: " + testSize + " samples");
            writer.println();
            writer.printf("Accuracy: %.4f%n", metrics.get("accuracy"));
            writer.printf("F1 Score: %.4f%n", metrics.get("f1"));
            writer.printf("Precision: %.4f%n", metrics.get("precision"));
            writer.printf("Recall: %.4f%n", metrics.get("recall"));
        }

        logger.info("Evaluation results saved to: {}", resultsPath);
    }

    /**
     * Gets the data reader for external access.
     *
     * @return JSONDataReader instance
     */
    public JSONDataReader getDataReader() {
        return dataReader;
    }
}