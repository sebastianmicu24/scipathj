package com.scipath.scipathj.training;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom deserializer for Map<String, Object> that handles mixed numeric types
 */
class FlexibleMapDeserializer extends JsonDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode node = p.getCodec().readTree(p);

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            result.put(field.getKey(), convertJsonNodeToObject(field.getValue()));
        }

        return result;
    }

    private Object convertJsonNodeToObject(JsonNode node) {
        try {
            if (node.isFloat() || node.isDouble()) {
                return node.asDouble();
            } else if (node.isInt() || node.isIntegralNumber()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else if (node.isBoolean()) {
                return node.asBoolean();
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isNull()) {
                return null;
            } else {
                // For complex objects like arrays, return as-is
                return node;
            }
        } catch (Exception e) {
            // Log the error and return a default value
            System.err.println("Error converting JSON node: " + node.toString() + ", error: " + e.getMessage());
            return node.asText(); // fallback to string representation
        }
    }
}

/**
 * Complete XGBoost model bundle in a single JSON structure.
 * Contains all model components: model data, metadata, configuration, and evaluation results.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XGBoostModelBundle {

    /**
     * Basic model information
     */
    public static class ModelInfo {
        @JsonProperty("version")
        public String version = "1.0.0";

        @JsonProperty("created")
        public String created;

        @JsonProperty("platform")
        public String platform = "SciPathJ";

        @JsonProperty("description")
        public String description = "XGBoost Cell Classification Model";

        @JsonProperty("title")
        public String title = "Cell Classification Model";

        @JsonProperty("author")
        public String author = "SciPathJ Application";

        public ModelInfo() {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            this.created = now.format(formatter);
        }
    }

    /**
     * XGBoost model data container
     */
    public static class XGBoostModelData {
        @JsonProperty("model_json")
        public String modelJson;

        @JsonProperty("model_type")
        public String modelType = "xgboost";

        public XGBoostModelData() {}
    }

    /**
     * Training configuration
     */
    public static class TrainingConfig {
        @JsonProperty("hyperparameters")
        @JsonDeserialize(using = FlexibleMapDeserializer.class)
        public Map<String, Object> hyperparameters;

        @JsonProperty("data_split")
        public DataSplit dataSplit;

        public static class DataSplit {
            @JsonProperty("train_ratio")
            public float trainRatio;

            @JsonProperty("balance_classes")
            public boolean balanceClasses;

            @JsonProperty("train_samples")
            public int trainSamples;

            @JsonProperty("test_samples")
            public int testSamples;

            @JsonProperty("class_distribution")
            public Map<String, Double> classDistribution;
        }
    }

    /**
     * Evaluation results container
     */
    public static class EvaluationResults {
        @JsonProperty("overall_metrics")
        public Map<String, Double> overallMetrics;

        @JsonProperty("per_class_metrics")
        public Map<String, Map<String, Double>> perClassMetrics;

        @JsonProperty("confusion_matrix")
        public Map<String, Map<String, Integer>> confusionMatrix;
    }

    /**
     * Feature metadata
     */
    public static class FeatureMetadata {
        @JsonProperty("selected_features")
        public List<String> selectedFeatures;

        @JsonProperty("num_selected_features")
        public int numSelectedFeatures;

        @JsonProperty("feature_types")
        public Map<String, Integer> featureTypes;

        @JsonProperty("feature_importance")
        public List<Map<String, Object>> featureImportance;
    }

    /**
     * Label/Mapping metadata
     */
    public static class LabelMetadata {
        @JsonProperty("label_mapping")
        @JsonDeserialize(using = FlexibleMapDeserializer.class)
        public Map<String, Object> labelMapping;

        @JsonProperty("class_details")
        public Map<Integer, ClassDetail> classDetails;

        @JsonProperty("num_classes")
        public int numClasses;
    }

    /**
     * Class details structure
     */
    public static class ClassDetail {
        @JsonProperty("name")
        public String name;

        @JsonProperty("id")
        public int id;

        @JsonProperty("color")
        public String color;
    }

    // Main structure fields
    @JsonProperty("model_info")
    public ModelInfo modelInfo;

    @JsonProperty("xgboost_model")
    public XGBoostModelData xgboostModel;

    @JsonProperty("training_config")
    public TrainingConfig trainingConfig;

    @JsonProperty("evaluation_results")
    public EvaluationResults evaluationResults;

    @JsonProperty("feature_metadata")
    public FeatureMetadata featureMetadata;

    @JsonProperty("label_metadata")
    public LabelMetadata labelMetadata;

    // Root-level compatibility fields (for backwards compatibility)
    @JsonProperty("modelVersion")
    public String modelVersion;

    @JsonProperty("modelDescription")
    public String modelDescription;

    @JsonProperty("modelTitle")
    public String modelTitle;

    /**
     * Default constructor
     */
    public XGBoostModelBundle() {
        this.modelInfo = new ModelInfo();
        this.xgboostModel = new XGBoostModelData();
        this.trainingConfig = new TrainingConfig();
        this.evaluationResults = new EvaluationResults();
        this.featureMetadata = new FeatureMetadata();
        this.labelMetadata = new LabelMetadata();
    }

    /**
     * Constructor with basic metadata
     */
    public XGBoostModelBundle(String title, String description) {
        this();
        this.modelInfo.title = title != null ? title : "Cell Classification Model";
        this.modelInfo.description = description != null ? description : "XGBoost Cell Classification Model";
    }

    // Getters for backward compatibility
    public String getModelVersion() { return modelInfo.version; }
    public String getModelDescription() { return modelInfo.description; }
    public String getModelTitle() { return modelInfo.title; }
}