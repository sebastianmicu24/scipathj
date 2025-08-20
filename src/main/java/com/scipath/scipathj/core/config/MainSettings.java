package com.scipath.scipathj.core.config;

import java.awt.*;

/**
 * Immutable configuration record for main application settings.
 * Manages scale conversion and type-specific ROI appearance settings.
 * Follows dependency injection principles and uses Java 16+ record syntax for immutability.
 *
 * @param pixelsPerMicrometer The scale conversion factor (pixels per micrometer)
 * @param scaleUnit The unit for scale display (e.g., "μm")
 * @param vesselSettings Appearance settings for vessel ROIs
 * @param nucleusSettings Appearance settings for nucleus ROIs
 * @param cytoplasmSettings Appearance settings for cytoplasm ROIs
 * @param cellSettings Appearance settings for cell ROIs
 *
 * @author Sebastian Micu
 * @version 2.0.0
 * @since 1.0.0
 */
public record MainSettings(
    double pixelsPerMicrometer,
    String scaleUnit,
    ROIAppearanceSettings vesselSettings,
    ROIAppearanceSettings nucleusSettings,
    ROIAppearanceSettings cytoplasmSettings,
    ROIAppearanceSettings cellSettings) {

  // Default values for scale conversion
  public static final double DEFAULT_PIXELS_PER_MICROMETER = 1.0;
  public static final String DEFAULT_SCALE_UNIT = "μm";

  // Legacy constants for backward compatibility
  public static final double DEFAULT_PIXEL_WIDTH = 1.0;
  public static final double DEFAULT_PIXEL_HEIGHT = 1.0;
  public static final String DEFAULT_PIXEL_UNIT = "μm";
  public static final Color DEFAULT_BORDER_COLOR = new Color(255, 0, 0);
  public static final float DEFAULT_FILL_OPACITY = 0.2f;
  public static final int DEFAULT_BORDER_WIDTH = 2;

  /**
   * Enum for different ROI categories that can have different appearance settings.
   */
  public enum ROICategory {
    VESSEL("Vessel", new Color(255, 0, 0), 0.2f, 2),
    NUCLEUS("Nucleus", new Color(0, 0, 255), 0.2f, 2),
    CYTOPLASM("Cytoplasm", new Color(0, 255, 0), 0.2f, 2),
    CELL("Cell", new Color(255, 255, 0), 0.1f, 1);

    private final String displayName;
    private final Color defaultBorderColor;
    private final float defaultFillOpacity;
    private final int defaultBorderWidth;

    ROICategory(
        String displayName,
        Color defaultBorderColor,
        float defaultFillOpacity,
        int defaultBorderWidth) {
      this.displayName = displayName;
      this.defaultBorderColor = defaultBorderColor;
      this.defaultFillOpacity = defaultFillOpacity;
      this.defaultBorderWidth = defaultBorderWidth;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Color getDefaultBorderColor() {
      return defaultBorderColor;
    }

    public float getDefaultFillOpacity() {
      return defaultFillOpacity;
    }

    public int getDefaultBorderWidth() {
      return defaultBorderWidth;
    }
  }

  /**
   * Immutable settings record for ROI appearance configuration.
   *
   * @param borderColor The border color for the ROI
   * @param fillOpacity The fill opacity (0.0-1.0)
   * @param borderWidth The border width in pixels (must be at least 1)
   */
  public record ROIAppearanceSettings(Color borderColor, float fillOpacity, int borderWidth) {

    /**
     * Compact constructor with validation.
     */
    public ROIAppearanceSettings {
      if (borderColor == null) {
        throw new IllegalArgumentException("Border color cannot be null");
      }
      if (fillOpacity < 0.0f || fillOpacity > 1.0f) {
        throw new IllegalArgumentException(
            "Fill opacity must be between 0.0 and 1.0, got: " + fillOpacity);
      }
      if (borderWidth < 1) {
        throw new IllegalArgumentException("Border width must be at least 1, got: " + borderWidth);
      }
    }

    /**
     * Creates ROI appearance settings from a category's defaults.
     *
     * @param category The ROI category to use for default values
     * @return A new instance with the category's default settings
     */
    public static ROIAppearanceSettings fromCategory(ROICategory category) {
      return new ROIAppearanceSettings(
          category.getDefaultBorderColor(),
          category.getDefaultFillOpacity(),
          category.getDefaultBorderWidth());
    }

    /**
     * Gets the fill color with the appropriate alpha channel based on fill opacity.
     *
     * @return Color with alpha channel applied
     */
    public Color getFillColor() {
      int alpha = Math.round(fillOpacity * 255);
      return new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), alpha);
    }

    /**
     * Creates a new instance with updated border color.
     *
     * @param newBorderColor The new border color
     * @return A new instance with the updated border color
     */
    public ROIAppearanceSettings withBorderColor(Color newBorderColor) {
      return new ROIAppearanceSettings(newBorderColor, fillOpacity, borderWidth);
    }

    /**
     * Creates a new instance with updated fill opacity.
     *
     * @param newFillOpacity The new fill opacity (0.0-1.0)
     * @return A new instance with the updated fill opacity
     */
    public ROIAppearanceSettings withFillOpacity(float newFillOpacity) {
      return new ROIAppearanceSettings(borderColor, newFillOpacity, borderWidth);
    }

    /**
     * Creates a new instance with updated border width.
     *
     * @param newBorderWidth The new border width (must be at least 1)
     * @return A new instance with the updated border width
     */
    public ROIAppearanceSettings withBorderWidth(int newBorderWidth) {
      return new ROIAppearanceSettings(borderColor, fillOpacity, newBorderWidth);
    }
  }

  /**
   * Compact constructor with validation.
   */
  public MainSettings {
    if (pixelsPerMicrometer <= 0) {
      throw new IllegalArgumentException(
          "Pixels per micrometer must be positive, got: " + pixelsPerMicrometer);
    }
    if (scaleUnit == null || scaleUnit.trim().isEmpty()) {
      throw new IllegalArgumentException("Scale unit cannot be null or empty");
    }
    if (vesselSettings == null) {
      throw new IllegalArgumentException("Vessel settings cannot be null");
    }
    if (nucleusSettings == null) {
      throw new IllegalArgumentException("Nucleus settings cannot be null");
    }
    if (cytoplasmSettings == null) {
      throw new IllegalArgumentException("Cytoplasm settings cannot be null");
    }
    if (cellSettings == null) {
      throw new IllegalArgumentException("Cell settings cannot be null");
    }
  }

  /**
   * Creates a new MainSettings instance with default values.
   *
   * @return A new MainSettings instance with default values
   */
  public static MainSettings createDefault() {
    return new MainSettings(
        DEFAULT_PIXELS_PER_MICROMETER,
        DEFAULT_SCALE_UNIT,
        ROIAppearanceSettings.fromCategory(ROICategory.VESSEL),
        ROIAppearanceSettings.fromCategory(ROICategory.NUCLEUS),
        ROIAppearanceSettings.fromCategory(ROICategory.CYTOPLASM),
        ROIAppearanceSettings.fromCategory(ROICategory.CELL));
  }

  /**
   * Get appearance settings for a specific ROI category.
   *
   * @param category The ROI category
   * @return The appearance settings for the category
   */
  public ROIAppearanceSettings getSettingsForCategory(ROICategory category) {
    return switch (category) {
      case VESSEL -> vesselSettings;
      case NUCLEUS -> nucleusSettings;
      case CYTOPLASM -> cytoplasmSettings;
      case CELL -> cellSettings;
    };
  }

  /**
   * Creates a new MainSettings instance with updated pixels per micrometer.
   *
   * @param newPixelsPerMicrometer The new pixels per micrometer value
   * @return A new MainSettings instance with updated value
   */
  public MainSettings withPixelsPerMicrometer(double newPixelsPerMicrometer) {
    return new MainSettings(
        newPixelsPerMicrometer,
        scaleUnit,
        vesselSettings,
        nucleusSettings,
        cytoplasmSettings,
        cellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated scale unit.
   *
   * @param newScaleUnit The new scale unit
   * @return A new MainSettings instance with updated value
   */
  public MainSettings withScaleUnit(String newScaleUnit) {
    return new MainSettings(
        pixelsPerMicrometer,
        newScaleUnit,
        vesselSettings,
        nucleusSettings,
        cytoplasmSettings,
        cellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated vessel settings.
   *
   * @param newVesselSettings The new vessel settings
   * @return A new MainSettings instance with updated settings
   */
  public MainSettings withVesselSettings(ROIAppearanceSettings newVesselSettings) {
    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        newVesselSettings,
        nucleusSettings,
        cytoplasmSettings,
        cellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated nucleus settings.
   *
   * @param newNucleusSettings The new nucleus settings
   * @return A new MainSettings instance with updated settings
   */
  public MainSettings withNucleusSettings(ROIAppearanceSettings newNucleusSettings) {
    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselSettings,
        newNucleusSettings,
        cytoplasmSettings,
        cellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated cytoplasm settings.
   *
   * @param newCytoplasmSettings The new cytoplasm settings
   * @return A new MainSettings instance with updated settings
   */
  public MainSettings withCytoplasmSettings(ROIAppearanceSettings newCytoplasmSettings) {
    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselSettings,
        nucleusSettings,
        newCytoplasmSettings,
        cellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated cell settings.
   *
   * @param newCellSettings The new cell settings
   * @return A new MainSettings instance with updated settings
   */
  public MainSettings withCellSettings(ROIAppearanceSettings newCellSettings) {
    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselSettings,
        nucleusSettings,
        cytoplasmSettings,
        newCellSettings);
  }

  /**
   * Creates a new MainSettings instance with updated settings for a specific category.
   *
   * @param category The ROI category to update
   * @param newSettings The new settings for the category
   * @return A new MainSettings instance with updated settings
   */
  public MainSettings withCategorySettings(
      ROICategory category, ROIAppearanceSettings newSettings) {
    return switch (category) {
      case VESSEL -> withVesselSettings(newSettings);
      case NUCLEUS -> withNucleusSettings(newSettings);
      case CYTOPLASM -> withCytoplasmSettings(newSettings);
      case CELL -> withCellSettings(newSettings);
    };
  }

  /**
   * Convert pixels to micrometers using the current scale.
   *
   * @param pixels The value in pixels
   * @return The value in micrometers
   */
  public double pixelsToMicrometers(double pixels) {
    return pixels / pixelsPerMicrometer;
  }

  /**
   * Convert micrometers to pixels using the current scale.
   *
   * @param micrometers The value in micrometers
   * @return The value in pixels
   */
  public double micrometersToPixels(double micrometers) {
    return micrometers * pixelsPerMicrometer;
  }

  /**
   * Get a formatted string representation of a pixel value in the current scale unit.
   *
   * @param pixels The value in pixels
   * @return Formatted string with value and unit
   */
  public String formatPixelsWithUnit(double pixels) {
    double scaledValue = pixelsToMicrometers(pixels);
    return String.format("%.2f %s", scaledValue, scaleUnit);
  }

  /**
   * Check if current settings are different from defaults.
   *
   * @return true if any setting differs from its default value
   */
  public boolean hasCustomValues() {
    MainSettings defaultSettings = createDefault();
    return !this.equals(defaultSettings);
  }

  // Legacy compatibility methods (delegate to vessel settings)

  /**
   * @deprecated Use vesselSettings().borderColor() instead
   */
  @Deprecated
  public Color getRoiBorderColor() {
    return vesselSettings.borderColor();
  }

  /**
   * @deprecated Use vesselSettings().getFillColor() instead
   */
  @Deprecated
  public Color getRoiFillColor() {
    return vesselSettings.getFillColor();
  }

  /**
   * @deprecated Use vesselSettings().fillOpacity() instead
   */
  @Deprecated
  public float getRoiFillOpacity() {
    return vesselSettings.fillOpacity();
  }

  /**
   * @deprecated Use pixelsPerMicrometer() instead
   */
  @Deprecated
  public double getPixelsPerMicrometer() {
    return pixelsPerMicrometer;
  }

  /**
   * @deprecated Use scaleUnit() instead
   */
  @Deprecated
  public String getScaleUnit() {
    return scaleUnit;
  }

  /**
   * @deprecated Use vesselSettings() instead
   */
  @Deprecated
  public ROIAppearanceSettings getVesselSettings() {
    return vesselSettings;
  }

  /**
   * @deprecated Use nucleusSettings() instead
   */
  @Deprecated
  public ROIAppearanceSettings getNucleusSettings() {
    return nucleusSettings;
  }

  /**
   * @deprecated Use cytoplasmSettings() instead
   */
  @Deprecated
  public ROIAppearanceSettings getCytoplasmSettings() {
    return cytoplasmSettings;
  }

  /**
   * @deprecated Use cellSettings() instead
   */
  @Deprecated
  public ROIAppearanceSettings getCellSettings() {
    return cellSettings;
  }
}
