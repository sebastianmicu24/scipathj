package com.scipath.scipathj.core.config;

/**
 * Settings record for cytoplasm segmentation using Voronoi tessellation.
 * Immutable data carrier that manages all parameters related to cytoplasm detection and cell creation.
 * Uses Java 16+ record syntax for conciseness and immutability.
 *
 * @param useVesselExclusion Whether vessels should be excluded from cytoplasm regions
 * @param addImageBorder Whether to add a border around the image
 * @param borderWidth The width of the image border in pixels
 * @param applyVoronoi Whether Voronoi tessellation should be applied
 * @param minCellSize The minimum cell size in pixels
 * @param maxCellSize The maximum cell size in pixels
 * @param minCytoplasmSize The minimum cytoplasm size in pixels
 * @param validateCellShape Whether cell shape validation should be performed
 * @param maxAspectRatio The maximum allowed aspect ratio for cells
 * @param linkNucleusToCytoplasm Whether nucleus-cytoplasm linking should be performed
 * @param createCellROIs Whether cell ROIs should be created
 * @param excludeBorderCells Whether border cells should be excluded
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public record CytoplasmSegmentationSettings(
    boolean useVesselExclusion,
    boolean addImageBorder,
    int borderWidth,
    boolean applyVoronoi,
    double minCellSize,
    double maxCellSize,
    double minCytoplasmSize,
    boolean validateCellShape,
    double maxAspectRatio,
    boolean linkNucleusToCytoplasm,
    boolean createCellROIs,
    boolean excludeBorderCells) {

  // Default values based on SCHELI implementation
  public static final boolean DEFAULT_USE_VESSEL_EXCLUSION = true;
  public static final boolean DEFAULT_ADD_IMAGE_BORDER = true;
  public static final int DEFAULT_BORDER_WIDTH = 1;
  public static final boolean DEFAULT_APPLY_VORONOI = true;
  public static final double DEFAULT_MIN_CELL_SIZE = 100.0;
  public static final double DEFAULT_MAX_CELL_SIZE = 10000.0;
  public static final double DEFAULT_MIN_CYTOPLASM_SIZE = 50.0;
  public static final boolean DEFAULT_VALIDATE_CELL_SHAPE = true;
  public static final double DEFAULT_MAX_ASPECT_RATIO = 5.0;
  public static final boolean DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM = true;
  public static final boolean DEFAULT_CREATE_CELL_ROIS = true;
  public static final boolean DEFAULT_EXCLUDE_BORDER_CELLS = false;

  // Additional constants needed by ConfigurationManager
  public static final double DEFAULT_VORONOI_EXPANSION = 5.0;
  public static final double DEFAULT_GAUSSIAN_BLUR_SIGMA = 1.0;
  public static final double DEFAULT_MORPH_CLOSING_RADIUS = 2.0;
  public static final double DEFAULT_WATERSHED_TOLERANCE = 0.5;
  public static final double DEFAULT_MIN_CYTOPLASM_AREA = 50.0;
  public static final double DEFAULT_MAX_CYTOPLASM_AREA = 10000.0;
  public static final boolean DEFAULT_FILL_HOLES = true;
  public static final boolean DEFAULT_SMOOTH_BOUNDARIES = true;
  public static final boolean DEFAULT_VERBOSE = false;

  /**
   * Creates a new CytoplasmSegmentationSettings with validation.
   *
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public CytoplasmSegmentationSettings {
    if (borderWidth < 1) {
      throw new IllegalArgumentException("Border width must be at least 1, got: " + borderWidth);
    }
    if (minCellSize < 0) {
      throw new IllegalArgumentException(
          "Minimum cell size must be non-negative, got: " + minCellSize);
    }
    if (maxCellSize < 0) {
      throw new IllegalArgumentException(
          "Maximum cell size must be non-negative, got: " + maxCellSize);
    }
    if (minCellSize >= maxCellSize) {
      throw new IllegalArgumentException(
          "Minimum cell size ("
              + minCellSize
              + ") must be less than maximum cell size ("
              + maxCellSize
              + ")");
    }
    if (minCytoplasmSize < 0) {
      throw new IllegalArgumentException(
          "Minimum cytoplasm size must be non-negative, got: " + minCytoplasmSize);
    }
    if (maxAspectRatio < 1.0) {
      throw new IllegalArgumentException(
          "Maximum aspect ratio must be at least 1.0, got: " + maxAspectRatio);
    }
  }

  /**
   * Creates a new CytoplasmSegmentationSettings instance with default values.
   *
   * @return A new instance with default settings
   */
  public static CytoplasmSegmentationSettings createDefault() {
    return new CytoplasmSegmentationSettings(
        DEFAULT_USE_VESSEL_EXCLUSION,
        DEFAULT_ADD_IMAGE_BORDER,
        DEFAULT_BORDER_WIDTH,
        DEFAULT_APPLY_VORONOI,
        DEFAULT_MIN_CELL_SIZE,
        DEFAULT_MAX_CELL_SIZE,
        DEFAULT_MIN_CYTOPLASM_SIZE,
        DEFAULT_VALIDATE_CELL_SHAPE,
        DEFAULT_MAX_ASPECT_RATIO,
        DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM,
        DEFAULT_CREATE_CELL_ROIS,
        DEFAULT_EXCLUDE_BORDER_CELLS);
  }

  /**
   * Alias for useVesselExclusion() for compatibility.
   *
   * @return true if vessels should be excluded from cytoplasm regions
   */
  public boolean isExcludeVessels() {
    return useVesselExclusion;
  }

  /**
   * Alias for minCytoplasmSize() for compatibility.
   *
   * @return the minimum cytoplasm area
   */
  public double getMinCytoplasmArea() {
    return minCytoplasmSize;
  }

  /**
   * Creates a new instance with updated vessel exclusion setting.
   *
   * @param newUseVesselExclusion Whether to exclude vessels from cytoplasm regions
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withUseVesselExclusion(boolean newUseVesselExclusion) {
    return new CytoplasmSegmentationSettings(
        newUseVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Alias for withUseVesselExclusion() for compatibility.
   *
   * @param excludeVessels Whether to exclude vessels from cytoplasm regions
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withExcludeVessels(boolean excludeVessels) {
    return withUseVesselExclusion(excludeVessels);
  }

  /**
   * Creates a new instance with updated image border setting.
   *
   * @param newAddImageBorder Whether to add image border
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withAddImageBorder(boolean newAddImageBorder) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        newAddImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated border width.
   *
   * @param newBorderWidth The new border width (must be at least 1)
   * @return A new instance with the updated border width
   * @throws IllegalArgumentException if borderWidth is invalid
   */
  public CytoplasmSegmentationSettings withBorderWidth(int newBorderWidth) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        newBorderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated Voronoi application setting.
   *
   * @param newApplyVoronoi Whether to apply Voronoi tessellation
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withApplyVoronoi(boolean newApplyVoronoi) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        newApplyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated minimum cell size.
   *
   * @param newMinCellSize The new minimum cell size (must be non-negative)
   * @return A new instance with the updated minimum cell size
   * @throws IllegalArgumentException if minCellSize is invalid
   */
  public CytoplasmSegmentationSettings withMinCellSize(double newMinCellSize) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        newMinCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated maximum cell size.
   *
   * @param newMaxCellSize The new maximum cell size (must be non-negative)
   * @return A new instance with the updated maximum cell size
   * @throws IllegalArgumentException if maxCellSize is invalid
   */
  public CytoplasmSegmentationSettings withMaxCellSize(double newMaxCellSize) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        newMaxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated minimum cytoplasm size.
   *
   * @param newMinCytoplasmSize The new minimum cytoplasm size (must be non-negative)
   * @return A new instance with the updated minimum cytoplasm size
   * @throws IllegalArgumentException if minCytoplasmSize is invalid
   */
  public CytoplasmSegmentationSettings withMinCytoplasmSize(double newMinCytoplasmSize) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        newMinCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Alias for withMinCytoplasmSize() for compatibility.
   *
   * @param newMinCytoplasmArea The new minimum cytoplasm area (must be non-negative)
   * @return A new instance with the updated minimum cytoplasm area
   * @throws IllegalArgumentException if minCytoplasmArea is invalid
   */
  public CytoplasmSegmentationSettings withMinCytoplasmArea(double newMinCytoplasmArea) {
    return withMinCytoplasmSize(newMinCytoplasmArea);
  }

  /**
   * Creates a new instance with updated cell shape validation setting.
   *
   * @param newValidateCellShape Whether to validate cell shapes
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withValidateCellShape(boolean newValidateCellShape) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        newValidateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated maximum aspect ratio.
   *
   * @param newMaxAspectRatio The new maximum aspect ratio (must be at least 1.0)
   * @return A new instance with the updated maximum aspect ratio
   * @throws IllegalArgumentException if maxAspectRatio is invalid
   */
  public CytoplasmSegmentationSettings withMaxAspectRatio(double newMaxAspectRatio) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        newMaxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated nucleus-cytoplasm linking setting.
   *
   * @param newLinkNucleusToCytoplasm Whether to link nucleus and cytoplasm
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withLinkNucleusToCytoplasm(
      boolean newLinkNucleusToCytoplasm) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        newLinkNucleusToCytoplasm,
        createCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated cell ROI creation setting.
   *
   * @param newCreateCellROIs Whether to create cell ROIs
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withCreateCellROIs(boolean newCreateCellROIs) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        newCreateCellROIs,
        excludeBorderCells);
  }

  /**
   * Creates a new instance with updated border cell exclusion setting.
   *
   * @param newExcludeBorderCells Whether to exclude border cells
   * @return A new instance with the updated setting
   */
  public CytoplasmSegmentationSettings withExcludeBorderCells(boolean newExcludeBorderCells) {
    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        addImageBorder,
        borderWidth,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        validateCellShape,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs,
        newExcludeBorderCells);
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
    if (minCellSize >= maxCellSize) {
      throw new IllegalStateException(
          "Invalid cell size range: min (" + minCellSize + ") >= max (" + maxCellSize + ")");
    }
    if (minCytoplasmSize < 0) {
      throw new IllegalStateException(
          "Invalid minimum cytoplasm size: " + minCytoplasmSize + " (must be >= 0)");
    }
    if (maxAspectRatio < 1.0) {
      throw new IllegalStateException(
          "Invalid maximum aspect ratio: " + maxAspectRatio + " (must be >= 1.0)");
    }
    if (borderWidth < 1) {
      throw new IllegalStateException("Invalid border width: " + borderWidth + " (must be >= 1)");
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
        "CytoplasmSegmentationSettings[vessels=%s, voronoi=%s, cellSize=%.1f-%.1f, "
            + "cytoplasmSize>=%.1f, aspectRatio<=%.1f, link=%s, createCells=%s]",
        useVesselExclusion,
        applyVoronoi,
        minCellSize,
        maxCellSize,
        minCytoplasmSize,
        maxAspectRatio,
        linkNucleusToCytoplasm,
        createCellROIs);
  }
}
