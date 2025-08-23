package com.scipath.scipathj.core.config;

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