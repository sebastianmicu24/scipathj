package com.scipath.scipathj.core.config;

/**
 * Settings record for nuclear segmentation using StarDist.
 * Immutable data carrier that manages all parameters related to nucleus detection and segmentation.
 * Uses Java 16+ record syntax for conciseness and immutability.
 *
 * @param modelChoice The StarDist model choice
 * @param normalizeInput Whether input normalization is enabled
 * @param percentileBottom The bottom percentile for normalization (0.0-100.0)
 * @param percentileTop The top percentile for normalization (0.0-100.0)
 * @param probThresh The probability threshold for nucleus detection (0.0-1.0)
 * @param nmsThresh The non-maximum suppression threshold (0.0-1.0)
 * @param outputType The output type for StarDist results
 * @param nTiles The number of tiles for processing large images
 * @param excludeBoundary The boundary exclusion distance in pixels
 * @param roiPosition The ROI position setting
 * @param verbose Whether verbose output is enabled
 * @param showCsbdeepProgress Whether CSBDeep progress display is enabled
 * @param showProbAndDist Whether probability and distance maps should be shown
 * @param minNucleusSize The minimum nucleus size for filtering in pixels
 * @param maxNucleusSize The maximum nucleus size for filtering in pixels
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record NuclearSegmentationSettings(
    String modelChoice,
    boolean normalizeInput,
    float percentileBottom,
    float percentileTop,
    float probThresh,
    float nmsThresh,
    String outputType,
    int nTiles,
    int excludeBoundary,
    String roiPosition,
    boolean verbose,
    boolean showCsbdeepProgress,
    boolean showProbAndDist,
    double minNucleusSize,
    double maxNucleusSize) {

  // Default values based on SCHELI implementation
  public static final String DEFAULT_MODEL_CHOICE = "Versatile (H&E nuclei)";
  public static final boolean DEFAULT_NORMALIZE_INPUT = true;
  public static final float DEFAULT_PERCENTILE_BOTTOM = 1.0f;
  public static final float DEFAULT_PERCENTILE_TOP = 99.8f;
  public static final float DEFAULT_PROB_THRESH = 0.5f;
  public static final float DEFAULT_NMS_THRESH = 0.4f;
  public static final String DEFAULT_OUTPUT_TYPE = "ROI Manager";
  public static final int DEFAULT_N_TILES = 1;
  public static final int DEFAULT_EXCLUDE_BOUNDARY = 2;
  public static final String DEFAULT_ROI_POSITION = "Automatic";
  public static final boolean DEFAULT_VERBOSE = false;
  public static final boolean DEFAULT_SHOW_CSBDEEP_PROGRESS = false;
  public static final boolean DEFAULT_SHOW_PROB_AND_DIST = false;
  public static final double DEFAULT_MIN_NUCLEUS_SIZE = 10.0;
  public static final double DEFAULT_MAX_NUCLEUS_SIZE = 1000.0;

  /**
   * Creates a new NuclearSegmentationSettings with validation.
   *
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public NuclearSegmentationSettings {
    if (modelChoice == null || modelChoice.trim().isEmpty()) {
      throw new IllegalArgumentException("Model choice cannot be null or empty");
    }
    if (percentileBottom < 0.0f || percentileBottom > 100.0f) {
      throw new IllegalArgumentException(
          "Bottom percentile must be between 0.0 and 100.0, got: " + percentileBottom);
    }
    if (percentileTop < 0.0f || percentileTop > 100.0f) {
      throw new IllegalArgumentException(
          "Top percentile must be between 0.0 and 100.0, got: " + percentileTop);
    }
    if (percentileBottom >= percentileTop) {
      throw new IllegalArgumentException(
          "Bottom percentile ("
              + percentileBottom
              + ") must be less than top percentile ("
              + percentileTop
              + ")");
    }
    if (probThresh < 0.0f || probThresh > 1.0f) {
      throw new IllegalArgumentException(
          "Probability threshold must be between 0.0 and 1.0, got: " + probThresh);
    }
    if (nmsThresh < 0.0f || nmsThresh > 1.0f) {
      throw new IllegalArgumentException(
          "NMS threshold must be between 0.0 and 1.0, got: " + nmsThresh);
    }
    if (outputType == null || outputType.trim().isEmpty()) {
      throw new IllegalArgumentException("Output type cannot be null or empty");
    }
    if (nTiles < 1) {
      throw new IllegalArgumentException("Number of tiles must be at least 1, got: " + nTiles);
    }
    if (excludeBoundary < 0) {
      throw new IllegalArgumentException(
          "Exclude boundary must be non-negative, got: " + excludeBoundary);
    }
    if (roiPosition == null || roiPosition.trim().isEmpty()) {
      throw new IllegalArgumentException("ROI position cannot be null or empty");
    }
    if (minNucleusSize < 0.0) {
      throw new IllegalArgumentException(
          "Minimum nucleus size must be non-negative, got: " + minNucleusSize);
    }
    if (maxNucleusSize < 0.0) {
      throw new IllegalArgumentException(
          "Maximum nucleus size must be non-negative, got: " + maxNucleusSize);
    }
    if (minNucleusSize >= maxNucleusSize) {
      throw new IllegalArgumentException(
          "Minimum nucleus size ("
              + minNucleusSize
              + ") must be less than maximum nucleus size ("
              + maxNucleusSize
              + ")");
    }

    // Normalize string inputs
    modelChoice = modelChoice.trim();
    outputType = outputType.trim();
    roiPosition = roiPosition.trim();
  }

  /**
   * Creates a new NuclearSegmentationSettings instance with default values.
   *
   * @return A new instance with default settings
   */
  public static NuclearSegmentationSettings createDefault() {
    return new NuclearSegmentationSettings(
        DEFAULT_MODEL_CHOICE,
        DEFAULT_NORMALIZE_INPUT,
        DEFAULT_PERCENTILE_BOTTOM,
        DEFAULT_PERCENTILE_TOP,
        DEFAULT_PROB_THRESH,
        DEFAULT_NMS_THRESH,
        DEFAULT_OUTPUT_TYPE,
        DEFAULT_N_TILES,
        DEFAULT_EXCLUDE_BOUNDARY,
        DEFAULT_ROI_POSITION,
        DEFAULT_VERBOSE,
        DEFAULT_SHOW_CSBDEEP_PROGRESS,
        DEFAULT_SHOW_PROB_AND_DIST,
        DEFAULT_MIN_NUCLEUS_SIZE,
        DEFAULT_MAX_NUCLEUS_SIZE);
  }

  /**
   * Creates a new instance with updated model choice.
   *
   * @param newModelChoice The new model choice
   * @return A new instance with the updated model choice
   * @throws IllegalArgumentException if modelChoice is invalid
   */
  public NuclearSegmentationSettings withModelChoice(String newModelChoice) {
    return new NuclearSegmentationSettings(
        newModelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated input normalization setting.
   *
   * @param newNormalizeInput Whether to enable input normalization
   * @return A new instance with the updated setting
   */
  public NuclearSegmentationSettings withNormalizeInput(boolean newNormalizeInput) {
    return new NuclearSegmentationSettings(
        modelChoice,
        newNormalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated bottom percentile.
   *
   * @param newPercentileBottom The new bottom percentile (0.0-100.0)
   * @return A new instance with the updated bottom percentile
   * @throws IllegalArgumentException if percentileBottom is invalid
   */
  public NuclearSegmentationSettings withPercentileBottom(float newPercentileBottom) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        newPercentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated top percentile.
   *
   * @param newPercentileTop The new top percentile (0.0-100.0)
   * @return A new instance with the updated top percentile
   * @throws IllegalArgumentException if percentileTop is invalid
   */
  public NuclearSegmentationSettings withPercentileTop(float newPercentileTop) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        newPercentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated probability threshold.
   *
   * @param newProbThresh The new probability threshold (0.0-1.0)
   * @return A new instance with the updated probability threshold
   * @throws IllegalArgumentException if probThresh is invalid
   */
  public NuclearSegmentationSettings withProbThresh(float newProbThresh) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        newProbThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated NMS threshold.
   *
   * @param newNmsThresh The new NMS threshold (0.0-1.0)
   * @return A new instance with the updated NMS threshold
   * @throws IllegalArgumentException if nmsThresh is invalid
   */
  public NuclearSegmentationSettings withNmsThresh(float newNmsThresh) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        newNmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated output type.
   *
   * @param newOutputType The new output type
   * @return A new instance with the updated output type
   * @throws IllegalArgumentException if outputType is invalid
   */
  public NuclearSegmentationSettings withOutputType(String newOutputType) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        newOutputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated number of tiles.
   *
   * @param newNTiles The new number of tiles (must be at least 1)
   * @return A new instance with the updated number of tiles
   * @throws IllegalArgumentException if nTiles is invalid
   */
  public NuclearSegmentationSettings withNTiles(int newNTiles) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        newNTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated boundary exclusion distance.
   *
   * @param newExcludeBoundary The new boundary exclusion distance (must be non-negative)
   * @return A new instance with the updated boundary exclusion distance
   * @throws IllegalArgumentException if excludeBoundary is invalid
   */
  public NuclearSegmentationSettings withExcludeBoundary(int newExcludeBoundary) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        newExcludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated ROI position setting.
   *
   * @param newRoiPosition The new ROI position setting
   * @return A new instance with the updated ROI position setting
   * @throws IllegalArgumentException if roiPosition is invalid
   */
  public NuclearSegmentationSettings withRoiPosition(String newRoiPosition) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        newRoiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated verbose setting.
   *
   * @param newVerbose Whether to enable verbose output
   * @return A new instance with the updated verbose setting
   */
  public NuclearSegmentationSettings withVerbose(boolean newVerbose) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        newVerbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated CSBDeep progress display setting.
   *
   * @param newShowCsbdeepProgress Whether to show CSBDeep progress
   * @return A new instance with the updated setting
   */
  public NuclearSegmentationSettings withShowCsbdeepProgress(boolean newShowCsbdeepProgress) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        newShowCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated probability and distance maps display setting.
   *
   * @param newShowProbAndDist Whether to show probability and distance maps
   * @return A new instance with the updated setting
   */
  public NuclearSegmentationSettings withShowProbAndDist(boolean newShowProbAndDist) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        newShowProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated minimum nucleus size.
   *
   * @param newMinNucleusSize The new minimum nucleus size (must be non-negative)
   * @return A new instance with the updated minimum nucleus size
   * @throws IllegalArgumentException if minNucleusSize is invalid
   */
  public NuclearSegmentationSettings withMinNucleusSize(double newMinNucleusSize) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        newMinNucleusSize,
        maxNucleusSize);
  }

  /**
   * Creates a new instance with updated maximum nucleus size.
   *
   * @param newMaxNucleusSize The new maximum nucleus size (must be non-negative)
   * @return A new instance with the updated maximum nucleus size
   * @throws IllegalArgumentException if maxNucleusSize is invalid
   */
  public NuclearSegmentationSettings withMaxNucleusSize(double newMaxNucleusSize) {
    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        outputType,
        nTiles,
        excludeBoundary,
        roiPosition,
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        newMaxNucleusSize);
  }

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
     if (percentileBottom >= percentileTop) {
       throw new IllegalStateException(
           "Invalid percentile range: bottom ("
               + percentileBottom
               + ") >= top ("
               + percentileTop
               + ")");
     }
     if (minNucleusSize >= maxNucleusSize) {
       throw new IllegalStateException(
           "Invalid nucleus size range: min ("
               + minNucleusSize
               + ") >= max ("
               + maxNucleusSize
               + ")");
     }
     if (probThresh < 0.0f || probThresh > 1.0f) {
       throw new IllegalStateException(
           "Invalid probability threshold: " + probThresh + " (must be 0.0-1.0)");
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
        "NuclearSegmentationSettings[model=%s, probThresh=%.3f, nmsThresh=%.3f, "
            + "percentiles=%.1f-%.1f, nucleusSize=%.1f-%.1f, tiles=%d]",
        modelChoice,
        probThresh,
        nmsThresh,
        percentileBottom,
        percentileTop,
        minNucleusSize,
        maxNucleusSize,
        nTiles);
  }
}
