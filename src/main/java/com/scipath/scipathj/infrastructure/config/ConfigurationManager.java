package com.scipath.scipathj.infrastructure.config;

import com.scipath.scipathj.analysis.config.CytoplasmSegmentationSettings;
import com.scipath.scipathj.analysis.config.FeatureExtractionSettings;
import com.scipath.scipathj.analysis.config.NuclearSegmentationSettings;
import com.scipath.scipathj.analysis.config.VesselSegmentationSettings;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application configuration settings with persistence.
 * Handles loading and saving settings to/from properties files.
 * This class follows dependency injection principles and avoids singleton patterns.
 *
 * @author Sebastian Micu
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConfigurationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

  // Configuration file paths
  private static final String CONFIG_DIR =
      System.getProperty("user.home") + File.separator + ".scipathj";
  private static final String VESSEL_SETTINGS_FILE = "vessel_segmentation.properties";
  private static final String NUCLEAR_SETTINGS_FILE = "nuclear_segmentation.properties";
  private static final String CYTOPLASM_SETTINGS_FILE = "cytoplasm_segmentation.properties";
  private static final String FEATURE_EXTRACTION_SETTINGS_FILE = "feature_extraction.properties";
  private static final String MAIN_SETTINGS_FILE = "main_settings.properties";

  /**
   * Creates a new ConfigurationManager instance.
   * Initializes the configuration directory if it doesn't exist.
   */
  public ConfigurationManager() {
    ensureConfigDirectoryExists();
  }

  /**
   * Ensure the configuration directory exists.
   */
  private void ensureConfigDirectoryExists() {
    try {
      Path configPath = Paths.get(CONFIG_DIR);
      if (!Files.exists(configPath)) {
        Files.createDirectories(configPath);
        LOGGER.info("Created configuration directory: {}", CONFIG_DIR);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to create configuration directory: {}", CONFIG_DIR, e);
    }
  }

  /**
   * Generic method to load settings from a properties file.
   *
   * @param <T> The settings type
   * @param fileName The properties file name
   * @param settingsLoader Function to load properties into the settings object
   * @param settings The settings object to populate
   */
  private <T> void loadSettings(
      String fileName, BiConsumer<Properties, T> settingsLoader, T settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, fileName);

    if (!Files.exists(settingsFile)) {
      LOGGER.debug("Settings file not found: {}, using defaults", fileName);
      return;
    }

    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(settingsFile)) {
      properties.load(input);
      settingsLoader.accept(properties, settings);
      LOGGER.debug("Successfully loaded settings from: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to load settings from: {}", settingsFile, e);
    }
  }

  /**
   * Generic method to save settings to a properties file.
   *
   * @param <T> The settings type
   * @param fileName The properties file name
   * @param comment The comment for the properties file
   * @param settingsSaver Function to save settings to properties
   * @param settings The settings object to save
   */
  private <T> void saveSettings(
      String fileName, String comment, Function<T, Properties> settingsSaver, T settings) {
    Path settingsFile = Paths.get(CONFIG_DIR, fileName);

    Properties properties = settingsSaver.apply(settings);
    try (OutputStream output = Files.newOutputStream(settingsFile)) {
      properties.store(output, comment);
      LOGGER.debug("Successfully saved settings to: {}", settingsFile);
    } catch (IOException e) {
      LOGGER.error("Failed to save settings to: {}", settingsFile, e);
    }
  }

  /**
   * Loads vessel segmentation settings from the configuration file.
   *
   * @return The loaded vessel segmentation settings, or default settings if file doesn't exist
   */
  public VesselSegmentationSettings loadVesselSegmentationSettings() {
    Path settingsFile = Paths.get(CONFIG_DIR, VESSEL_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      return VesselSegmentationSettings.createDefault();
    }

    try (InputStream input = Files.newInputStream(settingsFile)) {
      Properties properties = new Properties();
      properties.load(input);
      return loadVesselProperties(properties);
    } catch (IOException e) {
      LOGGER.error("Error loading vessel settings: {}", e.getMessage());
      return VesselSegmentationSettings.createDefault();
    }
  }

  /**
   * Save vessel segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveVesselSegmentationSettings(VesselSegmentationSettings settings) {
    saveSettings(
        VESSEL_SETTINGS_FILE,
        "SciPathJ Vessel Segmentation Settings",
        this::createVesselProperties,
        settings);
  }

  /**
   * Loads nuclear segmentation settings from the configuration file.
   *
   * @return The loaded nuclear segmentation settings, or default settings if file doesn't exist
   */
  public NuclearSegmentationSettings loadNuclearSegmentationSettings() {
    Path settingsFile = Paths.get(CONFIG_DIR, NUCLEAR_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      return NuclearSegmentationSettings.createDefault();
    }

    try (InputStream input = Files.newInputStream(settingsFile)) {
      Properties properties = new Properties();
      properties.load(input);
      return loadNuclearProperties(properties);
    } catch (IOException e) {
      LOGGER.error("Error loading nuclear settings: {}", e.getMessage());
      return NuclearSegmentationSettings.createDefault();
    }
  }

  /**
   * Save nuclear segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveNuclearSegmentationSettings(NuclearSegmentationSettings settings) {
    saveSettings(
        NUCLEAR_SETTINGS_FILE,
        "SciPathJ Nuclear Segmentation Settings",
        this::createNuclearProperties,
        settings);
  }

  /**
   * Loads cytoplasm segmentation settings from the configuration file.
   *
   * @return The loaded cytoplasm segmentation settings, or default settings if file doesn't exist
   */
  public CytoplasmSegmentationSettings loadCytoplasmSegmentationSettings() {
    Path settingsFile = Paths.get(CONFIG_DIR, CYTOPLASM_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      return CytoplasmSegmentationSettings.createDefault();
    }

    try (InputStream input = Files.newInputStream(settingsFile)) {
      Properties properties = new Properties();
      properties.load(input);
      return loadCytoplasmProperties(properties);
    } catch (IOException e) {
      LOGGER.error("Error loading cytoplasm settings: {}", e.getMessage());
      return CytoplasmSegmentationSettings.createDefault();
    }
  }

  /**
   * Save cytoplasm segmentation settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveCytoplasmSegmentationSettings(CytoplasmSegmentationSettings settings) {
    saveSettings(
        CYTOPLASM_SETTINGS_FILE,
        "SciPathJ Cytoplasm Segmentation Settings",
        this::createCytoplasmProperties,
        settings);
  }

  /**
   * Loads main settings from the configuration file.
   *
   * @return The loaded main settings, or default settings if file doesn't exist
   */
  public MainSettings loadMainSettings() {
    Path settingsFile = Paths.get(CONFIG_DIR, MAIN_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      return MainSettings.createDefault();
    }

    try (InputStream input = Files.newInputStream(settingsFile)) {
      Properties properties = new Properties();
      properties.load(input);
      return loadMainSettingsFromProperties(properties);
    } catch (IOException e) {
      LOGGER.error("Error loading main settings: {}", e.getMessage());
      return MainSettings.createDefault();
    }
  }

  /**
   * Save main settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveMainSettings(MainSettings settings) {
    saveSettings(
        MAIN_SETTINGS_FILE, "SciPathJ Main Settings", this::createMainProperties, settings);
  }

  /**
   * Loads feature extraction settings from the configuration file.
   *
   * @return The loaded feature extraction settings, or default settings if file doesn't exist
   */
  public FeatureExtractionSettings loadFeatureExtractionSettings() {
    Path settingsFile = Paths.get(CONFIG_DIR, FEATURE_EXTRACTION_SETTINGS_FILE);

    if (!Files.exists(settingsFile)) {
      return FeatureExtractionSettings.createDefault();
    }

    try (InputStream input = Files.newInputStream(settingsFile)) {
      Properties properties = new Properties();
      properties.load(input);
      return loadFeatureExtractionProperties(properties);
    } catch (IOException e) {
      LOGGER.error("Error loading feature extraction settings: {}", e.getMessage());
      return FeatureExtractionSettings.createDefault();
    }
  }

  /**
   * Save feature extraction settings to the properties file.
   *
   * @param settings The settings object to save
   */
  public void saveFeatureExtractionSettings(FeatureExtractionSettings settings) {
    saveSettings(
        FEATURE_EXTRACTION_SETTINGS_FILE,
        "SciPathJ Feature Extraction Settings",
        this::createFeatureExtractionProperties,
        settings);
  }

  /**
   * Get the configuration directory path.
   *
   * @return The configuration directory path
   */
  public String getConfigDirectory() {
    return CONFIG_DIR;
  }

  /**
   * Check if a settings file exists.
   *
   * @param fileName The settings file name
   * @return true if the settings file exists
   */
  public boolean settingsFileExists(String fileName) {
    Path settingsFile = Paths.get(CONFIG_DIR, fileName);
    return Files.exists(settingsFile);
  }

  // === VESSEL SETTINGS PROPERTY HANDLERS ===

  private VesselSegmentationSettings loadVesselProperties(Properties properties) {
    int threshold =
        getIntProperty(properties, "threshold", VesselSegmentationSettings.DEFAULT_THRESHOLD);
    double minRoiSize =
        getDoubleProperty(
            properties, "minRoiSize", VesselSegmentationSettings.DEFAULT_MIN_ROI_SIZE);
    double maxRoiSize =
        getDoubleProperty(
            properties, "maxRoiSize", VesselSegmentationSettings.DEFAULT_MAX_ROI_SIZE);
    double gaussianBlurSigma =
        getDoubleProperty(
            properties,
            "gaussianBlurSigma",
            VesselSegmentationSettings.DEFAULT_GAUSSIAN_BLUR_SIGMA);
    boolean applyMorphologicalClosing =
        getBooleanProperty(
            properties,
            "applyMorphologicalClosing",
            VesselSegmentationSettings.DEFAULT_APPLY_MORPHOLOGICAL_CLOSING);

    return new VesselSegmentationSettings(
        threshold, minRoiSize, maxRoiSize, gaussianBlurSigma, applyMorphologicalClosing);
  }

  private Properties createVesselProperties(VesselSegmentationSettings settings) {
    Properties properties = new Properties();
    properties.setProperty("threshold", String.valueOf(settings.threshold()));
    properties.setProperty("minRoiSize", String.valueOf(settings.minRoiSize()));
    properties.setProperty("maxRoiSize", String.valueOf(settings.maxRoiSize()));
    properties.setProperty("gaussianBlurSigma", String.valueOf(settings.gaussianBlurSigma()));
    properties.setProperty(
        "applyMorphologicalClosing", String.valueOf(settings.applyMorphologicalClosing()));
    return properties;
  }

  // === NUCLEAR SETTINGS PROPERTY HANDLERS ===

  private NuclearSegmentationSettings loadNuclearProperties(Properties properties) {
    String modelChoice =
        getStringProperty(
            properties, "modelChoice", NuclearSegmentationSettings.DEFAULT_MODEL_CHOICE);
    boolean normalizeInput =
        getBooleanProperty(
            properties, "normalizeInput", NuclearSegmentationSettings.DEFAULT_NORMALIZE_INPUT);
    float percentileBottom =
        getFloatProperty(
            properties, "percentileBottom", NuclearSegmentationSettings.DEFAULT_PERCENTILE_BOTTOM);
    float percentileTop =
        getFloatProperty(
            properties, "percentileTop", NuclearSegmentationSettings.DEFAULT_PERCENTILE_TOP);
    float probThresh =
        getFloatProperty(properties, "probThresh", NuclearSegmentationSettings.DEFAULT_PROB_THRESH);
    float nmsThresh =
        getFloatProperty(properties, "nmsThresh", NuclearSegmentationSettings.DEFAULT_NMS_THRESH);
    int nTiles = getIntProperty(properties, "nTiles", NuclearSegmentationSettings.DEFAULT_N_TILES);
    int excludeBoundary =
        getIntProperty(
            properties, "excludeBoundary", NuclearSegmentationSettings.DEFAULT_EXCLUDE_BOUNDARY);
    double minNucleusSize =
        getDoubleProperty(
            properties, "minNucleusSize", NuclearSegmentationSettings.DEFAULT_MIN_NUCLEUS_SIZE);
    double maxNucleusSize =
        getDoubleProperty(
            properties, "maxNucleusSize", NuclearSegmentationSettings.DEFAULT_MAX_NUCLEUS_SIZE);
    boolean verbose =
        getBooleanProperty(properties, "verbose", NuclearSegmentationSettings.DEFAULT_VERBOSE);
    boolean showCsbdeepProgress =
        getBooleanProperty(
            properties,
            "showCsbdeepProgress",
            NuclearSegmentationSettings.DEFAULT_SHOW_CSBDEEP_PROGRESS);
    boolean showProbAndDist =
        getBooleanProperty(
            properties, "showProbAndDist", NuclearSegmentationSettings.DEFAULT_SHOW_PROB_AND_DIST);

    return new NuclearSegmentationSettings(
        modelChoice,
        normalizeInput,
        percentileBottom,
        percentileTop,
        probThresh,
        nmsThresh,
        "Both", // outputType - default value
        nTiles,
        excludeBoundary,
        "Automatic", // roiPosition - default value
        verbose,
        showCsbdeepProgress,
        showProbAndDist,
        minNucleusSize,
        maxNucleusSize);
  }

  private Properties createNuclearProperties(NuclearSegmentationSettings settings) {
    Properties properties = new Properties();
    properties.setProperty("modelChoice", settings.modelChoice());
    properties.setProperty("normalizeInput", String.valueOf(settings.normalizeInput()));
    properties.setProperty("percentileBottom", String.valueOf(settings.percentileBottom()));
    properties.setProperty("percentileTop", String.valueOf(settings.percentileTop()));
    properties.setProperty("probThresh", String.valueOf(settings.probThresh()));
    properties.setProperty("nmsThresh", String.valueOf(settings.nmsThresh()));
    properties.setProperty("nTiles", String.valueOf(settings.nTiles()));
    properties.setProperty("excludeBoundary", String.valueOf(settings.excludeBoundary()));
    properties.setProperty("verbose", String.valueOf(settings.verbose()));
    properties.setProperty("showCsbdeepProgress", String.valueOf(settings.showCsbdeepProgress()));
    properties.setProperty("showProbAndDist", String.valueOf(settings.showProbAndDist()));
    properties.setProperty("minNucleusSize", String.valueOf(settings.minNucleusSize()));
    properties.setProperty("maxNucleusSize", String.valueOf(settings.maxNucleusSize()));
    return properties;
  }

  // === CYTOPLASM SETTINGS PROPERTY HANDLERS ===

  private CytoplasmSegmentationSettings loadCytoplasmProperties(Properties properties) {
    double voronoiExpansion =
        getDoubleProperty(
            properties,
            "voronoiExpansion",
            CytoplasmSegmentationSettings.DEFAULT_VORONOI_EXPANSION);
    boolean useVesselExclusion =
        getBooleanProperty(
            properties,
            "useVesselExclusion",
            CytoplasmSegmentationSettings.DEFAULT_USE_VESSEL_EXCLUSION);
    double minCellSize =
        getDoubleProperty(
            properties, "minCellSize", CytoplasmSegmentationSettings.DEFAULT_MIN_CELL_SIZE);
    double maxCellSize =
        getDoubleProperty(
            properties, "maxCellSize", CytoplasmSegmentationSettings.DEFAULT_MAX_CELL_SIZE);
    double gaussianBlurSigma =
        getDoubleProperty(
            properties,
            "gaussianBlurSigma",
            CytoplasmSegmentationSettings.DEFAULT_GAUSSIAN_BLUR_SIGMA);
    double morphClosingRadius =
        getDoubleProperty(
            properties,
            "morphClosingRadius",
            CytoplasmSegmentationSettings.DEFAULT_MORPH_CLOSING_RADIUS);
    double watershedTolerance =
        getDoubleProperty(
            properties,
            "watershedTolerance",
            CytoplasmSegmentationSettings.DEFAULT_WATERSHED_TOLERANCE);
    double minCytoplasmArea =
        getDoubleProperty(
            properties,
            "minCytoplasmArea",
            CytoplasmSegmentationSettings.DEFAULT_MIN_CYTOPLASM_AREA);
    double maxCytoplasmArea =
        getDoubleProperty(
            properties,
            "maxCytoplasmArea",
            CytoplasmSegmentationSettings.DEFAULT_MAX_CYTOPLASM_AREA);
    boolean fillHoles =
        getBooleanProperty(
            properties, "fillHoles", CytoplasmSegmentationSettings.DEFAULT_FILL_HOLES);
    boolean smoothBoundaries =
        getBooleanProperty(
            properties,
            "smoothBoundaries",
            CytoplasmSegmentationSettings.DEFAULT_SMOOTH_BOUNDARIES);
    boolean verbose =
        getBooleanProperty(properties, "verbose", CytoplasmSegmentationSettings.DEFAULT_VERBOSE);

    return new CytoplasmSegmentationSettings(
        useVesselExclusion,
        CytoplasmSegmentationSettings.DEFAULT_ADD_IMAGE_BORDER,
        CytoplasmSegmentationSettings.DEFAULT_BORDER_WIDTH,
        CytoplasmSegmentationSettings.DEFAULT_APPLY_VORONOI,
        minCellSize,
        maxCellSize,
        minCytoplasmArea,
        CytoplasmSegmentationSettings.DEFAULT_VALIDATE_CELL_SHAPE,
        CytoplasmSegmentationSettings.DEFAULT_MAX_ASPECT_RATIO,
        CytoplasmSegmentationSettings.DEFAULT_LINK_NUCLEUS_TO_CYTOPLASM,
        CytoplasmSegmentationSettings.DEFAULT_CREATE_CELL_ROIS,
        CytoplasmSegmentationSettings.DEFAULT_EXCLUDE_BORDER_CELLS);
  }

  private Properties createCytoplasmProperties(CytoplasmSegmentationSettings settings) {
    Properties properties = new Properties();
    properties.setProperty("useVesselExclusion", String.valueOf(settings.useVesselExclusion()));
    properties.setProperty("addImageBorder", String.valueOf(settings.addImageBorder()));
    properties.setProperty("borderWidth", String.valueOf(settings.borderWidth()));
    properties.setProperty("applyVoronoi", String.valueOf(settings.applyVoronoi()));
    properties.setProperty("minCellSize", String.valueOf(settings.minCellSize()));
    properties.setProperty("maxCellSize", String.valueOf(settings.maxCellSize()));
    properties.setProperty("minCytoplasmSize", String.valueOf(settings.minCytoplasmSize()));
    properties.setProperty("validateCellShape", String.valueOf(settings.validateCellShape()));
    properties.setProperty("maxAspectRatio", String.valueOf(settings.maxAspectRatio()));
    properties.setProperty(
        "linkNucleusToCytoplasm", String.valueOf(settings.linkNucleusToCytoplasm()));
    properties.setProperty("createCellROIs", String.valueOf(settings.createCellROIs()));
    properties.setProperty("excludeBorderCells", String.valueOf(settings.excludeBorderCells()));
    return properties;
  }

  // === MAIN SETTINGS PROPERTY HANDLERS ===

  private MainSettings loadMainSettingsFromProperties(Properties properties) {
    // Load pixel scale settings
    double pixelsPerMicrometer =
        getDoubleProperty(
            properties, "pixelsPerMicrometer", MainSettings.DEFAULT_PIXELS_PER_MICROMETER);
    String scaleUnit = getStringProperty(properties, "scaleUnit", MainSettings.DEFAULT_SCALE_UNIT);

    // Load ROI appearance settings for each category
    MainSettings.ROIAppearanceSettings vesselSettings =
        loadROIAppearanceSettings(properties, "vessel");
    MainSettings.ROIAppearanceSettings nucleusSettings =
        loadROIAppearanceSettings(properties, "nucleus");
    MainSettings.ROIAppearanceSettings cytoplasmSettings =
        loadROIAppearanceSettings(properties, "cytoplasm");
    MainSettings.ROIAppearanceSettings cellSettings = loadROIAppearanceSettings(properties, "cell");

    // Load ignore ROI settings
    MainSettings.IgnoreROIAppearanceSettings ignoreSettings =
        loadIgnoreROIAppearanceSettings(properties);

    // Load CSV format setting
    boolean useEuCsvFormat = getBooleanProperty(
        properties, "useEuCsvFormat", MainSettings.DEFAULT_USE_EU_CSV_FORMAT);

    // Load ignore functionality setting
    boolean enableIgnoreFunctionality = getBooleanProperty(
        properties, "enableIgnoreFunctionality", MainSettings.DEFAULT_ENABLE_IGNORE_FUNCTIONALITY);

    // Load CSV inclusion setting for ignored ROIs
    boolean includeIgnoredInCsv = getBooleanProperty(
        properties, "includeIgnoredInCsv", MainSettings.DEFAULT_INCLUDE_IGNORED_IN_CSV);

    return new MainSettings(
        pixelsPerMicrometer,
        scaleUnit,
        vesselSettings,
        nucleusSettings,
        cytoplasmSettings,
        cellSettings,
        ignoreSettings,
        useEuCsvFormat,
        enableIgnoreFunctionality,
        includeIgnoredInCsv);
  }

  private MainSettings.ROIAppearanceSettings loadROIAppearanceSettings(
      Properties properties, String category) {
    // Load ROI appearance settings for specific category with fallback to defaults
    String prefix = category + ".";
    Color borderColor =
        getColorProperty(properties, prefix + "borderColor", MainSettings.DEFAULT_BORDER_COLOR);
    float fillOpacity =
        (float)
            getDoubleProperty(
                properties, prefix + "fillOpacity", MainSettings.DEFAULT_FILL_OPACITY);
    int borderWidth =
        getIntProperty(properties, prefix + "borderWidth", MainSettings.DEFAULT_BORDER_WIDTH);

    return new MainSettings.ROIAppearanceSettings(borderColor, fillOpacity, borderWidth);
  }

  private MainSettings.ROIAppearanceSettings loadROIAppearanceSettings(Properties properties) {
    // Load default ROI appearance settings (backward compatibility)
    Color borderColor =
        getColorProperty(properties, "roiBorderColor", MainSettings.DEFAULT_BORDER_COLOR);
    float fillOpacity =
        (float) getDoubleProperty(properties, "roiFillOpacity", MainSettings.DEFAULT_FILL_OPACITY);
    int borderWidth =
        getIntProperty(properties, "roiBorderWidth", MainSettings.DEFAULT_BORDER_WIDTH);

    return new MainSettings.ROIAppearanceSettings(borderColor, fillOpacity, borderWidth);
  }

  // This method is no longer needed since MainSettings is now immutable
  // Loading is handled in loadMainSettingsFromProperties

  private Properties createMainProperties(MainSettings settings) {
    Properties properties = new Properties();

    // Save scale settings
    properties.setProperty(
        "pixelsPerMicrometer", String.valueOf(settings.getPixelsPerMicrometer()));
    properties.setProperty("scaleUnit", settings.getScaleUnit());

    // Save type-specific ROI appearance settings
    saveROICategorySettings(properties, "vessel", settings.getVesselSettings());
    saveROICategorySettings(properties, "nucleus", settings.getNucleusSettings());
    saveROICategorySettings(properties, "cytoplasm", settings.getCytoplasmSettings());
    saveROICategorySettings(properties, "cell", settings.getCellSettings());

    // Save ignore ROI settings
    saveIgnoreROIAppearanceSettings(properties, settings.getIgnoreSettings());

    // Save CSV format setting
    properties.setProperty("useEuCsvFormat", String.valueOf(settings.useEuCsvFormat()));

    // Save ignore functionality setting
    properties.setProperty("enableIgnoreFunctionality", String.valueOf(settings.enableIgnoreFunctionality()));

    // Save CSV inclusion setting for ignored ROIs
    properties.setProperty("includeIgnoredInCsv", String.valueOf(settings.includeIgnoredInCsv()));

    // Vessel ROI settings are now handled through the modern VesselSegmentationSettings record

    return properties;
  }

  // === ROI CATEGORY SETTINGS HELPERS ===

  private MainSettings.ROIAppearanceSettings loadROICategorySettings(
      Properties properties, String categoryPrefix) {
    Color borderColor =
        getColorProperty(
            properties, categoryPrefix + ".borderColor", MainSettings.DEFAULT_BORDER_COLOR);
    float fillOpacity =
        (float)
            getDoubleProperty(
                properties, categoryPrefix + ".fillOpacity", MainSettings.DEFAULT_FILL_OPACITY);
    int borderWidth =
        getIntProperty(
            properties, categoryPrefix + ".borderWidth", MainSettings.DEFAULT_BORDER_WIDTH);

    return new MainSettings.ROIAppearanceSettings(borderColor, fillOpacity, borderWidth);
  }

  private void saveROICategorySettings(
      Properties properties, String categoryPrefix, MainSettings.ROIAppearanceSettings settings) {
    properties.setProperty(categoryPrefix + ".borderColor", colorToString(settings.borderColor()));
    properties.setProperty(categoryPrefix + ".fillOpacity", String.valueOf(settings.fillOpacity()));
    properties.setProperty(categoryPrefix + ".borderWidth", String.valueOf(settings.borderWidth()));
  }

  private MainSettings.IgnoreROIAppearanceSettings loadIgnoreROIAppearanceSettings(Properties properties) {
    int borderDistance = getIntProperty(properties, "ignore.borderDistance", MainSettings.DEFAULT_BORDER_DISTANCE);
    Color ignoreColor = getColorProperty(properties, "ignore.ignoreColor", MainSettings.DEFAULT_IGNORE_COLOR);
    float fillOpacity = (float) getDoubleProperty(properties, "ignore.fillOpacity", MainSettings.DEFAULT_FILL_OPACITY);
    int borderWidth = getIntProperty(properties, "ignore.borderWidth", MainSettings.DEFAULT_BORDER_WIDTH);
    boolean showIgnoredROIs = getBooleanProperty(properties, "ignore.showIgnoredROIs", MainSettings.DEFAULT_SHOW_IGNORE_ROIS);

    return new MainSettings.IgnoreROIAppearanceSettings(borderDistance, ignoreColor, fillOpacity, borderWidth, showIgnoredROIs);
  }

  private void saveIgnoreROIAppearanceSettings(Properties properties, MainSettings.IgnoreROIAppearanceSettings settings) {
    properties.setProperty("ignore.borderDistance", String.valueOf(settings.borderDistance()));
    properties.setProperty("ignore.ignoreColor", colorToString(settings.ignoreColor()));
    properties.setProperty("ignore.fillOpacity", String.valueOf(settings.fillOpacity()));
    properties.setProperty("ignore.borderWidth", String.valueOf(settings.borderWidth()));
    properties.setProperty("ignore.showIgnoredROIs", String.valueOf(settings.showIgnoredROIs()));
  }

  // === GENERIC PROPERTY LOADING HELPERS ===

  private String getStringProperty(Properties properties, String key, String defaultValue) {
    String value = properties.getProperty(key);
    return value != null ? value : defaultValue;
  }

  private int getIntProperty(Properties properties, String key, int defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        // Log warning and return default
      }
    }
    return defaultValue;
  }

  private double getDoubleProperty(Properties properties, String key, double defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException e) {
        // Log warning and return default
      }
    }
    return defaultValue;
  }

  private float getFloatProperty(Properties properties, String key, float defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        return Float.parseFloat(value);
      } catch (NumberFormatException e) {
        // Log warning and return default
      }
    }
    return defaultValue;
  }

  private boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
    String value = properties.getProperty(key);
    return value != null ? Boolean.parseBoolean(value) : defaultValue;
  }

  private Color getColorProperty(Properties properties, String key, Color defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        // First try Color.decode() for hex formats
        return Color.decode(value);
      } catch (NumberFormatException e) {
        try {
          // If decode fails, try parseColor for R,G,B format
          return parseColor(value);
        } catch (Exception parseException) {
          // Log warning and return default
          System.err.println("Invalid color value for " + key + ": " + value);
        }
      }
    }
    return defaultValue;
  }

  private void loadStringProperty(
      Properties properties, String key, java.util.function.Consumer<String> setter) {
    String value = properties.getProperty(key);
    if (value != null) {
      setter.accept(value);
    }
  }

  private void loadIntProperty(
      Properties properties, String key, java.util.function.Consumer<Integer> setter) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        setter.accept(Integer.parseInt(value));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid integer value for {}: {}", key, value);
      }
    }
  }

  private void loadDoubleProperty(
      Properties properties, String key, java.util.function.Consumer<Double> setter) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        setter.accept(Double.parseDouble(value));
      } catch (NumberFormatException e) {
        LOGGER.warn("Invalid double value for {}: {}", key, value);
      }
    }
  }

  // === COLOR PARSING UTILITIES ===

  /**
   * Parse a color from string format "R,G,B" or "R,G,B,A".
   *
   * @param colorStr The color string
   * @return The parsed Color object
   */
  private Color parseColor(String colorStr) {
    String[] parts = colorStr.split(",");
    if (parts.length == 3) {
      int r = Integer.parseInt(parts[0].trim());
      int g = Integer.parseInt(parts[1].trim());
      int b = Integer.parseInt(parts[2].trim());
      return new Color(r, g, b);
    } else if (parts.length == 4) {
      int r = Integer.parseInt(parts[0].trim());
      int g = Integer.parseInt(parts[1].trim());
      int b = Integer.parseInt(parts[2].trim());
      int a = Integer.parseInt(parts[3].trim());
      return new Color(r, g, b, a);
    } else {
      throw new IllegalArgumentException("Invalid color format: " + colorStr);
    }
  }

  /**
   * Convert a color to string format "R,G,B".
   *
   * @param color The color to convert
   * @return The color string
   */
  private String colorToString(Color color) {
    return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
  }

  // === FEATURE EXTRACTION SETTINGS PROPERTY HANDLERS ===

  private FeatureExtractionSettings loadFeatureExtractionProperties(Properties properties) {
    // Load performance settings
    boolean enablePerformanceOptimizations = getBooleanProperty(
        properties, "enablePerformanceOptimizations", FeatureExtractionSettings.DEFAULT_ENABLE_PERFORMANCE_OPTIMIZATIONS);
    int spatialGridSize = getIntProperty(
        properties, "spatialGridSize", FeatureExtractionSettings.DEFAULT_SPATIAL_GRID_SIZE);
    int batchSize = getIntProperty(
        properties, "batchSize", FeatureExtractionSettings.DEFAULT_BATCH_SIZE);
    boolean sortROIs = getBooleanProperty(
        properties, "sortROIs", FeatureExtractionSettings.DEFAULT_SORT_ROIS);

    // Load feature maps for each region type
    java.util.Map<String, Boolean> cellFeatures = loadFeatureMapFromProperties(
        properties, "cell", FeatureExtractionSettings.createDefault().cellFeatures());
    java.util.Map<String, Boolean> nucleusFeatures = loadFeatureMapFromProperties(
        properties, "nucleus", FeatureExtractionSettings.createDefault().nucleusFeatures());
    java.util.Map<String, Boolean> cytoplasmFeatures = loadFeatureMapFromProperties(
        properties, "cytoplasm", FeatureExtractionSettings.createDefault().cytoplasmFeatures());
    java.util.Map<String, Boolean> vesselFeatures = loadFeatureMapFromProperties(
        properties, "vessel", FeatureExtractionSettings.createDefault().vesselFeatures());

    return new FeatureExtractionSettings(
        cellFeatures, nucleusFeatures, cytoplasmFeatures, vesselFeatures,
        enablePerformanceOptimizations, spatialGridSize, batchSize, sortROIs);
  }

  private java.util.Map<String, Boolean> loadFeatureMapFromProperties(
      Properties properties, String regionType, java.util.Map<String, Boolean> defaults) {
    java.util.Map<String, Boolean> features = new java.util.HashMap<>(defaults);

    // Load each feature from properties
    for (String featureName : defaults.keySet()) {
      String propertyKey = regionType + "." + featureName;
      boolean value = getBooleanProperty(properties, propertyKey, defaults.get(featureName));
      features.put(featureName, value);
    }

    return features;
  }

  private Properties createFeatureExtractionProperties(FeatureExtractionSettings settings) {
    Properties properties = new Properties();

    // Save performance settings
    properties.setProperty("enablePerformanceOptimizations",
        String.valueOf(settings.enablePerformanceOptimizations()));
    properties.setProperty("spatialGridSize", String.valueOf(settings.spatialGridSize()));
    properties.setProperty("batchSize", String.valueOf(settings.batchSize()));
    properties.setProperty("sortROIs", String.valueOf(settings.sortROIs()));

    // Save feature maps for each region type
    saveFeatureMapToProperties(properties, "cell", settings.cellFeatures());
    saveFeatureMapToProperties(properties, "nucleus", settings.nucleusFeatures());
    saveFeatureMapToProperties(properties, "cytoplasm", settings.cytoplasmFeatures());
    saveFeatureMapToProperties(properties, "vessel", settings.vesselFeatures());

    return properties;
  }

  private void saveFeatureMapToProperties(
      Properties properties, String regionType, java.util.Map<String, Boolean> features) {
    for (java.util.Map.Entry<String, Boolean> entry : features.entrySet()) {
      String propertyKey = regionType + "." + entry.getKey();
      properties.setProperty(propertyKey, String.valueOf(entry.getValue()));
    }
  }
}
