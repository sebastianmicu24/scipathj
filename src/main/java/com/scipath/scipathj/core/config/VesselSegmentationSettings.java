package com.scipath.scipathj.core.config;

/**
 * Settings record for vessel segmentation parameters.
 * Immutable data carrier that manages default values and user-configured values.
 * Uses Java 16+ record syntax for conciseness and immutability.
 *
 * @param threshold The threshold value for vessel detection (0-255)
 * @param minRoiSize The minimum ROI size in pixels (must be non-negative)
 * @param maxRoiSize The maximum ROI size in pixels (must be non-negative)
 * @param gaussianBlurSigma The Gaussian blur sigma parameter (must be non-negative)
 * @param applyMorphologicalClosing Whether to apply morphological closing operations
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record VesselSegmentationSettings(
    int threshold,
    double minRoiSize,
    double maxRoiSize,
    double gaussianBlurSigma,
    boolean applyMorphologicalClosing) {

  // Default values
  public static final int DEFAULT_THRESHOLD = 220;
  public static final double DEFAULT_MIN_ROI_SIZE = 50.0;
  public static final double DEFAULT_MAX_ROI_SIZE = 10000.0;
  public static final double DEFAULT_GAUSSIAN_BLUR_SIGMA = 2.0;
  public static final boolean DEFAULT_APPLY_MORPHOLOGICAL_CLOSING = true;

  /**
   * Creates a new VesselSegmentationSettings with validation.
   *
   * @param threshold The threshold value for vessel detection (0-255)
   * @param minRoiSize The minimum ROI size in pixels (must be non-negative)
   * @param maxRoiSize The maximum ROI size in pixels (must be non-negative)
   * @param gaussianBlurSigma The Gaussian blur sigma parameter (must be non-negative)
   * @param applyMorphologicalClosing Whether to apply morphological closing operations
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public VesselSegmentationSettings {
    if (threshold < 0 || threshold > 255) {
      throw new IllegalArgumentException("Threshold must be between 0 and 255, got: " + threshold);
    }
    if (minRoiSize < 0) {
      throw new IllegalArgumentException(
          "Minimum ROI size must be non-negative, got: " + minRoiSize);
    }
    if (maxRoiSize < 0) {
      throw new IllegalArgumentException(
          "Maximum ROI size must be non-negative, got: " + maxRoiSize);
    }
    if (minRoiSize > maxRoiSize) {
      throw new IllegalArgumentException(
          "Minimum ROI size ("
              + minRoiSize
              + ") cannot be greater than maximum ROI size ("
              + maxRoiSize
              + ")");
    }
    if (gaussianBlurSigma < 0) {
      throw new IllegalArgumentException(
          "Gaussian blur sigma must be non-negative, got: " + gaussianBlurSigma);
    }
  }

  /**
   * Creates a new VesselSegmentationSettings instance with default values.
   *
   * @return A new instance with default settings
   */
  public static VesselSegmentationSettings createDefault() {
    return new VesselSegmentationSettings(
        DEFAULT_THRESHOLD,
        DEFAULT_MIN_ROI_SIZE,
        DEFAULT_MAX_ROI_SIZE,
        DEFAULT_GAUSSIAN_BLUR_SIGMA,
        DEFAULT_APPLY_MORPHOLOGICAL_CLOSING);
  }

  /**
   * Creates a new instance with updated threshold value.
   *
   * @param newThreshold The new threshold value (0-255)
   * @return A new instance with the updated threshold
   * @throws IllegalArgumentException if threshold is invalid
   */
  public VesselSegmentationSettings withThreshold(int newThreshold) {
    return new VesselSegmentationSettings(
        newThreshold, minRoiSize, maxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
  }

  /**
   * Creates a new instance with updated minimum ROI size.
   *
   * @param newMinRoiSize The new minimum ROI size (must be non-negative)
   * @return A new instance with the updated minimum ROI size
   * @throws IllegalArgumentException if minRoiSize is invalid
   */
  public VesselSegmentationSettings withMinRoiSize(double newMinRoiSize) {
    return new VesselSegmentationSettings(
        threshold, newMinRoiSize, maxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
  }

  /**
   * Creates a new instance with updated maximum ROI size.
   *
   * @param newMaxRoiSize The new maximum ROI size (must be non-negative)
   * @return A new instance with the updated maximum ROI size
   * @throws IllegalArgumentException if maxRoiSize is invalid
   */
  public VesselSegmentationSettings withMaxRoiSize(double newMaxRoiSize) {
    return new VesselSegmentationSettings(
        threshold, minRoiSize, newMaxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
  }

  /**
   * Creates a new instance with updated Gaussian blur sigma.
   *
   * @param newGaussianBlurSigma The new Gaussian blur sigma (must be non-negative)
   * @return A new instance with the updated Gaussian blur sigma
   * @throws IllegalArgumentException if gaussianBlurSigma is invalid
   */
  public VesselSegmentationSettings withGaussianBlurSigma(double newGaussianBlurSigma) {
    return new VesselSegmentationSettings(
        threshold, minRoiSize, maxRoiSize, newGaussianBlurSigma, applyMorphologicalClosing);
  }

  /**
   * Creates a new instance with updated morphological closing setting.
   *
   * @param newApplyMorphologicalClosing Whether to apply morphological closing
   * @return A new instance with the updated morphological closing setting
   */
  public VesselSegmentationSettings withApplyMorphologicalClosing(
      boolean newApplyMorphologicalClosing) {
    return new VesselSegmentationSettings(
        threshold, minRoiSize, maxRoiSize, gaussianBlurSigma, newApplyMorphologicalClosing);
  }

  /**
   * Check if current settings are different from defaults.
   *
   * @return true if any setting differs from its default value
   */
  public boolean hasCustomValues() {
    return threshold != DEFAULT_THRESHOLD
        || minRoiSize != DEFAULT_MIN_ROI_SIZE
        || maxRoiSize != DEFAULT_MAX_ROI_SIZE
        || gaussianBlurSigma != DEFAULT_GAUSSIAN_BLUR_SIGMA
        || applyMorphologicalClosing != DEFAULT_APPLY_MORPHOLOGICAL_CLOSING;
  }

  /**
   * Validate that all current settings are within acceptable ranges.
   * This method is called automatically by the constructor, but can be used
   * for additional validation if needed.
   *
   * @throws IllegalStateException if any setting is invalid
   */
  public void validate() {
    if (threshold < 0 || threshold > 255) {
      throw new IllegalStateException("Invalid threshold value: " + threshold);
    }
    if (minRoiSize < 0) {
      throw new IllegalStateException("Invalid minimum ROI size: " + minRoiSize);
    }
    if (maxRoiSize < 0) {
      throw new IllegalStateException("Invalid maximum ROI size: " + maxRoiSize);
    }
    if (minRoiSize > maxRoiSize) {
      throw new IllegalStateException("Minimum ROI size cannot be greater than maximum ROI size");
    }
    if (gaussianBlurSigma < 0) {
      throw new IllegalStateException("Invalid Gaussian blur sigma: " + gaussianBlurSigma);
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
        "VesselSegmentationSettings{threshold=%d, minRoiSize=%.1f, maxRoiSize=%.1f, "
            + "gaussianBlurSigma=%.1f, applyMorphologicalClosing=%b}",
        threshold, minRoiSize, maxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
  }
}
