package com.scipath.scipathj.core.config;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application settings class for global configuration.
 * Manages scale conversion, type-specific ROI appearance, and other global settings with persistence.
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
   * Settings for a specific ROI type
   */
  public static class ROIAppearanceSettings {
    private Color borderColor;
    private Color fillColor;
    private float fillOpacity;
    private int borderWidth;

    public ROIAppearanceSettings(Color borderColor, float fillOpacity, int borderWidth) {
      this.borderColor = borderColor;
      this.fillOpacity = fillOpacity;
      this.borderWidth = borderWidth;
      updateFillColor();
    }

    private void updateFillColor() {
      int alpha = Math.round(fillOpacity * 255);
      this.fillColor =
          new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), alpha);
    }

    // Getters
    public Color getBorderColor() {
      return borderColor;
    }

    public Color getFillColor() {
      return fillColor;
    }

    public float getFillOpacity() {
      return fillOpacity;
    }

    public int getBorderWidth() {
      return borderWidth;
    }

    // Setters with validation
    public void setBorderColor(Color borderColor) {
      if (borderColor == null) {
        throw new IllegalArgumentException("Border color cannot be null");
      }
      this.borderColor = borderColor;
      updateFillColor();
    }

    public void setFillOpacity(float fillOpacity) {
      if (fillOpacity < 0.0f || fillOpacity > 1.0f) {
        throw new IllegalArgumentException("Fill opacity must be between 0.0 and 1.0");
      }
      this.fillOpacity = fillOpacity;
      updateFillColor();
    }

    public void setBorderWidth(int borderWidth) {
      if (borderWidth < 1) {
        throw new IllegalArgumentException("Border width must be at least 1");
      }
      this.borderWidth = borderWidth;
    }

    public ROIAppearanceSettings copy() {
      return new ROIAppearanceSettings(new Color(borderColor.getRGB()), fillOpacity, borderWidth);
    }
  }

  // Default values for scale conversion
  public static final double DEFAULT_PIXELS_PER_MICROMETER =
      1.0; // 1 pixel = 1 micrometer by default
  public static final String DEFAULT_SCALE_UNIT = "μm"; // micrometers

  // Current values (initialized with defaults)
  private double pixelsPerMicrometer = DEFAULT_PIXELS_PER_MICROMETER;
  private String scaleUnit = DEFAULT_SCALE_UNIT;

  // Type-specific ROI appearance settings
  private final ROIAppearanceSettings vesselSettings;
  private final ROIAppearanceSettings nucleusSettings;
  private final ROIAppearanceSettings cytoplasmSettings;
  private final ROIAppearanceSettings cellSettings;

  // Settings change listeners
  private final List<SettingsChangeListener> listeners;

  public interface SettingsChangeListener {
    void onSettingsChanged();
  }

  // Singleton instance
  private static MainSettings instance;

  private MainSettings() {
    // Initialize type-specific settings with defaults
    this.vesselSettings =
        new ROIAppearanceSettings(
            ROICategory.VESSEL.getDefaultBorderColor(),
            ROICategory.VESSEL.getDefaultFillOpacity(),
            ROICategory.VESSEL.getDefaultBorderWidth());
    this.nucleusSettings =
        new ROIAppearanceSettings(
            ROICategory.NUCLEUS.getDefaultBorderColor(),
            ROICategory.NUCLEUS.getDefaultFillOpacity(),
            ROICategory.NUCLEUS.getDefaultBorderWidth());
    this.cytoplasmSettings =
        new ROIAppearanceSettings(
            ROICategory.CYTOPLASM.getDefaultBorderColor(),
            ROICategory.CYTOPLASM.getDefaultFillOpacity(),
            ROICategory.CYTOPLASM.getDefaultBorderWidth());
    this.cellSettings =
        new ROIAppearanceSettings(
            ROICategory.CELL.getDefaultBorderColor(),
            ROICategory.CELL.getDefaultFillOpacity(),
            ROICategory.CELL.getDefaultBorderWidth());

    this.listeners = new CopyOnWriteArrayList<>();
    LOGGER.debug("MainSettings initialized with default values for all ROI types");
  }

  /**
   * Get the singleton instance of MainSettings.
   *
   * @return The singleton instance
   */
  public static synchronized MainSettings getInstance() {
    if (instance == null) {
      instance = new MainSettings();
    }
    return instance;
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
    return vesselSettings.getBorderColor();
  }

  public Color getRoiFillColor() {
    return vesselSettings.getFillColor();
  }

  public float getRoiFillOpacity() {
    return vesselSettings.getFillOpacity();
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

  // Convenience setters for backward compatibility (delegates to vessel settings)
  public void setRoiBorderColor(Color roiBorderColor) {
    vesselSettings.setBorderColor(roiBorderColor);
    notifySettingsChanged();
    LOGGER.debug(
        "Vessel ROI border color set to: RGB({}, {}, {})",
        roiBorderColor.getRed(),
        roiBorderColor.getGreen(),
        roiBorderColor.getBlue());
  }

  public void setRoiFillOpacity(float roiFillOpacity) {
    vesselSettings.setFillOpacity(roiFillOpacity);
    notifySettingsChanged();
    LOGGER.debug("Vessel ROI fill opacity set to: {}", roiFillOpacity);
  }

  public void setRoiBorderWidth(int roiBorderWidth) {
    vesselSettings.setBorderWidth(roiBorderWidth);
    notifySettingsChanged();
    LOGGER.debug("Vessel ROI border width set to: {}", roiBorderWidth);
  }

  /**
   * Update settings for a specific ROI category
   */
  public void updateCategorySettings(
      ROICategory category, Color borderColor, float fillOpacity, int borderWidth) {
    ROIAppearanceSettings settings = getSettingsForCategory(category);
    settings.setBorderColor(borderColor);
    settings.setFillOpacity(fillOpacity);
    settings.setBorderWidth(borderWidth);
    notifySettingsChanged();
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
    vesselSettings.setBorderColor(ROICategory.VESSEL.getDefaultBorderColor());
    vesselSettings.setFillOpacity(ROICategory.VESSEL.getDefaultFillOpacity());
    vesselSettings.setBorderWidth(ROICategory.VESSEL.getDefaultBorderWidth());

    nucleusSettings.setBorderColor(ROICategory.NUCLEUS.getDefaultBorderColor());
    nucleusSettings.setFillOpacity(ROICategory.NUCLEUS.getDefaultFillOpacity());
    nucleusSettings.setBorderWidth(ROICategory.NUCLEUS.getDefaultBorderWidth());

    cytoplasmSettings.setBorderColor(ROICategory.CYTOPLASM.getDefaultBorderColor());
    cytoplasmSettings.setFillOpacity(ROICategory.CYTOPLASM.getDefaultFillOpacity());
    cytoplasmSettings.setBorderWidth(ROICategory.CYTOPLASM.getDefaultBorderWidth());

    cellSettings.setBorderColor(ROICategory.CELL.getDefaultBorderColor());
    cellSettings.setFillOpacity(ROICategory.CELL.getDefaultFillOpacity());
    cellSettings.setBorderWidth(ROICategory.CELL.getDefaultBorderWidth());

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
    return !vesselSettings.getBorderColor().equals(ROICategory.VESSEL.getDefaultBorderColor())
        || vesselSettings.getFillOpacity() != ROICategory.VESSEL.getDefaultFillOpacity()
        || vesselSettings.getBorderWidth() != ROICategory.VESSEL.getDefaultBorderWidth()
        || !nucleusSettings.getBorderColor().equals(ROICategory.NUCLEUS.getDefaultBorderColor())
        || nucleusSettings.getFillOpacity() != ROICategory.NUCLEUS.getDefaultFillOpacity()
        || nucleusSettings.getBorderWidth() != ROICategory.NUCLEUS.getDefaultBorderWidth()
        || !cytoplasmSettings.getBorderColor().equals(ROICategory.CYTOPLASM.getDefaultBorderColor())
        || cytoplasmSettings.getFillOpacity() != ROICategory.CYTOPLASM.getDefaultFillOpacity()
        || cytoplasmSettings.getBorderWidth() != ROICategory.CYTOPLASM.getDefaultBorderWidth()
        || !cellSettings.getBorderColor().equals(ROICategory.CELL.getDefaultBorderColor())
        || cellSettings.getFillOpacity() != ROICategory.CELL.getDefaultFillOpacity()
        || cellSettings.getBorderWidth() != ROICategory.CELL.getDefaultBorderWidth();
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
        settings.getBorderColor().getRed(),
        settings.getBorderColor().getGreen(),
        settings.getBorderColor().getBlue(),
        settings.getFillOpacity(),
        settings.getBorderWidth());
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
    if (settings.getBorderColor() == null) {
      throw new IllegalStateException(categoryName + " border color cannot be null");
    }
    if (settings.getFillColor() == null) {
      throw new IllegalStateException(categoryName + " fill color cannot be null");
    }
    if (settings.getFillOpacity() < 0.0f || settings.getFillOpacity() > 1.0f) {
      throw new IllegalStateException(
          "Invalid " + categoryName + " fill opacity: " + settings.getFillOpacity());
    }
    if (settings.getBorderWidth() < 1) {
      throw new IllegalStateException(
          "Invalid " + categoryName + " border width: " + settings.getBorderWidth());
    }
  }
}
