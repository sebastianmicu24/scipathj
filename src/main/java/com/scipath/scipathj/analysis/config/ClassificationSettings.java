package com.scipath.scipathj.analysis.config;

/**
 * Settings record for cell classification parameters using XGBoost.
 * Immutable data carrier that manages default values and user-configured values for XGBoost model paths.
 * Uses Java 16+ record syntax for conciseness and immutability.
 *
 * @param modelPath Path to the XGBoost model file (.json format)
 * @param selectedFeaturesPath Path to the selected features file (.txt format)
 * @param labelMappingPath Path to the label mapping file (.properties format)
 * @param classDetailsPath Path to the class details file (.json format)
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record ClassificationSettings(
    String modelPath,
    String selectedFeaturesPath,
    String labelMappingPath,
    String classDetailsPath) {

  // Default model file paths (relative to resources directory)
  public static final String DEFAULT_MODEL_PATH = "/models/2D/xgboost_model.json";
  public static final String DEFAULT_SELECTED_FEATURES_PATH = "/models/2D/selected_features.txt";
  public static final String DEFAULT_LABEL_MAPPING_PATH = "/models/2D/xgboost_label_mapping.properties";
  public static final String DEFAULT_CLASS_DETAILS_PATH = "/models/2D/class_details.json";

  /**
   * Creates a new ClassificationSettings with validation.
   *
   * @param modelPath Path to the XGBoost model file (cannot be null or empty)
   * @param selectedFeaturesPath Path to the selected features file (cannot be null or empty)
   * @param labelMappingPath Path to the label mapping file (cannot be null or empty)
   * @param classDetailsPath Path to the class details file (cannot be null or empty)
   * @throws IllegalArgumentException if any path parameter is null or empty
   */
  public ClassificationSettings {
    if (modelPath == null || modelPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Model path cannot be null or empty");
    }
    if (selectedFeaturesPath == null || selectedFeaturesPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Selected features path cannot be null or empty");
    }
    if (labelMappingPath == null || labelMappingPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Label mapping path cannot be null or empty");
    }
    if (classDetailsPath == null || classDetailsPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Class details path cannot be null or empty");
    }
  }

  /**
   * Creates a new ClassificationSettings instance with default values.
   * Uses the built-in default model and supporting files.
   *
   * @return A new instance with default settings
   */
  public static ClassificationSettings createDefault() {
    return new ClassificationSettings(
        DEFAULT_MODEL_PATH,
        DEFAULT_SELECTED_FEATURES_PATH,
        DEFAULT_LABEL_MAPPING_PATH,
        DEFAULT_CLASS_DETAILS_PATH);
  }

  /**
   * Creates a new ClassificationSettings instance with custom model path.
   * Uses default paths for supporting files but allows custom XGBoost model.
   *
   * @param customModelPath Path to custom XGBoost model file
   * @return A new instance with custom model path
   */
  public static ClassificationSettings withCustomModel(String customModelPath) {
    return new ClassificationSettings(
        customModelPath,
        DEFAULT_SELECTED_FEATURES_PATH,
        DEFAULT_LABEL_MAPPING_PATH,
        DEFAULT_CLASS_DETAILS_PATH);
  }

  /**
   * Creates a new ClassificationSettings instance with custom model and all supporting files.
   * Allows complete customization of all classifier components.
   *
   * @param customModelPath Path to custom XGBoost model file
   * @param customFeaturesPath Path to custom selected features file
   * @param customMappingPath Path to custom label mapping file
   * @param customDetailsPath Path to custom class details file
   * @return A new instance with fully custom settings
   */
  public static ClassificationSettings withCustomPaths(
      String customModelPath,
      String customFeaturesPath,
      String customMappingPath,
      String customDetailsPath) {
    return new ClassificationSettings(
        customModelPath,
        customFeaturesPath,
        customMappingPath,
        customDetailsPath);
  }

  /**
   * Creates a new instance with updated model path.
   *
   * @param newModelPath The new model path (cannot be null or empty)
   * @return A new instance with the updated model path
   * @throws IllegalArgumentException if modelPath is invalid
   */
  public ClassificationSettings withModelPath(String newModelPath) {
    return new ClassificationSettings(
        newModelPath, selectedFeaturesPath, labelMappingPath, classDetailsPath);
  }

  /**
   * Creates a new instance with updated selected features path.
   *
   * @param newSelectedFeaturesPath The new selected features path (cannot be null or empty)
   * @return A new instance with the updated selected features path
   * @throws IllegalArgumentException if selectedFeaturesPath is invalid
   */
  public ClassificationSettings withSelectedFeaturesPath(String newSelectedFeaturesPath) {
    return new ClassificationSettings(
        modelPath, newSelectedFeaturesPath, labelMappingPath, classDetailsPath);
  }

  /**
   * Creates a new instance with updated label mapping path.
   *
   * @param newLabelMappingPath The new label mapping path (cannot be null or empty)
   * @return A new instance with the updated label mapping path
   * @throws IllegalArgumentException if labelMappingPath is invalid
   */
  public ClassificationSettings withLabelMappingPath(String newLabelMappingPath) {
    return new ClassificationSettings(
        modelPath, selectedFeaturesPath, newLabelMappingPath, classDetailsPath);
  }

  /**
   * Creates a new instance with updated class details path.
   *
   * @param newClassDetailsPath The new class details path (cannot be null or empty)
   * @return A new instance with the updated class details path
   * @throws IllegalArgumentException if classDetailsPath is invalid
   */
  public ClassificationSettings withClassDetailsPath(String newClassDetailsPath) {
    return new ClassificationSettings(
        modelPath, selectedFeaturesPath, labelMappingPath, newClassDetailsPath);
  }

  /**
   * Check if current settings are different from defaults.
   *
   * @return true if any path differs from its default value
   */
  public boolean hasCustomValues() {
    return !DEFAULT_MODEL_PATH.equals(modelPath)
        || !DEFAULT_SELECTED_FEATURES_PATH.equals(selectedFeaturesPath)
        || !DEFAULT_LABEL_MAPPING_PATH.equals(labelMappingPath)
        || !DEFAULT_CLASS_DETAILS_PATH.equals(classDetailsPath);
  }

  /**
   * Check if all paths use default values.
   *
   * @return true if all paths match their default values
   */
  public boolean isUsingDefaults() {
    return DEFAULT_MODEL_PATH.equals(modelPath)
        && DEFAULT_SELECTED_FEATURES_PATH.equals(selectedFeaturesPath)
        && DEFAULT_LABEL_MAPPING_PATH.equals(labelMappingPath)
        && DEFAULT_CLASS_DETAILS_PATH.equals(classDetailsPath);
  }

  /**
   * Validate that all current settings are valid.
   * This method is called automatically by the constructor, but can be used
   * for additional validation if needed.
   *
   * @throws IllegalStateException if any path is null or empty
   */
  public void validate() {
    if (modelPath == null || modelPath.trim().isEmpty()) {
      throw new IllegalStateException("Invalid model path: null or empty");
    }
    if (selectedFeaturesPath == null || selectedFeaturesPath.trim().isEmpty()) {
      throw new IllegalStateException("Invalid selected features path: null or empty");
    }
    if (labelMappingPath == null || labelMappingPath.trim().isEmpty()) {
      throw new IllegalStateException("Invalid label mapping path: null or empty");
    }
    if (classDetailsPath == null || classDetailsPath.trim().isEmpty()) {
      throw new IllegalStateException("Invalid class details path: null or empty");
    }
  }

  /**
   * Get a summary of the model paths configuration.
   *
   * @return String describing the current configuration
   */
  public String getConfigurationInfo() {
    StringBuilder info = new StringBuilder();
    info.append("XGBoost Classification Settings:\n");
    info.append("  Model Path: ").append(modelPath).append("\n");
    info.append("  Selected Features: ").append(selectedFeaturesPath).append("\n");
    info.append("  Label Mapping: ").append(labelMappingPath).append("\n");
    info.append("  Class Details: ").append(classDetailsPath).append("\n");
    info.append("  Using Defaults: ").append(isUsingDefaults());
    return info.toString();
  }

  /**
   * Get a string representation of current settings.
   *
   * @return String representation of settings
   */
  @Override
  public String toString() {
    return String.format(
        "ClassificationSettings{modelPath='%s', selectedFeaturesPath='%s', "
            + "labelMappingPath='%s', classDetailsPath='%s'}",
        modelPath, selectedFeaturesPath, labelMappingPath, classDetailsPath);
  }
}