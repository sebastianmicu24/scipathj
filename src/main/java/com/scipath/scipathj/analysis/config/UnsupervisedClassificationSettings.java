package com.scipath.scipathj.analysis.config;

import java.util.List;

/**
 * Settings record for unsupervised cell classification (clustering).
 *
 * @param k Number of clusters
 * @param selectedFeatures List of feature names to use for clustering
 * @param enabled Whether unsupervised classification is enabled
 * @param algorithm The clustering algorithm to use ("K-Means" or "DBSCAN")
 * @param maxIterations Maximum number of iterations for K-Means
 * @param epsilon Epsilon parameter for DBSCAN
 * @param minPoints Minimum points parameter for DBSCAN
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record UnsupervisedClassificationSettings(
    int k,
    List<String> selectedFeatures,
    boolean enabled,
    String algorithm,
    int maxIterations,
    double epsilon,
    int minPoints) {

  /**
   * Creates a new UnsupervisedClassificationSettings with validation.
   *
   * @param k Number of clusters (must be >= 2)
   * @param selectedFeatures List of feature names (cannot be null)
   * @param enabled Whether unsupervised classification is enabled
   * @param algorithm The clustering algorithm to use ("K-Means" or "DBSCAN")
   * @param maxIterations Maximum number of iterations for K-Means
   * @param epsilon Epsilon parameter for DBSCAN
   * @param minPoints Minimum points parameter for DBSCAN
   * @throws IllegalArgumentException if k < 2 or selectedFeatures is null
   */
  public UnsupervisedClassificationSettings {
    if (k < 2) {
      throw new IllegalArgumentException("Number of clusters (k) must be at least 2");
    }
    if (selectedFeatures == null) {
      throw new IllegalArgumentException("Selected features list cannot be null");
    }
    if (algorithm == null || algorithm.isEmpty()) {
        // Default to K-Means if not specified
        algorithm = "K-Means";
    }
  }

  /**
   * Legacy constructor for backward compatibility.
   */
  public UnsupervisedClassificationSettings(int k, List<String> selectedFeatures, boolean enabled) {
      this(k, selectedFeatures, enabled, "K-Means", 100, 0.5, 5);
  }

  /**
   * Creates a default settings instance.
   *
   * @return Default settings (k=3, all features, disabled, K-Means, 100 iterations)
   */
  public static UnsupervisedClassificationSettings createDefault() {
    return new UnsupervisedClassificationSettings(
        3,
        List.of(), // Empty list means "use all available features" by default logic
        false,
        "K-Means",
        100,
        0.5,
        5);
  }

  /**
   * Creates a new instance with updated k.
   */
  public UnsupervisedClassificationSettings withK(int newK) {
    return new UnsupervisedClassificationSettings(newK, selectedFeatures, enabled, algorithm, maxIterations, epsilon, minPoints);
  }

  /**
   * Creates a new instance with updated selected features.
   */
  public UnsupervisedClassificationSettings withSelectedFeatures(List<String> newSelectedFeatures) {
    return new UnsupervisedClassificationSettings(k, newSelectedFeatures, enabled, algorithm, maxIterations, epsilon, minPoints);
  }

  /**
   * Creates a new instance with updated enabled status.
   */
  public UnsupervisedClassificationSettings withEnabled(boolean newEnabled) {
    return new UnsupervisedClassificationSettings(k, selectedFeatures, newEnabled, algorithm, maxIterations, epsilon, minPoints);
  }
}