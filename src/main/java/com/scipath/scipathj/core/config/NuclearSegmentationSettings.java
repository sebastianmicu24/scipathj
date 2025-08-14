package com.scipath.scipathj.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration settings for nuclear segmentation using StarDist.
 * This class manages all parameters related to nucleus detection and segmentation.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class NuclearSegmentationSettings {

  private static final Logger LOGGER = LoggerFactory.getLogger(NuclearSegmentationSettings.class);

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

  @JsonProperty("modelChoice")
  private String modelChoice = DEFAULT_MODEL_CHOICE;

  @JsonProperty("normalizeInput")
  private boolean normalizeInput = DEFAULT_NORMALIZE_INPUT;

  @JsonProperty("percentileBottom")
  private float percentileBottom = DEFAULT_PERCENTILE_BOTTOM;

  @JsonProperty("percentileTop")
  private float percentileTop = DEFAULT_PERCENTILE_TOP;

  @JsonProperty("probThresh")
  private float probThresh = DEFAULT_PROB_THRESH;

  @JsonProperty("nmsThresh")
  private float nmsThresh = DEFAULT_NMS_THRESH;

  @JsonProperty("outputType")
  private String outputType = DEFAULT_OUTPUT_TYPE;

  @JsonProperty("nTiles")
  private int nTiles = DEFAULT_N_TILES;

  @JsonProperty("excludeBoundary")
  private int excludeBoundary = DEFAULT_EXCLUDE_BOUNDARY;

  @JsonProperty("roiPosition")
  private String roiPosition = DEFAULT_ROI_POSITION;

  @JsonProperty("verbose")
  private boolean verbose = DEFAULT_VERBOSE;

  @JsonProperty("showCsbdeepProgress")
  private boolean showCsbdeepProgress = DEFAULT_SHOW_CSBDEEP_PROGRESS;

  @JsonProperty("showProbAndDist")
  private boolean showProbAndDist = DEFAULT_SHOW_PROB_AND_DIST;

  @JsonProperty("minNucleusSize")
  private double minNucleusSize = DEFAULT_MIN_NUCLEUS_SIZE;

  @JsonProperty("maxNucleusSize")
  private double maxNucleusSize = DEFAULT_MAX_NUCLEUS_SIZE;

  /**
   * Default constructor.
   */
  public NuclearSegmentationSettings() {
    LOGGER.debug("Created NuclearSegmentationSettings with default values");
  }

  /**
   * Gets the StarDist model choice.
   *
   * @return the model choice string
   */
  public String getModelChoice() {
    return modelChoice;
  }

  /**
   * Sets the StarDist model choice.
   *
   * @param modelChoice the model choice string
   */
  public void setModelChoice(String modelChoice) {
    this.modelChoice = modelChoice;
    LOGGER.debug("Set model choice to: {}", modelChoice);
  }

  /**
   * Gets whether input normalization is enabled.
   *
   * @return true if input normalization is enabled
   */
  public boolean isNormalizeInput() {
    return normalizeInput;
  }

  /**
   * Sets whether input normalization is enabled.
   *
   * @param normalizeInput true to enable input normalization
   */
  public void setNormalizeInput(boolean normalizeInput) {
    this.normalizeInput = normalizeInput;
    LOGGER.debug("Set normalize input to: {}", normalizeInput);
  }

  /**
   * Gets the bottom percentile for normalization.
   *
   * @return the bottom percentile value
   */
  public float getPercentileBottom() {
    return percentileBottom;
  }

  /**
   * Sets the bottom percentile for normalization.
   *
   * @param percentileBottom the bottom percentile value (0.0-100.0)
   */
  public void setPercentileBottom(float percentileBottom) {
    this.percentileBottom = Math.max(0.0f, Math.min(100.0f, percentileBottom));
    LOGGER.debug("Set percentile bottom to: {}", this.percentileBottom);
  }

  /**
   * Gets the top percentile for normalization.
   *
   * @return the top percentile value
   */
  public float getPercentileTop() {
    return percentileTop;
  }

  /**
   * Sets the top percentile for normalization.
   *
   * @param percentileTop the top percentile value (0.0-100.0)
   */
  public void setPercentileTop(float percentileTop) {
    this.percentileTop = Math.max(0.0f, Math.min(100.0f, percentileTop));
    LOGGER.debug("Set percentile top to: {}", this.percentileTop);
  }

  /**
   * Gets the probability threshold for nucleus detection.
   *
   * @return the probability threshold (0.0-1.0)
   */
  public float getProbThresh() {
    return probThresh;
  }

  /**
   * Sets the probability threshold for nucleus detection.
   *
   * @param probThresh the probability threshold (0.0-1.0)
   */
  public void setProbThresh(float probThresh) {
    this.probThresh = Math.max(0.0f, Math.min(1.0f, probThresh));
    LOGGER.debug("Set probability threshold to: {}", this.probThresh);
  }

  /**
   * Gets the non-maximum suppression threshold.
   *
   * @return the NMS threshold (0.0-1.0)
   */
  public float getNmsThresh() {
    return nmsThresh;
  }

  /**
   * Sets the non-maximum suppression threshold.
   *
   * @param nmsThresh the NMS threshold (0.0-1.0)
   */
  public void setNmsThresh(float nmsThresh) {
    this.nmsThresh = Math.max(0.0f, Math.min(1.0f, nmsThresh));
    LOGGER.debug("Set NMS threshold to: {}", this.nmsThresh);
  }

  /**
   * Gets the output type for StarDist results.
   *
   * @return the output type string
   */
  public String getOutputType() {
    return outputType;
  }

  /**
   * Sets the output type for StarDist results.
   *
   * @param outputType the output type string
   */
  public void setOutputType(String outputType) {
    this.outputType = outputType;
    LOGGER.debug("Set output type to: {}", outputType);
  }

  /**
   * Gets the number of tiles for processing large images.
   *
   * @return the number of tiles
   */
  public int getNTiles() {
    return nTiles;
  }

  /**
   * Sets the number of tiles for processing large images.
   *
   * @param nTiles the number of tiles (must be positive)
   */
  public void setNTiles(int nTiles) {
    this.nTiles = Math.max(1, nTiles);
    LOGGER.debug("Set number of tiles to: {}", this.nTiles);
  }

  /**
   * Gets the boundary exclusion distance.
   *
   * @return the boundary exclusion distance in pixels
   */
  public int getExcludeBoundary() {
    return excludeBoundary;
  }

  /**
   * Sets the boundary exclusion distance.
   *
   * @param excludeBoundary the boundary exclusion distance in pixels
   */
  public void setExcludeBoundary(int excludeBoundary) {
    this.excludeBoundary = Math.max(0, excludeBoundary);
    LOGGER.debug("Set exclude boundary to: {}", this.excludeBoundary);
  }

  /**
   * Gets the ROI position setting.
   *
   * @return the ROI position string
   */
  public String getRoiPosition() {
    return roiPosition;
  }

  /**
   * Sets the ROI position setting.
   *
   * @param roiPosition the ROI position string
   */
  public void setRoiPosition(String roiPosition) {
    this.roiPosition = roiPosition;
    LOGGER.debug("Set ROI position to: {}", roiPosition);
  }

  /**
   * Gets whether verbose output is enabled.
   *
   * @return true if verbose output is enabled
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets whether verbose output is enabled.
   *
   * @param verbose true to enable verbose output
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
    LOGGER.debug("Set verbose to: {}", verbose);
  }

  /**
   * Gets whether CSBDeep progress display is enabled.
   *
   * @return true if CSBDeep progress display is enabled
   */
  public boolean isShowCsbdeepProgress() {
    return showCsbdeepProgress;
  }

  /**
   * Sets whether CSBDeep progress display is enabled.
   *
   * @param showCsbdeepProgress true to show CSBDeep progress
   */
  public void setShowCsbdeepProgress(boolean showCsbdeepProgress) {
    this.showCsbdeepProgress = showCsbdeepProgress;
    LOGGER.debug("Set show CSBDeep progress to: {}", showCsbdeepProgress);
  }

  /**
   * Gets whether probability and distance maps should be shown.
   *
   * @return true if probability and distance maps should be shown
   */
  public boolean isShowProbAndDist() {
    return showProbAndDist;
  }

  /**
   * Sets whether probability and distance maps should be shown.
   *
   * @param showProbAndDist true to show probability and distance maps
   */
  public void setShowProbAndDist(boolean showProbAndDist) {
    this.showProbAndDist = showProbAndDist;
    LOGGER.debug("Set show probability and distance to: {}", showProbAndDist);
  }

  /**
   * Gets the minimum nucleus size for filtering.
   *
   * @return the minimum nucleus size in pixels
   */
  public double getMinNucleusSize() {
    return minNucleusSize;
  }

  /**
   * Sets the minimum nucleus size for filtering.
   *
   * @param minNucleusSize the minimum nucleus size in pixels
   */
  public void setMinNucleusSize(double minNucleusSize) {
    this.minNucleusSize = Math.max(0.0, minNucleusSize);
    LOGGER.debug("Set minimum nucleus size to: {}", this.minNucleusSize);
  }

  /**
   * Gets the maximum nucleus size for filtering.
   *
   * @return the maximum nucleus size in pixels
   */
  public double getMaxNucleusSize() {
    return maxNucleusSize;
  }

  /**
   * Sets the maximum nucleus size for filtering.
   *
   * @param maxNucleusSize the maximum nucleus size in pixels
   */
  public void setMaxNucleusSize(double maxNucleusSize) {
    this.maxNucleusSize = Math.max(0.0, maxNucleusSize);
    LOGGER.debug("Set maximum nucleus size to: {}", this.maxNucleusSize);
  }

  /**
   * Validates the current settings and returns whether they are valid.
   *
   * @return true if settings are valid, false otherwise
   */
  public boolean isValid() {
    boolean valid = true;

    if (percentileBottom >= percentileTop) {
      LOGGER.warn(
          "Invalid percentile range: bottom ({}) >= top ({})", percentileBottom, percentileTop);
      valid = false;
    }

    if (minNucleusSize >= maxNucleusSize) {
      LOGGER.warn(
          "Invalid nucleus size range: min ({}) >= max ({})", minNucleusSize, maxNucleusSize);
      valid = false;
    }

    if (probThresh < 0.0f || probThresh > 1.0f) {
      LOGGER.warn("Invalid probability threshold: {} (must be 0.0-1.0)", probThresh);
      valid = false;
    }

    return valid;
  }

  /**
   * Resets all settings to their default values.
   */
  public void resetToDefaults() {
    this.modelChoice = DEFAULT_MODEL_CHOICE;
    this.normalizeInput = DEFAULT_NORMALIZE_INPUT;
    this.percentileBottom = DEFAULT_PERCENTILE_BOTTOM;
    this.percentileTop = DEFAULT_PERCENTILE_TOP;
    this.probThresh = DEFAULT_PROB_THRESH;
    this.nmsThresh = DEFAULT_NMS_THRESH;
    this.outputType = DEFAULT_OUTPUT_TYPE;
    this.nTiles = DEFAULT_N_TILES;
    this.excludeBoundary = DEFAULT_EXCLUDE_BOUNDARY;
    this.roiPosition = DEFAULT_ROI_POSITION;
    this.verbose = DEFAULT_VERBOSE;
    this.showCsbdeepProgress = DEFAULT_SHOW_CSBDEEP_PROGRESS;
    this.showProbAndDist = DEFAULT_SHOW_PROB_AND_DIST;
    this.minNucleusSize = DEFAULT_MIN_NUCLEUS_SIZE;
    this.maxNucleusSize = DEFAULT_MAX_NUCLEUS_SIZE;

    LOGGER.info("Reset nuclear segmentation settings to defaults");
  }

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
