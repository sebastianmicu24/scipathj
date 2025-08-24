package com.scipath.scipathj.analysis.config;

/**
 * Settings record for feature extraction from segmented regions.
 * Immutable data carrier that manages all parameters related to feature extraction
 * from cell nuclei, cytoplasm, and vessel regions.
 * Uses Java 16+ record syntax for conciseness and immutability.
 * Based on SCHELI feature extraction system with region-specific feature control.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record FeatureExtractionSettings(
    // Feature maps for each region type (from SCHELI ConfigVariables)
    java.util.Map<String, Boolean> cellFeatures,
    java.util.Map<String, Boolean> nucleusFeatures,
    java.util.Map<String, Boolean> cytoplasmFeatures,
    java.util.Map<String, Boolean> vesselFeatures,

    // Performance settings
    boolean enablePerformanceOptimizations,
    int spatialGridSize,
    int batchSize,
    boolean sortROIs) {

  // Default values - all features enabled by default (matching SCHELI)
  public static final boolean DEFAULT_FEATURE_ENABLED = true;
  public static final boolean DEFAULT_STAIN_FEATURE_ENABLED = true; // H&E features enabled by default for H&E images
  public static final boolean DEFAULT_ENABLE_PERFORMANCE_OPTIMIZATIONS = true;
  public static final int DEFAULT_SPATIAL_GRID_SIZE = 100;
  public static final int DEFAULT_BATCH_SIZE = 100;
  public static final boolean DEFAULT_SORT_ROIS = true;

  /**
   * Creates a new FeatureExtractionSettings with validation.
   *
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public FeatureExtractionSettings {
    if (spatialGridSize < 1) {
      throw new IllegalArgumentException("Spatial grid size must be at least 1, got: " + spatialGridSize);
    }
    if (batchSize < 1) {
      throw new IllegalArgumentException("Batch size must be at least 1, got: " + batchSize);
    }
  }

  // Feature names matching SCHELI ConfigVariables
  private static final String[] CELL_FEATURES = {
      "vessel_distance", "neighbor_count", "closest_neighbor_distance",
      "area", "perim", "width", "height", "major", "minor", "angle", "circ", "intden",
      "feret", "feretx", "ferety", "feretangle", "minferet", "ar", "round", "solidity",
      "mean", "stddev", "mode", "min", "max", "median", "skew", "kurt",
      "hema_mean", "hema_stddev", "hema_mode", "hema_min", "hema_max", "hema_median", "hema_skew", "hema_kurt",
      "eosin_mean", "eosin_stddev", "eosin_mode", "eosin_min", "eosin_max", "eosin_median", "eosin_skew", "eosin_kurt"
  };

  /**
   * Creates a new FeatureExtractionSettings instance with default values.
   * All standard features enabled by default, H&E features disabled by default.
   *
   * @return A new instance with default settings
   */
  public static FeatureExtractionSettings createDefault() {
      java.util.Map<String, Boolean> cellFeatures = createDefaultFeatureMap();
      java.util.Map<String, Boolean> nucleusFeatures = createDefaultFeatureMap();
      java.util.Map<String, Boolean> cytoplasmFeatures = createDefaultFeatureMap();
      java.util.Map<String, Boolean> vesselFeatures = createVesselFeatureMap();

      return new FeatureExtractionSettings(
          cellFeatures, nucleusFeatures, cytoplasmFeatures, vesselFeatures,
          DEFAULT_ENABLE_PERFORMANCE_OPTIMIZATIONS, DEFAULT_SPATIAL_GRID_SIZE, DEFAULT_BATCH_SIZE, DEFAULT_SORT_ROIS);
  }

  /**
   * Creates a default feature map for vessels (morphological and intensity features only).
   */
  private static java.util.Map<String, Boolean> createVesselFeatureMap() {
      java.util.Map<String, Boolean> features = new java.util.HashMap<>();

      // Add morphological features
      String[] morphFeatures = {"area", "perim", "circ", "ar", "solidity", "feret", "feretx", "ferety", "feretangle", "minferet", "major", "minor", "round"};
      for (String feature : morphFeatures) {
          features.put(feature, true);
      }

      // Add intensity features
      String[] intensityFeatures = {"mean", "stddev", "min", "max", "median", "intden", "skew", "kurt"};
      for (String feature : intensityFeatures) {
          features.put(feature, true);
      }

      // Add spatial features (vessels need these for distance calculations)
      features.put("vessel_distance", true);
      features.put("neighbor_count", true);
      features.put("closest_neighbor_distance", true);

      return java.util.Collections.unmodifiableMap(features);
  }

  /**
   * Creates a default feature map with all features enabled by default.
   */
  private static java.util.Map<String, Boolean> createDefaultFeatureMap() {
      java.util.Map<String, Boolean> features = new java.util.HashMap<>();
      for (String feature : CELL_FEATURES) {
          // All features enabled by default, including H&E features
          features.put(feature, DEFAULT_FEATURE_ENABLED);
      }
      return java.util.Collections.unmodifiableMap(features);
  }



  // Note: Individual with* methods for each feature flag would be too numerous (33+ methods)
  // For now, we rely on creating new instances with the constructor
  // In a real implementation, you might want to add the most commonly used with* methods

  /**
   * Validates the current settings and returns whether they are valid.
   *
   * @return true if settings are valid, false otherwise
   */
  public boolean isValid() {
    try {
      validate();
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  /**
   * Validate that all current settings are within acceptable ranges.
   * This method is called automatically by the constructor, but can be used
   * for additional validation if needed.
   *
   * @throws IllegalStateException if any setting is invalid
   */
  public void validate() {
    if (spatialGridSize < 1) {
      throw new IllegalStateException("Invalid spatial grid size: " + spatialGridSize + " (must be >= 1)");
    }
    if (batchSize < 1) {
      throw new IllegalStateException("Invalid batch size: " + batchSize + " (must be >= 1)");
    }
  }

  /**
   * Get a string representation of current settings.
   *
   * @return String representation of settings
   */
  @Override
  public String toString() {
    return String.format(
        "FeatureExtractionSettings[features=%d enabled, optimizations=%s, gridSize=%d, batchSize=%d, sort=%s]",
        countEnabledFeatures(),
        enablePerformanceOptimizations,
        spatialGridSize,
        batchSize,
        sortROIs);
  }

  /**
   * Check if a feature is scale-dependent (i.e., represents a size measurement).
   *
   * @param featureName The name of the feature to check
   * @return true if the feature represents a size measurement that should be scale-aware
   */
  public static boolean isScaleDependentFeature(String featureName) {
    // Size-related features that should be converted to scaled units
    String[] scaleDependentFeatures = {
        "area", "perim", "width", "height", "major", "minor", "feret", "feretx", "ferety", "minferet",
        "mean", "stddev", "min", "max", "median", "intden", "skew", "kurt",
        "hema_mean", "hema_stddev", "hema_min", "hema_max", "hema_median", "hema_skew", "hema_kurt",
        "eosin_mean", "eosin_stddev", "eosin_min", "eosin_max", "eosin_median", "eosin_skew", "eosin_kurt"
    };

    for (String scaleFeature : scaleDependentFeatures) {
      if (scaleFeature.equals(featureName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a feature is area-dependent (i.e., represents an area measurement).
   *
   * @param featureName The name of the feature to check
   * @return true if the feature represents an area measurement
   */
  public static boolean isAreaDependentFeature(String featureName) {
    // Area-related features that should be converted to squared scaled units (μm², mm², etc.)
    String[] areaFeatures = {"area", "intden"};

    for (String areaFeature : areaFeatures) {
      if (areaFeature.equals(featureName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert a feature value to scaled units for display purposes only.
   * This should NOT be used for classification since the model was trained on pixel values.
   *
   * @param featureName The name of the feature
   * @param pixelValue The value in pixels
   * @param mainSettings The main settings containing scale information
   * @return The converted value in scaled units (for display only)
   */
  public static double convertFeatureToScaledUnitsForDisplay(String featureName, double pixelValue,
                                                  com.scipath.scipathj.infrastructure.config.MainSettings mainSettings) {
    String lowerFeatureName = featureName.toLowerCase();
    double scaleFactor = mainSettings.pixelsPerMicrometer();

    if (isNucleusAreaFeature(featureName) || isAreaFeature(featureName)) {
      // Area features: pixels² → μm² (divide by scale²)
      return pixelValue / (scaleFactor * scaleFactor);
    } else if (isLinearSizeFeature(featureName) || isPositionFeature(featureName)) {
      // Linear features: pixels → μm (divide by scale)
      return pixelValue / scaleFactor;
    } else {
      // Non-scale dependent features remain unchanged
      return pixelValue;
    }
  }

  /**
   * Check if feature is a nucleus area feature.
   */
  private static boolean isNucleusAreaFeature(String featureName) {
    String lower = featureName.toLowerCase();
    return lower.contains("nucleus") && (lower.contains("area") || lower.equals("area"));
  }

  /**
   * Check if feature is a vessel area feature.
   */
  private static boolean isVesselAreaFeature(String featureName) {
    String lower = featureName.toLowerCase();
    return lower.contains("vessel") && lower.contains("area");
  }

  /**
   * Check if feature is a linear size feature (perimeter, feret distances).
   */
  private static boolean isLinearSizeFeature(String featureName) {
    String lower = featureName.toLowerCase();
    return lower.contains("perim") || lower.contains("feret") ||
           lower.contains("major") || lower.contains("minor") ||
           lower.contains("width") || lower.contains("height") ||
           lower.contains("mean") || lower.contains("stddev") ||
           lower.contains("min") || lower.contains("max") ||
           lower.contains("median") || lower.contains("skew") ||
           lower.contains("kurt");
  }

  /**
   * Check if feature is a position feature.
   */
  private static boolean isPositionFeature(String featureName) {
    String lower = featureName.toLowerCase();
    return lower.equals("x") || lower.equals("y") ||
           lower.equals("xm") || lower.equals("ym") ||
           lower.contains("distance") || lower.contains("radius");
  }

  /**
   * Check if feature is an area feature.
   */
  private static boolean isAreaFeature(String featureName) {
    String lower = featureName.toLowerCase();
    return lower.equals("area") || lower.contains("area") ||
           lower.equals("intden") || lower.equals("solidity");
  }

  /**
   * Get the appropriate unit string for a feature based on its type.
   *
   * @param featureName The name of the feature
   * @param mainSettings The main settings containing scale information
   * @return The unit string (e.g., "μm", "μm²", "") for the feature
   */
  public static String getFeatureUnit(String featureName,
                                     com.scipath.scipathj.infrastructure.config.MainSettings mainSettings) {
    if (isAreaDependentFeature(featureName)) {
      return mainSettings.scaleUnit() + "²";
    } else if (isScaleDependentFeature(featureName)) {
      return mainSettings.scaleUnit();
    } else {
      return ""; // Dimensionless or count features
    }
  }

  /**
   * Count the total number of enabled features.
   */
  private int countEnabledFeatures() {
    int count = 0;

    // Count enabled features in all region maps
    for (Boolean enabled : cellFeatures.values()) {
      if (enabled) count++;
    }
    for (Boolean enabled : nucleusFeatures.values()) {
      if (enabled) count++;
    }
    for (Boolean enabled : cytoplasmFeatures.values()) {
      if (enabled) count++;
    }

    return count;
  }
}