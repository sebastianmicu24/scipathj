package com.scipath.scipathj.core.config;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application settings class for global configuration.
 * Manages scale conversion, type-specific ROI appearance, and other global settings with persistence.
 * Uses dependency injection instead of singleton pattern for better testability and flexibility.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class MainSettings {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainSettings.class);

  /**
   * Enum for different ROI types that can have different appearance settings
   */
  public enum ROICategory {
    VESSEL("Vessel", new Color(255, 0, 0), 0.2f, 2), // Red
    NUCLEUS("Nucleus", new Color(0, 0, 255), 0.2f, 2), // Blue
    CYTOPLASM("Cytoplasm", new Color(0, 255, 0), 0.2f, 2), // Green
    CELL("Cell", new Color(255, 255, 0), 0.1f, 1); // Yellow

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
   * Uses Java 16+ record syntax for conciseness and immutability.
   *
   * @param borderColor The border color for the ROI
   * @param fillOpacity The fill opacity (0.0-1.0)
   * @param borderWidth The border width in pixels (must be at least 1)
   *
   * @author Sebastian Micu
   * @version 1.0.0
   * @since 1.0.0
   */
  public record ROIAppearanceSettings(Color borderColor, float fillOpacity, int borderWidth) {

    /**
     * Creates a new ROIAppearanceSettings with validation.
     *
     * @throws IllegalArgumentException if any parameter is invalid
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
     * Creates a new ROIAppearanceSettings from a ROI category's defaults.
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
     * @throws IllegalArgumentException if borderColor is null
     */
    public ROIAppearanceSettings withBorderColor(Color newBorderColor) {
      return new ROIAppearanceSettings(newBorderColor, fillOpacity, borderWidth);
    }

    /**
     * Creates a new instance with updated fill opacity.
     *
     * @param newFillOpacity The new fill opacity (0.0-1.0)
     * @return A new instance with the updated fill opacity
     * @throws IllegalArgumentException if fillOpacity is invalid
     */
    public ROIAppearanceSettings withFillOpacity(float newFillOpacity) {
      return new ROIAppearanceSettings(borderColor, newFillOpacity, borderWidth);
    }

    /**
     * Creates a new instance with updated border width.
     *
     * @param newBorderWidth The new border width (must be at least 1)
     * @return A new instance with the updated border width
     * @throws IllegalArgumentException if borderWidth is invalid
     */
    public ROIAppearanceSettings withBorderWidth(int newBorderWidth) {
      return new ROIAppearanceSettings(borderColor, fillOpacity, newBorderWidth);
    }

    /**
     * Creates a copy of this settings instance.
     *
     * @return A new instance with the same values
     */
    public ROIAppearanceSettings copy() {
      return new ROIAppearanceSettings(new Color(borderColor.getRGB()), fillOpacity, borderWidth);
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
     *
     * @throws IllegalStateException if any setting is invalid
     */
    public void validate() {
      if (borderColor == null) {
        throw new IllegalStateException("Border color cannot be null");
      }
      if (fillOpacity < 0.0f || fillOpacity > 1.0f) {
        throw new IllegalStateException(
            "Invalid fill opacity: " + fillOpacity + " (must be 0.0-1.0)");
      }
      if (borderWidth < 1) {
        throw new IllegalStateException(
            "Invalid border width: " + borderWidth + " (must be at least 1)");
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
          "ROIAppearanceSettings[RGB(%d,%d,%d), opacity=%.2f, width=%d]",
          borderColor.getRed(),
          borderColor.getGreen(),
          borderColor.getBlue(),
          fillOpacity,
          borderWidth);
    }
  }

  // Default values for scale conversion
  public static final double DEFAULT_PIXELS_PER_MICROMETER =
      1.0; // 1 pixel = 1 micrometer by default
  public static final String DEFAULT_SCALE_UNIT = "μm"; // micrometers

  // Additional constants needed by ConfigurationManager
  public static final double DEFAULT_PIXEL_WIDTH = 1.0;
  public static final double DEFAULT_PIXEL_HEIGHT = 1.0;
  public static final String DEFAULT_PIXEL_UNIT = "μm";
  public static final Color DEFAULT_BORDER_COLOR = new Color(255, 0, 0); // Red
  public static final float DEFAULT_FILL_OPACITY = 0.2f;
  public static final int DEFAULT_BORDER_WIDTH = 2;

  // Current values (initialized with defaults)
  private double pixelsPerMicrometer = DEFAULT_PIXELS_PER_MICROMETER;
  private String scaleUnit = DEFAULT_SCALE_UNIT;

  // Type-specific ROI appearance settings (mutable references to immutable records)
  private ROIAppearanceSettings vesselSettings;
  private ROIAppearanceSettings nucleusSettings;
  private ROIAppearanceSettings cytoplasmSettings;
  private ROIAppearanceSettings cellSettings;

  // Settings change listeners
  private final List<SettingsChangeListener> listeners;

  public interface SettingsChangeListener {
    void onSettingsChanged();
  }

  /**
   * Creates a new MainSettings instance with default values.
   * Uses dependency injection pattern instead of singleton for better testability.
   */
  public MainSettings() {
    // Initialize type-specific settings with defaults
    this.vesselSettings = ROIAppearanceSettings.fromCategory(ROICategory.VESSEL);
    this.nucleusSettings = ROIAppearanceSettings.fromCategory(ROICategory.NUCLEUS);
    this.cytoplasmSettings = ROIAppearanceSettings.fromCategory(ROICategory.CYTOPLASM);
    this.cellSettings = ROIAppearanceSettings.fromCategory(ROICategory.CELL);

    this.listeners = new CopyOnWriteArrayList<>();
    LOGGER.debug("MainSettings initialized with default values for all ROI types");
  }

  /**
   * Creates a new MainSettings instance with custom values.
   *
   * @param pixelsPerMicrometer The scale conversion factor
   * @param scaleUnit The unit for scale display
   * @param vesselSettings Settings for vessel ROIs
   * @param nucleusSettings Settings for nucleus ROIs
   * @param cytoplasmSettings Settings for cytoplasm ROIs
   * @param cellSettings Settings for cell ROIs
   */
  public MainSettings(
      double pixelsPerMicrometer,
      String scaleUnit,
      ROIAppearanceSettings vesselSettings,
      ROIAppearanceSettings nucleusSettings,
      ROIAppearanceSettings cytoplasmSettings,
      ROIAppearanceSettings cellSettings) {
    this.pixelsPerMicrometer = pixelsPerMicrometer;
    this.scaleUnit = scaleUnit;
    this.vesselSettings = vesselSettings;
    this.nucleusSettings = nucleusSettings;
    this.cytoplasmSettings = cytoplasmSettings;
    this.cellSettings = cellSettings;
    this.listeners = new CopyOnWriteArrayList<>();

    validate(); // Validate all settings on construction
    LOGGER.debug("MainSettings initialized with custom values");
  }

  // Scale conversion getters
  public double getPixelsPerMicrometer() {
    return pixelsPerMicrometer;
  }

  public String getScaleUnit() {
    return scaleUnit;
  }

  // Settings change listener management
  public void addSettingsChangeListener(SettingsChangeListener listener) {
    listeners.add(listener);
  }

  public void removeSettingsChangeListener(SettingsChangeListener listener) {
    listeners.remove(listener);
  }

  private void notifySettingsChanged() {
    listeners.forEach(
        listener -> {
          try {
            listener.onSettingsChanged();
          } catch (Exception e) {
            LOGGER.error("Error notifying settings change listener", e);
          }
        });
  }

  // Type-specific ROI appearance getters
  public ROIAppearanceSettings getVesselSettings() {
    return vesselSettings;
  }

  public ROIAppearanceSettings getNucleusSettings() {
    return nucleusSettings;
  }

  public ROIAppearanceSettings getCytoplasmSettings() {
    return cytoplasmSettings;
  }

  public ROIAppearanceSettings getCellSettings() {
    return cellSettings;
  }

  // Convenience methods for backward compatibility (use vessel settings as default)
  public Color getRoiBorderColor() {
    return vesselSettings.borderColor();
  }

  public Color getRoiFillColor() {
    return vesselSettings.getFillColor();
  }

  public float getRoiFillOpacity() {
    return vesselSettings.fillOpacity();
  }

  /**
   * Get appearance settings for a specific ROI category
   */
  public ROIAppearanceSettings getSettingsForCategory(ROICategory category) {
    switch (category) {
      case VESSEL:
        return vesselSettings;
      case NUCLEUS:
        return nucleusSettings;
      case CYTOPLASM:
        return cytoplasmSettings;
      case CELL:
        return cellSettings;
      default:
        return vesselSettings; // Default to vessel settings
    }
  }

  // Scale conversion setters with validation
  public void setPixelsPerMicrometer(double pixelsPerMicrometer) {
    if (pixelsPerMicrometer <= 0) {
      throw new IllegalArgumentException("Pixels per micrometer must be positive");
    }
    this.pixelsPerMicrometer = pixelsPerMicrometer;
    LOGGER.debug("Pixels per micrometer set to: {}", pixelsPerMicrometer);
  }

  public void setScaleUnit(String scaleUnit) {
    if (scaleUnit == null || scaleUnit.trim().isEmpty()) {
      throw new IllegalArgumentException("Scale unit cannot be null or empty");
    }
    this.scaleUnit = scaleUnit.trim();
    LOGGER.debug("Scale unit set to: {}", scaleUnit);
  }

  // ROI settings setters (creates new immutable instances)
  public void setVesselSettings(ROIAppearanceSettings newSettings) {
    if (newSettings == null) {
      throw new IllegalArgumentException("Vessel settings cannot be null");
    }
    this.vesselSettings = newSettings;
    notifySettingsChanged();
    LOGGER.debug("Vessel settings updated: {}", newSettings);
  }

  public void setNucleusSettings(ROIAppearanceSettings newSettings) {
    if (newSettings == null) {
      throw new IllegalArgumentException("Nucleus settings cannot be null");
    }
    this.nucleusSettings = newSettings;
    notifySettingsChanged();
    LOGGER.debug("Nucleus settings updated: {}", newSettings);
  }

  public void setCytoplasmSettings(ROIAppearanceSettings newSettings) {
    if (newSettings == null) {
      throw new IllegalArgumentException("Cytoplasm settings cannot be null");
    }
    this.cytoplasmSettings = newSettings;
    notifySettingsChanged();
    LOGGER.debug("Cytoplasm settings updated: {}", newSettings);
  }

  public void setCellSettings(ROIAppearanceSettings newSettings) {
    if (newSettings == null) {
      throw new IllegalArgumentException("Cell settings cannot be null");
    }
    this.cellSettings = newSettings;
    notifySettingsChanged();
    LOGGER.debug("Cell settings updated: {}", newSettings);
  }

  // Convenience setters for backward compatibility (delegates to vessel settings)
  public void setRoiBorderColor(Color roiBorderColor) {
    setVesselSettings(vesselSettings.withBorderColor(roiBorderColor));
    LOGGER.debug(
        "Vessel ROI border color set to: RGB({}, {}, {})",
        roiBorderColor.getRed(),
        roiBorderColor.getGreen(),
        roiBorderColor.getBlue());
  }

  public void setRoiFillOpacity(float roiFillOpacity) {
    setVesselSettings(vesselSettings.withFillOpacity(roiFillOpacity));
    LOGGER.debug("Vessel ROI fill opacity set to: {}", roiFillOpacity);
  }

  public void setRoiBorderWidth(int roiBorderWidth) {
    setVesselSettings(vesselSettings.withBorderWidth(roiBorderWidth));
    LOGGER.debug("Vessel ROI border width set to: {}", roiBorderWidth);
  }

  /**
   * Update settings for a specific ROI category
   */
  public void updateCategorySettings(
      ROICategory category, Color borderColor, float fillOpacity, int borderWidth) {
    ROIAppearanceSettings newSettings =
        new ROIAppearanceSettings(borderColor, fillOpacity, borderWidth);

    switch (category) {
      case VESSEL:
        setVesselSettings(newSettings);
        break;
      case NUCLEUS:
        setNucleusSettings(newSettings);
        break;
      case CYTOPLASM:
        setCytoplasmSettings(newSettings);
        break;
      case CELL:
        setCellSettings(newSettings);
        break;
      default:
        throw new IllegalArgumentException("Unknown ROI category: " + category);
    }

    LOGGER.debug(
        "Updated {} settings: color=RGB({},{},{}), opacity={}, width={}",
        category.getDisplayName(),
        borderColor.getRed(),
        borderColor.getGreen(),
        borderColor.getBlue(),
        fillOpacity,
        borderWidth);
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
   * Reset all settings to their default values.
   */
  public void resetToDefaults() {
    pixelsPerMicrometer = DEFAULT_PIXELS_PER_MICROMETER;
    scaleUnit = DEFAULT_SCALE_UNIT;

    // Reset all ROI category settings to defaults
    vesselSettings = ROIAppearanceSettings.fromCategory(ROICategory.VESSEL);
    nucleusSettings = ROIAppearanceSettings.fromCategory(ROICategory.NUCLEUS);
    cytoplasmSettings = ROIAppearanceSettings.fromCategory(ROICategory.CYTOPLASM);
    cellSettings = ROIAppearanceSettings.fromCategory(ROICategory.CELL);

    notifySettingsChanged();
    LOGGER.info(
        "Main settings reset to defaults for vessel, nucleus, cytoplasm, and cell ROI types");
  }

  /**
   * Check if current settings are different from defaults.
   *
   * @return true if any setting differs from its default value
   */
  public boolean hasCustomValues() {
    return pixelsPerMicrometer != DEFAULT_PIXELS_PER_MICROMETER
        || !scaleUnit.equals(DEFAULT_SCALE_UNIT)
        || hasCustomROISettings();
  }

  private boolean hasCustomROISettings() {
    ROIAppearanceSettings defaultVessel = ROIAppearanceSettings.fromCategory(ROICategory.VESSEL);
    ROIAppearanceSettings defaultNucleus = ROIAppearanceSettings.fromCategory(ROICategory.NUCLEUS);
    ROIAppearanceSettings defaultCytoplasm =
        ROIAppearanceSettings.fromCategory(ROICategory.CYTOPLASM);
    ROIAppearanceSettings defaultCell = ROIAppearanceSettings.fromCategory(ROICategory.CELL);

    return !vesselSettings.equals(defaultVessel)
        || !nucleusSettings.equals(defaultNucleus)
        || !cytoplasmSettings.equals(defaultCytoplasm)
        || !cellSettings.equals(defaultCell);
  }

  /**
   * Get a string representation of current settings.
   *
   * @return String representation of settings
   */
  @Override
  public String toString() {
    return String.format(
        "MainSettings{pixelsPerMicrometer=%.3f, scaleUnit='%s', "
            + "vessel=%s, nucleus=%s, cytoplasm=%s, cell=%s}",
        pixelsPerMicrometer,
        scaleUnit,
        formatROISettings("Vessel", vesselSettings),
        formatROISettings("Nucleus", nucleusSettings),
        formatROISettings("Cytoplasm", cytoplasmSettings),
        formatROISettings("Cell", cellSettings));
  }

  private String formatROISettings(String type, ROIAppearanceSettings settings) {
    return String.format(
        "%s[RGB(%d,%d,%d),opacity=%.2f,width=%d]",
        type,
        settings.borderColor().getRed(),
        settings.borderColor().getGreen(),
        settings.borderColor().getBlue(),
        settings.fillOpacity(),
        settings.borderWidth());
  }

  /**
   * Validate that all current settings are within acceptable ranges.
   *
   * @throws IllegalStateException if any setting is invalid
   */
  public void validate() {
    if (pixelsPerMicrometer <= 0) {
      throw new IllegalStateException("Invalid pixels per micrometer: " + pixelsPerMicrometer);
    }
    if (scaleUnit == null || scaleUnit.trim().isEmpty()) {
      throw new IllegalStateException("Invalid scale unit: " + scaleUnit);
    }

    // Validate all ROI category settings
    validateROISettings("Vessel", vesselSettings);
    validateROISettings("Nucleus", nucleusSettings);
    validateROISettings("Cytoplasm", cytoplasmSettings);
    validateROISettings("Cell", cellSettings);
  }

  private void validateROISettings(String categoryName, ROIAppearanceSettings settings) {
    if (settings == null) {
      throw new IllegalStateException(categoryName + " settings cannot be null");
    }
    try {
      settings.validate();
    } catch (IllegalStateException e) {
      throw new IllegalStateException(
          "Invalid " + categoryName + " settings: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new MainSettings instance with default values.
   * Factory method for easier instantiation.
   *
   * @return A new MainSettings instance with default values
   */
  public static MainSettings createDefault() {
    return new MainSettings();
  }

  /**
   * Creates a copy of this MainSettings instance.
   *
   * @return A new MainSettings instance with the same values
   */
  public MainSettings copy() {
    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselSettings.copy(),
        nucleusSettings.copy(),
        cytoplasmSettings.copy(),
        cellSettings.copy());
  }
}
